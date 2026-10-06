package com.example.bookstore.controller;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.UserRoles;
import com.example.bookstore.service.BookService;
import com.example.bookstore.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            if (UserRoles.ADMIN.equalsIgnoreCase(currentUser.getRole())) {
                return "redirect:/admin";
            }
            if (UserRoles.STAFF.equalsIgnoreCase(currentUser.getRole())) {
                return "redirect:/staff";
            }
        }
        model.addAttribute("featuredBooks", bookService.getFeaturedBooks(8));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "index";
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "admin/index";
    }
}
