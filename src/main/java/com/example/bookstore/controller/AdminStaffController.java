package com.example.bookstore.controller;

import com.example.bookstore.model.StaffPermissions;
import com.example.bookstore.service.UserService;
import com.example.bookstore.service.WorkShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {

    @Autowired
    private UserService userService;

    @Autowired
    private WorkShiftService workShiftService;

    @GetMapping
    public String staffPage(Model model) {
        model.addAttribute("staffList", userService.getStaffUsers());
        model.addAttribute("staffPermissionLabels", StaffPermissions.allLabels());
        model.addAttribute("shifts", workShiftService.getAll());
        return "admin/staff";
    }

    @PostMapping("/create")
    public String createStaff(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              @RequestParam("fullName") String fullName,
                              @RequestParam(value = "email", required = false) String email,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "permissions", required = false) String[] permissions,
                              RedirectAttributes redirectAttributes) {
        String error = userService.createStaff(username, password, fullName, email, phone, permissions);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã tạo tài khoản nhân viên: " + username);
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/shifts")
    public String addShift(@RequestParam("userId") Integer userId,
                           @RequestParam("workDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                           @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                           @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                           @RequestParam(value = "note", required = false) String note,
                           RedirectAttributes redirectAttributes) {
        String error = workShiftService.create(userId, workDate, startTime, endTime, note);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Đã thêm ca làm việc.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/shifts/delete")
    public String deleteShift(@RequestParam("id") Integer id, RedirectAttributes redirectAttributes) {
        workShiftService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa ca làm việc.");
        return "redirect:/admin/staff";
    }
}
