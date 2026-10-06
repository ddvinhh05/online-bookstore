package com.example.bookstore.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Luồng trạng thái đơn hàng:
 * PENDING → CONFIRMED → SHIPPING → DELIVERED
 * (có thể CANCELLED trước khi giao xong)
 */
public final class OrderStatuses {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String SHIPPING = "SHIPPING";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";

    public static final Set<String> ALL = Set.of(
            PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
    );

    /** Đơn tính vào doanh thu. */
    public static final List<String> REVENUE_STATUSES = List.of(DELIVERED, "COMPLETED");

    private OrderStatuses() {
    }

    public static String label(String status) {
        if (status == null) {
            return "Không rõ";
        }
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case SHIPPING -> "Đang vận chuyển";
            case DELIVERED -> "Đã giao";
            case CANCELLED -> "Đã hủy";
            case "COMPLETED" -> "Đã giao";
            default -> status;
        };
    }

    public static String nextStatus(String current) {
        if (current == null) {
            return CONFIRMED;
        }
        return switch (current) {
            case PENDING -> CONFIRMED;
            case CONFIRMED -> SHIPPING;
            case SHIPPING -> DELIVERED;
            case "COMPLETED" -> null;
            default -> null;
        };
    }

    public static boolean canCancel(String status) {
        return PENDING.equals(status) || CONFIRMED.equals(status) || SHIPPING.equals(status);
    }

    public static boolean isFinal(String status) {
        return DELIVERED.equals(status) || CANCELLED.equals(status) || "COMPLETED".equals(status);
    }

    public static Map<String, String> allLabels() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PENDING, label(PENDING));
        map.put(CONFIRMED, label(CONFIRMED));
        map.put(SHIPPING, label(SHIPPING));
        map.put(DELIVERED, label(DELIVERED));
        map.put(CANCELLED, label(CANCELLED));
        return map;
    }
}
