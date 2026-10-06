package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderDetail;
import com.example.bookstore.entity.ReturnRequest;
import com.example.bookstore.entity.User;
import com.example.bookstore.model.OrderStatuses;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderDetailRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.ReturnRequestRepository;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.util.ImageStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReturnRequestService {

    public static final Path UPLOAD_DIR = Paths.get("uploads", "returns").toAbsolutePath().normalize();

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ReturnRequest> getAllReturnRequests() {
        return returnRequestRepository.findAllWithDetails();
    }

    public List<ReturnRequest> getReturnRequestsByUserId(Integer userId) {
        return returnRequestRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public ReturnRequest getById(Integer id) {
        if (id == null) {
            return null;
        }
        return returnRequestRepository.findByIdWithDetails(id).orElse(null);
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return "Không rõ";
        }
        return switch (status) {
            case "PENDING" -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Từ chối";
            default -> status;
        };
    }

    public static Map<String, String> allStatusLabels() {
        Map<String, String> map = new HashMap<>();
        map.put("PENDING", statusLabel("PENDING"));
        map.put("APPROVED", statusLabel("APPROVED"));
        map.put("REJECTED", statusLabel("REJECTED"));
        return map;
    }

    public long countByStatus(String status) {
        return returnRequestRepository.countByStatus(status);
    }

    public Double sumOrderAmountByStatus(String status) {
        Double sum = returnRequestRepository.sumOrderAmountByStatus(status);
        return sum != null ? sum : 0d;
    }

    /** Đơn đã giao và chưa có yêu cầu PENDING/APPROVED. */
    public boolean canRequestReturn(Order order) {
        if (order == null || order.getId() == null) {
            return false;
        }
        if (!isDelivered(order.getStatus())) {
            return false;
        }
        return !returnRequestRepository.existsByOrder_IdAndStatusIn(
                order.getId(), List.of("PENDING", "APPROVED"));
    }

    public Map<Integer, Boolean> buildCanReturnMap(List<Order> orders) {
        Map<Integer, Boolean> map = new HashMap<>();
        if (orders == null) {
            return map;
        }
        for (Order order : orders) {
            if (order != null && order.getId() != null) {
                map.put(order.getId(), canRequestReturn(order));
            }
        }
        return map;
    }

    private boolean isDelivered(String status) {
        return OrderStatuses.DELIVERED.equals(status) || "COMPLETED".equals(status);
    }

    public String createReturnRequest(Integer orderId, Integer userId, String reason, MultipartFile imageFile) {
        if (orderId == null) {
            return "Vui lòng chọn mã đơn hàng.";
        }
        if (reason == null || reason.isBlank()) {
            return "Vui lòng nhập lý do hoàn trả.";
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (order == null) {
            return "Không tìm thấy đơn hàng.";
        }
        if (user == null) {
            return "Không tìm thấy tài khoản.";
        }
        if (order.getUser() == null || !userId.equals(order.getUser().getId())) {
            return "Bạn chỉ gửi hoàn trả cho đơn của mình.";
        }
        if (!isDelivered(order.getStatus())) {
            return "Chỉ hoàn trả được đơn đã giao thành công.";
        }
        if (returnRequestRepository.existsByOrder_IdAndStatus(orderId, "PENDING")) {
            return "Đơn này đang có yêu cầu hoàn trả chờ duyệt.";
        }
        if (returnRequestRepository.existsByOrder_IdAndStatus(orderId, "APPROVED")) {
            return "Đơn này đã được duyệt hoàn trả trước đó.";
        }

        try {
            ReturnRequest request = new ReturnRequest();
            request.setOrder(order);
            request.setUser(user);
            request.setReason(reason.trim());
            request.setStatus("PENDING");
            request.setCreatedAt(LocalDateTime.now());
            if (imageFile != null && !imageFile.isEmpty()) {
                request.setImagePath(ImageStorage.save(imageFile, UPLOAD_DIR, "/uploads/returns/"));
            }
            returnRequestRepository.save(request);
            return null;
        } catch (IOException ex) {
            return "Lỗi tải ảnh: " + ex.getMessage();
        }
    }

    /**
     * @return null nếu thành công
     */
    @Transactional
    public String updateStatus(Integer id, String status) {
        ReturnRequest request = returnRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return "Không tìm thấy yêu cầu hoàn trả.";
        }
        if (!"PENDING".equals(request.getStatus())) {
            return "Yêu cầu này đã được xử lý.";
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            return "Trạng thái không hợp lệ.";
        }
        if ("APPROVED".equals(status)) {
            Order order = request.getOrder();
            if (order == null) {
                return "Yêu cầu không gắn với đơn hàng.";
            }
            if (!isDelivered(order.getStatus())) {
                return "Chỉ duyệt hoàn trả cho đơn đã giao.";
            }
            restoreStock(order.getId());
        }
        request.setStatus(status);
        returnRequestRepository.save(request);
        return null;
    }

    private void restoreStock(Integer orderId) {
        List<OrderDetail> details = orderDetailRepository.findByOrder_Id(orderId);
        for (OrderDetail detail : details) {
            Book book = detail.getBook();
            if (book == null) {
                continue;
            }
            int stock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
            int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
            book.setStockQuantity(stock + qty);
            bookRepository.save(book);
        }
    }
}
