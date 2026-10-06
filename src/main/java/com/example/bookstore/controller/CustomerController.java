package com.example.bookstore.controller;

import com.example.bookstore.entity.DiscountCode;
import com.example.bookstore.entity.User;
import com.example.bookstore.service.BookService;
import com.example.bookstore.service.CartService;
import com.example.bookstore.service.CategoryService;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.service.ReturnRequestService;
import com.example.bookstore.model.OrderStatuses;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReturnRequestService returnRequestService;

    @Autowired
    private CartService cartService;

    @GetMapping("/shop")
    public String shop(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "categoryId", required = false) Integer categoryId,
                       @RequestParam(value = "view", required = false) String view,
                       Model model) {
        String mode = view == null ? "all" : view.trim().toLowerCase();
        String pageTitle;
        String pageLead;
        String breadcrumb;
        String activeNav;

        if ("featured".equals(mode)) {
            model.addAttribute("listBooks", bookService.getFeaturedBooks(24));
            pageTitle = "Sản phẩm nổi bật";
            pageLead = "Những cuốn được tuyển chọn trên kệ Trí Tuệ.";
            breadcrumb = "Sản phẩm nổi bật";
            activeNav = "featured";
        } else if ("new".equals(mode)) {
            model.addAttribute("listBooks", bookService.getNewestBooks(24));
            pageTitle = "Sách mới";
            pageLead = "Các đầu sách mới cập nhật gần đây.";
            breadcrumb = "Sách mới";
            activeNav = "new";
        } else {
            model.addAttribute("listBooks", bookService.searchBooks(q, categoryId));
            pageTitle = "Tất cả sản phẩm";
            pageLead = "Tìm theo tên hoặc danh mục — bấm vào sách để xem chi tiết.";
            breadcrumb = "Tất cả sản phẩm";
            activeNav = "shop";
        }

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("view", mode);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageLead", pageLead);
        model.addAttribute("breadcrumb", breadcrumb);
        model.addAttribute("activeNav", activeNav);
        return "shop";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "categories";
    }

    @GetMapping("/shop/books/{id}")
    public String bookDetail(@PathVariable("id") Integer id,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        var book = bookService.getBookById(id);
        if (book == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sách.");
            return "redirect:/shop";
        }
        model.addAttribute("book", book);
        model.addAttribute("galleryImages", bookService.getDisplayImagePaths(book));
        return "book-detail";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("bookId") Integer bookId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            @RequestParam(value = "redirect", required = false) String redirect,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản admin không dùng giỏ hàng khách.");
            return "redirect:/shop";
        }

        String error = cartService.addToCart(session, bookId, quantity);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã thêm vào giỏ hàng.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }

        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            return "redirect:" + redirect;
        }
        return "redirect:/shop";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cartItems", cartService.getItems(session));
        model.addAttribute("cartSubtotal", cartService.getSubtotal(session));
        model.addAttribute("discountAmount", cartService.getDiscountAmount(session));
        model.addAttribute("cartTotal", cartService.getTotal(session));
        model.addAttribute("appliedDiscount", cartService.getAppliedDiscount(session));
        return "cart";
    }

    @PostMapping("/cart/coupon")
    public String applyCoupon(@RequestParam("code") String code,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        String error = cartService.applyCoupon(session, code);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã áp dụng mã giảm giá.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/coupon/remove")
    public String removeCoupon(HttpSession session, RedirectAttributes redirectAttributes) {
        cartService.removeCoupon(session);
        redirectAttributes.addFlashAttribute("success", "Đã bỏ mã giảm giá.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam("bookId") Integer bookId,
                             @RequestParam("quantity") Integer quantity,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String error = cartService.updateQuantity(session, bookId, quantity);
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
        } else {
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật giỏ hàng.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("bookId") Integer bookId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        cartService.removeItem(session, bookId);
        redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getId() == null) {
            redirectAttributes.addFlashAttribute("error", "Đăng nhập tài khoản khách để thanh toán.");
            return "redirect:/login?redirect=/cart";
        }
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản admin không đặt hàng được.");
            return "redirect:/cart";
        }

        try {
            DiscountCode discount = cartService.getAppliedDiscount(session);
            String couponCode = discount != null ? discount.getCode() : null;
            Integer percentOff = discount != null ? discount.getPercentOff() : null;
            String error = orderService.placeOrderFromCart(
                    currentUser.getId(), cartService.getItems(session), couponCode, percentOff);
            if (error == null) {
                if (couponCode != null) {
                    cartService.consumeCoupon(couponCode);
                }
                cartService.clear(session);
                redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công!");
                return "redirect:/my-orders";
            }
            redirectAttributes.addFlashAttribute("error", error);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Lỗi thanh toán: " + ex.getMessage());
        }
        return "redirect:/cart";
    }

    @GetMapping("/my-orders")
    public String myOrders(@RequestParam(value = "returnOrderId", required = false) Integer returnOrderId,
                           HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login?redirect=/my-orders";
        }
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return "redirect:/admin/orders";
        }

        var listOrders = orderService.getOrdersByUserId(currentUser.getId());
        var canReturnMap = returnRequestService.buildCanReturnMap(listOrders);
        model.addAttribute("listOrders", listOrders);
        model.addAttribute("listReturns", returnRequestService.getReturnRequestsByUserId(currentUser.getId()));
        model.addAttribute("statusLabels", OrderStatuses.allLabels());
        model.addAttribute("returnStatusLabels", ReturnRequestService.allStatusLabels());
        model.addAttribute("canReturnMap", canReturnMap);
        model.addAttribute("hasReturnableOrders", canReturnMap.containsValue(Boolean.TRUE));
        model.addAttribute("returnOrderId", returnOrderId);
        return "my-orders";
    }

    @PostMapping("/my-orders/returns")
    public String requestReturn(@RequestParam("orderId") Integer orderId,
                                @RequestParam("reason") String reason,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login?redirect=/my-orders";
        }

        String error = returnRequestService.createReturnRequest(
                orderId, currentUser.getId(), reason, imageFile);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã gửi yêu cầu hoàn trả cho đơn #" + orderId + ".");
            return "redirect:/my-orders";
        }
        redirectAttributes.addFlashAttribute("error", error);
        return "redirect:/my-orders?returnOrderId=" + orderId;
    }

    @PostMapping("/my-orders/cancel")
    public String cancelMyOrder(@RequestParam("id") Integer id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login?redirect=/my-orders";
        }

        String error = orderService.cancelOrder(id, currentUser.getId());
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn #" + id + ".");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/my-orders";
    }
}
