package com.example.bookstore.controller;

import com.example.bookstore.entity.User;
import com.example.bookstore.service.CartService;
import com.example.bookstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @ModelAttribute("currentUser")
    public User currentUser(HttpSession session) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null || sessionUser.getId() == null) {
            return sessionUser;
        }
        User fresh = userService.getById(sessionUser.getId());
        if (fresh == null) {
            return sessionUser;
        }
        return userService.toSessionUser(fresh);
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        return cartService.getItemCount(session);
    }
}
