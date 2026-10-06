package com.example.bookstore.config;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.UserRoles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StaffInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect=/staff");
            return false;
        }

        String role = currentUser.getRole();
        if (!UserRoles.STAFF.equalsIgnoreCase(role) && !UserRoles.ADMIN.equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }

        return true;
    }
}
