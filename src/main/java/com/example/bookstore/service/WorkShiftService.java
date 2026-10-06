package com.example.bookstore.service;

import com.example.bookstore.entity.User;
import com.example.bookstore.entity.WorkShift;
import com.example.bookstore.model.UserRoles;
import com.example.bookstore.repository.WorkShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class WorkShiftService {

    @Autowired
    private WorkShiftRepository workShiftRepository;

    @Autowired
    private UserService userService;

    public List<WorkShift> getAll() {
        return workShiftRepository.findAllByOrderByWorkDateDescStartTimeAsc();
    }

    public String create(Integer userId, LocalDate workDate, LocalTime startTime, LocalTime endTime, String note) {
        User user = userService.getById(userId);
        if (user == null) {
            return "Không tìm thấy nhân viên.";
        }
        if (!UserRoles.STAFF.equalsIgnoreCase(user.getRole())
                && !UserRoles.ADMIN.equalsIgnoreCase(user.getRole())) {
            return "Chỉ gán ca cho tài khoản STAFF hoặc ADMIN.";
        }
        if (workDate == null || startTime == null || endTime == null) {
            return "Vui lòng nhập đủ ngày và giờ làm.";
        }
        if (!endTime.isAfter(startTime)) {
            return "Giờ kết thúc phải sau giờ bắt đầu.";
        }

        WorkShift shift = new WorkShift();
        shift.setUser(user);
        shift.setWorkDate(workDate);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setNote(note != null && !note.isBlank() ? note.trim() : null);
        workShiftRepository.save(shift);
        return null;
    }

    public void delete(Integer id) {
        workShiftRepository.deleteById(id);
    }
}
