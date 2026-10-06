package com.example.bookstore.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class StaffPermissions {

    public static final String ORDER_CONFIRM = "ORDER_CONFIRM";
    public static final String ORDER_SHIP = "ORDER_SHIP";
    public static final String ORDER_DELIVER = "ORDER_DELIVER";
    public static final String ORDER_CANCEL = "ORDER_CANCEL";
    public static final String RETURN_APPROVE = "RETURN_APPROVE";
    public static final String RETURN_REJECT = "RETURN_REJECT";

    public static final Set<String> ALL = Set.of(
            ORDER_CONFIRM, ORDER_SHIP, ORDER_DELIVER, ORDER_CANCEL, RETURN_APPROVE, RETURN_REJECT
    );

    private StaffPermissions() {
    }

    public static Map<String, String> allLabels() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ORDER_CONFIRM, "Xác nhận đơn");
        map.put(ORDER_SHIP, "Đang vận chuyển");
        map.put(ORDER_DELIVER, "Đã giao");
        map.put(ORDER_CANCEL, "Hủy đơn");
        map.put(RETURN_APPROVE, "Duyệt hoàn trả");
        map.put(RETURN_REJECT, "Từ chối hoàn trả");
        return map;
    }

    public static String join(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        return permissions.stream()
                .filter(ALL::contains)
                .sorted()
                .collect(Collectors.joining(","));
    }

    public static Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(ALL::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<String> defaultAll() {
        return new LinkedHashSet<>(ALL);
    }

    public static String permissionForStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "CONFIRMED" -> ORDER_CONFIRM;
            case "SHIPPING" -> ORDER_SHIP;
            case "DELIVERED" -> ORDER_DELIVER;
            default -> null;
        };
    }

    public static Set<String> empty() {
        return Collections.emptySet();
    }
}
