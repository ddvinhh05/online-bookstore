package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "discount_codes",
        indexes = {
                @Index(name = "idx_discount_active", columnList = "active")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "percent_off", nullable = false)
    private Integer percentOff;

    @Column(length = 200)
    private String description;

    /** Số lần được phép dùng. null = không giới hạn. */
    @Column(name = "max_uses")
    private Integer maxUses;

    /** Đã dùng bao nhiêu lần. */
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Boolean active = true;

    public boolean hasRemainingUses() {
        if (maxUses == null) {
            return true;
        }
        int used = usedCount != null ? usedCount : 0;
        return used < maxUses;
    }

    public Integer getRemainingUses() {
        if (maxUses == null) {
            return null;
        }
        int used = usedCount != null ? usedCount : 0;
        return Math.max(0, maxUses - used);
    }
}
