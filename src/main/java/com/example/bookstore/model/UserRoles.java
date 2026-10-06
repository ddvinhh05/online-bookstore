package com.example.bookstore.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class UserRoles {

    public static final String USER = "USER";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";

    public static final Set<String> ALL = Set.of(USER, STAFF, ADMIN);

    private UserRoles() {
    }

    public static boolean isValid(String role) {
        return role != null && ALL.contains(role);
    }

    public static String label(String role) {
        if (role == null) {
            return "Không rõ";
        }
        return switch (role) {
            case USER -> "Khách hàng";
            case STAFF -> "Nhân viên";
            case ADMIN -> "Quản trị";
            default -> role;
        };
    }

    public static Map<String, String> allLabels() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(USER, label(USER));
        map.put(STAFF, label(STAFF));
        map.put(ADMIN, label(ADMIN));
        return map;
    }
}
