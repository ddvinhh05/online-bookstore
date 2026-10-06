package com.example.bookstore.controller;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.StaffPermissions;
import com.example.bookstore.model.UserRoles;
import com.example.bookstore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        List<User> staffOnly = userService.getStaffOnlyUsers();
        model.addAttribute("listUsers", staffOnly);
        model.addAttribute("roleLabels", UserRoles.allLabels());
        model.addAttribute("staffPermissionLabels", StaffPermissions.allLabels());

        Map<Integer, Set<String>> userPerms = new HashMap<>();
        for (User u : staffOnly) {
            userPerms.put(u.getId(), StaffPermissions.parse(u.getPermissions()));
        }
        model.addAttribute("userPerms", userPerms);
        return "admin/users";
    }

    @PostMapping("/role")
    public String updateRole(@RequestParam("id") Integer id,
                             @RequestParam("role") String role,
                             @RequestParam(value = "permissions", required = false) String[] permissions,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        Integer adminId = currentUser != null ? currentUser.getId() : null;

        String error = userService.updateRoleAndPermissions(id, role, permissions, adminId);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success",
                    "Đã cập nhật quyền tài khoản #" + id + " → " + UserRoles.label(role));
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/users";
    }
}
