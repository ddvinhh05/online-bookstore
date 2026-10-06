package com.example.bookstore.service;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.StaffPermissions;
import com.example.bookstore.model.UserRoles;
import com.example.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getStaffUsers() {
        return userRepository.findAll().stream()
                .filter(u -> UserRoles.STAFF.equalsIgnoreCase(u.getRole())
                        || UserRoles.ADMIN.equalsIgnoreCase(u.getRole()))
                .toList();
    }

    public List<User> getStaffOnlyUsers() {
        return userRepository.findAll().stream()
                .filter(u -> UserRoles.STAFF.equalsIgnoreCase(u.getRole()))
                .toList();
    }

    public User getById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

    /** Tạo tài khoản nhân viên mới (role STAFF). */
    public String createStaff(String username, String password, String fullName,
                              String email, String phone, String[] permissionCodes) {
        if (username == null || username.isBlank()) {
            return "Vui lòng nhập tên đăng nhập.";
        }
        if (password == null || password.isBlank()) {
            return "Vui lòng nhập mật khẩu.";
        }
        if (fullName == null || fullName.isBlank()) {
            return "Vui lòng nhập họ tên.";
        }
        username = username.trim();
        if (usernameExists(username)) {
            return "Tên đăng nhập đã tồn tại.";
        }
        if (email != null && !email.isBlank() && emailExists(email.trim())) {
            return "Email đã được sử dụng.";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setFullName(fullName.trim());
        user.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        user.setRole(UserRoles.STAFF);

        Set<String> selected = new HashSet<>();
        if (permissionCodes != null) {
            selected.addAll(Arrays.asList(permissionCodes));
        }
        if (selected.isEmpty()) {
            selected = StaffPermissions.defaultAll();
        }
        user.setPermissions(StaffPermissions.join(selected));
        userRepository.save(user);
        return null;
    }

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username) != null;
    }

    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.findByEmail(email) != null;
    }

    public User register(User user) {
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole(UserRoles.USER);
        }
        if (!UserRoles.STAFF.equals(user.getRole())) {
            user.setPermissions(null);
        }
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public String updateRoleAndPermissions(Integer userId,
                                           String newRole,
                                           String[] permissionCodes,
                                           Integer currentAdminId) {
        if (!UserRoles.isValid(newRole)) {
            return "Role không hợp lệ. Chỉ chọn USER, STAFF hoặc ADMIN.";
        }
        User user = getById(userId);
        if (user == null) {
            return "Không tìm thấy tài khoản.";
        }
        if (currentAdminId != null && currentAdminId.equals(userId) && !UserRoles.ADMIN.equals(newRole)) {
            return "Bạn không thể tự hạ quyền tài khoản admin đang đăng nhập.";
        }

        user.setRole(newRole);
        if (UserRoles.STAFF.equals(newRole)) {
            Set<String> selected = new HashSet<>();
            if (permissionCodes != null) {
                selected.addAll(Arrays.asList(permissionCodes));
            }
            user.setPermissions(StaffPermissions.join(selected));
        } else {
            user.setPermissions(null);
        }
        userRepository.save(user);
        return null;
    }

    public boolean hasPermission(User user, String permission) {
        if (user == null || permission == null) {
            return false;
        }
        if (UserRoles.ADMIN.equalsIgnoreCase(user.getRole())) {
            return true;
        }
        if (!UserRoles.STAFF.equalsIgnoreCase(user.getRole())) {
            return false;
        }
        return StaffPermissions.parse(user.getPermissions()).contains(permission);
    }

    public Set<String> permissionSet(User user) {
        if (user == null) {
            return StaffPermissions.empty();
        }
        if (UserRoles.ADMIN.equalsIgnoreCase(user.getRole())) {
            return StaffPermissions.defaultAll();
        }
        return StaffPermissions.parse(user.getPermissions());
    }

    public User toSessionUser(User user) {
        User sessionUser = new User();
        sessionUser.setId(user.getId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setFullName(user.getFullName());
        sessionUser.setEmail(user.getEmail());
        sessionUser.setPhone(user.getPhone());
        sessionUser.setAddress(user.getAddress());
        sessionUser.setRole(user.getRole());
        sessionUser.setPermissions(user.getPermissions());
        sessionUser.setPassword(null);
        return sessionUser;
    }

    /**
     * Cập nhật thông tin tài khoản đang đăng nhập.
     * @return null nếu OK, ngược lại thông báo lỗi
     */
    public String updateProfile(Integer userId, String fullName, String email, String phone,
                                String address, String currentPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            return "Không tìm thấy tài khoản.";
        }
        if (fullName == null || fullName.isBlank()) {
            return "Vui lòng nhập họ tên.";
        }

        String emailTrim = email != null ? email.trim() : "";
        if (!emailTrim.isEmpty()) {
            User byEmail = userRepository.findByEmail(emailTrim);
            if (byEmail != null && !byEmail.getId().equals(userId)) {
                return "Email đã được sử dụng bởi tài khoản khác.";
            }
            user.setEmail(emailTrim);
        } else {
            user.setEmail(null);
        }

        boolean changingPassword = newPassword != null && !newPassword.isBlank();
        if (changingPassword) {
            if (currentPassword == null || currentPassword.isBlank()) {
                return "Vui lòng nhập mật khẩu cũ để đổi mật khẩu.";
            }
            if (!currentPassword.equals(user.getPassword())) {
                return "Mật khẩu cũ không đúng.";
            }
            if (newPassword.equals(currentPassword)) {
                return "Mật khẩu mới phải khác mật khẩu cũ.";
            }
            user.setPassword(newPassword);
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        user.setAddress(address != null && !address.isBlank() ? address.trim() : null);
        userRepository.save(user);
        return null;
    }
}
