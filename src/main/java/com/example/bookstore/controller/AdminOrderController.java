package com.example.bookstore.controller;

import com.example.bookstore.entity.DiscountCode;
import com.example.bookstore.model.OrderStatuses;
import com.example.bookstore.repository.DiscountCodeRepository;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.service.ReturnRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bookstore.model.RevenueDTO;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReturnRequestService returnRequestService;

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    @GetMapping("/orders")
    public String viewOrdersPage(Model model) {
        model.addAttribute("listOrders", orderService.getAllOrders());
        model.addAttribute("listReturns", returnRequestService.getAllReturnRequests());
        model.addAttribute("statusLabels", OrderStatuses.allLabels());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Integer id,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        var order = orderService.getOrderById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderService.getOrderDetails(id));
        model.addAttribute("statusLabels", OrderStatuses.allLabels());
        model.addAttribute("backUrl", "/admin/orders");
        return "order-detail";
    }

    @GetMapping("/returns/{id}")
    public String returnDetail(@PathVariable("id") Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        var ret = returnRequestService.getById(id);
        if (ret == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy yêu cầu hoàn trả #" + id);
            return "redirect:/admin/orders";
        }
        model.addAttribute("ret", ret);
        model.addAttribute("statusLabel", ReturnRequestService.statusLabel(ret.getStatus()));
        if (ret.getOrder() != null) {
            model.addAttribute("orderDetails", orderService.getOrderDetails(ret.getOrder().getId()));
            model.addAttribute("statusLabels", OrderStatuses.allLabels());
        }
        model.addAttribute("backUrl", "/admin/orders");
        model.addAttribute("orderDetailUrl", ret.getOrder() != null ? "/admin/orders/" + ret.getOrder().getId() : null);
        return "return-detail";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam("id") Integer id,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        String error = orderService.updateStatus(id, status);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success",
                    "Đơn #" + id + " → " + OrderStatuses.label(status));
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/cancel")
    public String cancelOrder(@RequestParam("id") Integer id,
                              RedirectAttributes redirectAttributes) {
        String error = orderService.cancelOrder(id, null);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn #" + id + " và hoàn kho.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/returns/approve")
    public String approveReturn(@RequestParam("id") Integer id,
                                RedirectAttributes redirectAttributes) {
        String error = returnRequestService.updateStatus(id, "APPROVED");
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã duyệt hoàn trả #" + id + " và hoàn kho.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/returns/reject")
    public String rejectReturn(@RequestParam("id") Integer id,
                               RedirectAttributes redirectAttributes) {
        String error = returnRequestService.updateStatus(id, "REJECTED");
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã từ chối yêu cầu hoàn trả #" + id);
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/discounts")
    public String discounts(Model model) {
        model.addAttribute("listCodes", discountCodeRepository.findAll());
        return "admin/discounts";
    }

    @PostMapping("/discounts/create")
    public String createDiscount(@RequestParam("code") String code,
                                 @RequestParam("percentOff") Integer percentOff,
                                 @RequestParam(value = "maxUses", required = false) Integer maxUses,
                                 @RequestParam(value = "description", required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        if (code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập mã.");
            return "redirect:/admin/discounts";
        }
        if (percentOff == null || percentOff < 1 || percentOff > 100) {
            redirectAttributes.addFlashAttribute("error", "Phần trăm giảm phải từ 1 đến 100.");
            return "redirect:/admin/discounts";
        }
        if (maxUses != null && maxUses < 1) {
            redirectAttributes.addFlashAttribute("error", "Số lượng phải từ 1 trở lên (hoặc để trống = không giới hạn).");
            return "redirect:/admin/discounts";
        }
        String normalized = code.trim().toUpperCase();
        if (discountCodeRepository.findByCodeIgnoreCase(normalized) != null) {
            redirectAttributes.addFlashAttribute("error", "Mã đã tồn tại.");
            return "redirect:/admin/discounts";
        }
        DiscountCode d = new DiscountCode();
        d.setCode(normalized);
        d.setPercentOff(percentOff);
        d.setMaxUses(maxUses);
        d.setUsedCount(0);
        d.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        d.setActive(true);
        discountCodeRepository.save(d);
        redirectAttributes.addFlashAttribute("success", "Đã tạo mã " + normalized);
        return "redirect:/admin/discounts";
    }

    @PostMapping("/discounts/toggle")
    public String toggleDiscount(@RequestParam("id") Integer id,
                                 @RequestParam("active") Boolean active,
                                 RedirectAttributes redirectAttributes) {
        DiscountCode d = discountCodeRepository.findById(id).orElse(null);
        if (d == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy mã.");
            return "redirect:/admin/discounts";
        }
        d.setActive(Boolean.TRUE.equals(active));
        discountCodeRepository.save(d);
        redirectAttributes.addFlashAttribute("success",
                Boolean.TRUE.equals(active) ? "Đã bật mã " + d.getCode() : "Đã tắt mã " + d.getCode());
        return "redirect:/admin/discounts";
    }

    @PostMapping("/discounts/delete")
    public String deleteDiscount(@RequestParam("id") Integer id,
                                 RedirectAttributes redirectAttributes) {
        if (discountCodeRepository.existsById(id)) {
            discountCodeRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa mã giảm giá.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy mã.");
        }
        return "redirect:/admin/discounts";
    }

    @GetMapping("/revenue")
    public String revenuePage(Model model) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<RevenueDTO> chartData = orderService.getDailyRevenue(from, now);
        double chartMax = 0;
        if (chartData != null) {
            for (RevenueDTO row : chartData) {
                if (row.getRevenue() != null && row.getRevenue() > chartMax) {
                    chartMax = row.getRevenue();
                }
            }
        }

        model.addAttribute("totalRevenue", orderService.getTotalRevenue());
        model.addAttribute("last7DaysRevenue", orderService.getRevenueBetween(from, now));
        model.addAttribute("deliveredCount", orderService.countDelivered());
        model.addAttribute("cancelledCount", orderService.countCancelled());
        model.addAttribute("cancelledAmount", orderService.getCancelledAmount());
        model.addAttribute("approvedReturnCount", returnRequestService.countByStatus("APPROVED"));
        model.addAttribute("approvedReturnAmount", returnRequestService.sumOrderAmountByStatus("APPROVED"));
        model.addAttribute("pendingReturnCount", returnRequestService.countByStatus("PENDING"));
        model.addAttribute("chartData", chartData);
        model.addAttribute("chartMax", chartMax);

        return "admin/revenue";
    }
}
