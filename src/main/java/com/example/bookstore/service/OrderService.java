package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderDetail;
import com.example.bookstore.entity.User;
import com.example.bookstore.model.CartItem;
import com.example.bookstore.model.OrderStatuses;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderDetailRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.bookstore.model.RevenueDTO;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAllWithUser();
    }

    public List<Order> getOrdersByUserId(Integer userId) {
        return orderRepository.findByUser_IdOrderByOrderDateDesc(userId);
    }

    public Order getOrderById(Integer orderId) {
        if (orderId == null) {
            return null;
        }
        return orderRepository.findByIdWithUser(orderId).orElse(null);
    }

    public List<OrderDetail> getOrderDetails(Integer orderId) {
        if (orderId == null) {
            return List.of();
        }
        return orderDetailRepository.findDetailsWithBook(orderId);
    }

    /**
     * Đặt đơn từ giỏ hàng.
     * @return null nếu thành công
     */
    @Transactional
    public String placeOrderFromCart(Integer userId, List<CartItem> items,
                                     String couponCode, Integer percentOff) {
        if (userId == null) {
            return "Bạn cần đăng nhập để mua hàng.";
        }
        if (items == null || items.isEmpty()) {
            return "Giỏ hàng đang trống.";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "Không tìm thấy tài khoản. Hãy đăng nhập lại.";
        }

        double subtotal = 0;
        List<OrderDetail> detailsToSave = new ArrayList<>();
        List<Book> booksToUpdate = new ArrayList<>();

        for (CartItem item : items) {
            if (item.getBookId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                return "Giỏ hàng có sản phẩm không hợp lệ.";
            }
            Book book = bookRepository.findById(item.getBookId()).orElse(null);
            if (book == null) {
                return "Không tìm thấy sách: " + item.getTitle();
            }
            int stock = book.getStockQuantity() != null ? book.getStockQuantity() : 0;
            if (stock < item.getQuantity()) {
                return "Không đủ tồn kho cho \"" + book.getTitle() + "\" (còn " + stock + ").";
            }
            if (book.getPrice() == null) {
                return "Sách \"" + book.getTitle() + "\" chưa có giá.";
            }

            subtotal += book.getPrice() * item.getQuantity();

            OrderDetail detail = new OrderDetail();
            detail.setBook(book);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(book.getPrice());
            detailsToSave.add(detail);

            book.setStockQuantity(stock - item.getQuantity());
            booksToUpdate.add(book);
        }

        double discountAmount = 0;
        if (couponCode != null && !couponCode.isBlank() && percentOff != null && percentOff > 0) {
            discountAmount = subtotal * Math.min(percentOff, 100) / 100.0;
        }
        double total = Math.max(0, subtotal - discountAmount);

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(total);
        order.setCouponCode(couponCode != null && !couponCode.isBlank() ? couponCode.trim() : null);
        order.setDiscountAmount(discountAmount > 0 ? discountAmount : null);
        order.setStatus(OrderStatuses.PENDING);
        order = orderRepository.save(order);

        for (OrderDetail detail : detailsToSave) {
            detail.setOrder(order);
            orderDetailRepository.save(detail);
        }
        for (Book book : booksToUpdate) {
            bookRepository.save(book);
        }

        return null;
    }

    @Transactional
    public String updateStatus(Integer orderId, String newStatus) {
        if (orderId == null) {
            return "Thiếu mã đơn hàng.";
        }
        if (newStatus == null || !OrderStatuses.ALL.contains(newStatus)) {
            return "Trạng thái không hợp lệ.";
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "Không tìm thấy đơn hàng.";
        }

        String current = order.getStatus() != null ? order.getStatus() : OrderStatuses.PENDING;
        if (OrderStatuses.isFinal(current)) {
            return "Đơn đã ở trạng thái \"" + OrderStatuses.label(current) + "\", không thể đổi tiếp.";
        }

        if (OrderStatuses.CANCELLED.equals(newStatus)) {
            if (!OrderStatuses.canCancel(current)) {
                return "Không thể hủy đơn ở trạng thái hiện tại.";
            }
            restoreStock(orderId);
        } else {
            String expectedNext = OrderStatuses.nextStatus(current);
            if (expectedNext == null || !expectedNext.equals(newStatus)) {
                return "Chỉ được chuyển sang: " + OrderStatuses.label(expectedNext)
                        + " (hiện tại: " + OrderStatuses.label(current) + ").";
            }
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        return null;
    }

    @Transactional
    public String cancelOrder(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "Không tìm thấy đơn hàng.";
        }
        if (userId != null && (order.getUser() == null || !userId.equals(order.getUser().getId()))) {
            return "Bạn không thể hủy đơn của người khác.";
        }
        if (userId != null && !OrderStatuses.PENDING.equals(order.getStatus())) {
            return "Bạn chỉ hủy được đơn đang chờ xác nhận.";
        }
        if (!OrderStatuses.canCancel(order.getStatus())) {
            return "Không thể hủy đơn ở trạng thái \"" + OrderStatuses.label(order.getStatus()) + "\".";
        }
        return updateStatus(orderId, OrderStatuses.CANCELLED);
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

    /** Tổng doanh thu (đơn đã giao). */
    public Double getTotalRevenue() {
        Double sum = orderRepository.sumRevenueByStatuses(OrderStatuses.REVENUE_STATUSES);
        return sum != null ? sum : 0d;
    }

    /** Tổng doanh thu trong khoảng thời gian. */
    public Double getRevenueBetween(LocalDateTime from, LocalDateTime to) {
        Double sum = orderRepository.sumRevenueByStatusesAndDateRange(OrderStatuses.REVENUE_STATUSES, from, to);
        return sum != null ? sum : 0d;
    }

    /** Số đơn đã giao. */
    public long countDelivered() {
        return orderRepository.countByStatusIn(OrderStatuses.REVENUE_STATUSES);
    }

    /** Số đơn đã giao trong khoảng. */
    public long countDeliveredBetween(LocalDateTime from, LocalDateTime to) {
        return orderRepository.countByStatusInAndOrderDateBetween(OrderStatuses.REVENUE_STATUSES, from, to);
    }

    /** Số đơn đã hủy. */
    public long countCancelled() {
        return orderRepository.countByStatus(OrderStatuses.CANCELLED);
    }

    /** Tổng tiền các đơn đã hủy. */
    public Double getCancelledAmount() {
        Double sum = orderRepository.sumRevenueByStatuses(List.of(OrderStatuses.CANCELLED));
        return sum != null ? sum : 0d;
    }

    /** Tổng tất cả đơn. */
    public long countAll() {
        return orderRepository.count();
    }

    /** Doanh thu theo ngày trong khoảng thời gian (cho biểu đồ). */
    public List<RevenueDTO> getDailyRevenue(LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByStatusesAndDateRange(OrderStatuses.REVENUE_STATUSES, from, to);
        Map<String, Double> map = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d/M");
        LocalDate start = from.toLocalDate();
        LocalDate end = to.toLocalDate();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            map.put(d.format(fmt), 0.0);
        }
        for (Order o : orders) {
            if (o.getOrderDate() == null) {
                continue;
            }
            String key = o.getOrderDate().toLocalDate().format(fmt);
            map.merge(key, o.getTotalAmount() != null ? o.getTotalAmount() : 0.0, Double::sum);
        }
        return map.entrySet().stream()
                .map(e -> new RevenueDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /** Doanh thu theo tháng trong năm (cho biểu đồ). */
    public List<RevenueDTO> getMonthlyRevenue(int year) {
        LocalDateTime from = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<Order> orders = orderRepository.findByStatusesAndDateRange(OrderStatuses.REVENUE_STATUSES, from, to);
        Map<String, Double> map = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            map.put(String.format("T%d", m), 0.0);
        }
        for (Order o : orders) {
            if (o.getOrderDate() == null) {
                continue;
            }
            int month = o.getOrderDate().getMonthValue();
            String key = String.format("T%d", month);
            map.merge(key, o.getTotalAmount() != null ? o.getTotalAmount() : 0.0, Double::sum);
        }
        return map.entrySet().stream()
                .map(e -> new RevenueDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /** Đơn gần đây nhất. */
    public List<Order> getRecentOrders(int limit) {
        List<Order> all = orderRepository.findAllWithUser();
        return all.subList(0, Math.min(limit, all.size()));
    }
}
