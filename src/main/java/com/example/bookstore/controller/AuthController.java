package com.example.bookstore.controller;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.UserRoles;
import com.example.bookstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "redirect", required = false) String redirect,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute("currentUser") != null) {
            return redirectHome(session);
        }
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam(value = "redirect", required = false) String redirect,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User user = userService.login(username.trim(), password);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Sai tên đăng nhập hoặc mật khẩu.");
            if (redirect != null && !redirect.isBlank()) {
                return "redirect:/login?redirect=" + redirect;
            }
            return "redirect:/login";
        }

        session.setAttribute("currentUser", userService.toSessionUser(user));
        redirectAttributes.addFlashAttribute("success", "Đăng nhập thành công. Xin chào " + user.getFullName() + "!");

        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            if (redirect.startsWith("/admin") && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                return homeByRole(user);
            }
            if (redirect.startsWith("/staff")
                    && !"STAFF".equalsIgnoreCase(user.getRole())
                    && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                return homeByRole(user);
            }
            return "redirect:" + redirect;
        }
        return homeByRole(user);
    }

    @GetMapping("/register")
    public String showRegister(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") != null) {
            return redirectHome(session);
        }
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user,
                           @RequestParam("confirmPassword") String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()
                || user.getFullName() == null || user.getFullName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng điền đầy đủ thông tin bắt buộc.");
            return "redirect:/register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/register";
        }

        if (userService.usernameExists(user.getUsername().trim())) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "redirect:/register";
        }

        if (user.getEmail() != null && !user.getEmail().isBlank() && userService.emailExists(user.getEmail().trim())) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng.");
            return "redirect:/register";
        }

        user.setUsername(user.getUsername().trim());
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim());
        }
        user.setRole("USER");
        userService.register(user);

        redirectAttributes.addFlashAttribute("success", "Đăng ký thành công. Hãy đăng nhập để tiếp tục.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Bạn đã đăng xuất.");
        return "redirect:/login";
    }

    @GetMapping("/account")
    public String accountPage(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null || sessionUser.getId() == null) {
            return "redirect:/login?redirect=/account";
        }
        User user = userService.getById(sessionUser.getId());
        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roleLabel", UserRoles.label(user.getRole()));
        return "account";
    }

    @PostMapping("/account")
    public String updateAccount(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "email", required = false) String email,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "address", required = false) String address,
                                @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null || sessionUser.getId() == null) {
            return "redirect:/login?redirect=/account";
        }

        String error = userService.updateProfile(
                sessionUser.getId(), fullName, email, phone, address,
                currentPassword, newPassword);
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/account";
        }

        User updated = userService.getById(sessionUser.getId());
        session.setAttribute("currentUser", userService.toSessionUser(updated));
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật thông tin tài khoản.");
        return "redirect:/account";
    }

    private String redirectHome(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        return homeByRole(currentUser);
    }

    private String homeByRole(User user) {
        if (user == null) {
            return "redirect:/";
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return "redirect:/admin";
        }
        if ("STAFF".equalsIgnoreCase(user.getRole())) {
            return "redirect:/staff";
        }
        return "redirect:/";
    }
}
