package com.example.bookstore.controller;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.OrderStatuses;
import com.example.bookstore.model.StaffPermissions;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.service.ReturnRequestService;
import com.example.bookstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReturnRequestService returnRequestService;

    @Autowired
    private UserService userService;

    private User freshUser(HttpSession session) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null || sessionUser.getId() == null) {
            return null;
        }
        User dbUser = userService.getById(sessionUser.getId());
        if (dbUser == null) {
            return sessionUser;
        }
        User refreshed = userService.toSessionUser(dbUser);
        session.setAttribute("currentUser", refreshed);
        return refreshed;
    }

    @GetMapping
    public String home() {
        return "staff/index";
    }

    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        User currentUser = freshUser(session);
        model.addAttribute("listOrders", orderService.getAllOrders());
        model.addAttribute("listReturns", returnRequestService.getAllReturnRequests());
        model.addAttribute("statusLabels", OrderStatuses.allLabels());
        model.addAttribute("perms", userService.permissionSet(currentUser));
        return "staff/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Integer id,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        freshUser(session);
        var order = orderService.getOrderById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/staff/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderService.getOrderDetails(id));
        model.addAttribute("statusLabels", OrderStatuses.allLabels());
        model.addAttribute("backUrl", "/staff/orders");
        return "order-detail";
    }

    @GetMapping("/returns/{id}")
    public String returnDetail(@PathVariable("id") Integer id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User currentUser = freshUser(session);
        var ret = returnRequestService.getById(id);
        if (ret == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy yêu cầu hoàn trả #" + id);
            return "redirect:/staff/orders";
        }
        model.addAttribute("ret", ret);
        model.addAttribute("statusLabel", ReturnRequestService.statusLabel(ret.getStatus()));
        if (ret.getOrder() != null) {
            model.addAttribute("orderDetails", orderService.getOrderDetails(ret.getOrder().getId()));
            model.addAttribute("statusLabels", OrderStatuses.allLabels());
        }
        model.addAttribute("backUrl", "/staff/orders");
        model.addAttribute("orderDetailUrl", ret.getOrder() != null ? "/staff/orders/" + ret.getOrder().getId() : null);
        model.addAttribute("perms", userService.permissionSet(currentUser));
        return "return-detail";
    }

    @PostMapping("/orders/status")
    public String updateStatus(@RequestParam("id") Integer id,
                               @RequestParam("status") String status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = freshUser(session);
        String needed = StaffPermissions.permissionForStatus(status);
        if (needed == null || !userService.hasPermission(currentUser, needed)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không được cấp quyền thao tác này.");
            return "redirect:/staff/orders";
        }

        String error = orderService.updateStatus(id, status);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success",
                    "Đơn #" + id + " → " + OrderStatuses.label(status));
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/orders/cancel")
    public String cancel(@RequestParam("id") Integer id,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        User currentUser = freshUser(session);
        if (!userService.hasPermission(currentUser, StaffPermissions.ORDER_CANCEL)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không được cấp quyền hủy đơn.");
            return "redirect:/staff/orders";
        }

        String error = orderService.cancelOrder(id, null);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn #" + id + " và hoàn kho.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/returns/approve")
    public String approveReturn(@RequestParam("id") Integer id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = freshUser(session);
        if (!userService.hasPermission(currentUser, StaffPermissions.RETURN_APPROVE)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không được cấp quyền duyệt hoàn trả.");
            return "redirect:/staff/orders";
        }

        String error = returnRequestService.updateStatus(id, "APPROVED");
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã duyệt hoàn trả #" + id + " và hoàn kho.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/returns/reject")
    public String rejectReturn(@RequestParam("id") Integer id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User currentUser = freshUser(session);
        if (!userService.hasPermission(currentUser, StaffPermissions.RETURN_REJECT)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không được cấp quyền từ chối hoàn trả.");
            return "redirect:/staff/orders";
        }

        String error = returnRequestService.updateStatus(id, "REJECTED");
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã từ chối yêu cầu hoàn trả #" + id);
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/staff/orders";
    }
}
