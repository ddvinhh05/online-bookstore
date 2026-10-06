package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "order_details",
        indexes = {
                @Index(name = "idx_order_details_order", columnList = "order_id"),
                @Index(name = "idx_order_details_book", columnList = "book_id")
        }
)
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer quantity;

    /** Đơn giá tại thời điểm đặt (snapshot). Thành tiền = unitPrice × quantity. */
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Transient
    public Double getLineTotal() {
        if (unitPrice == null || quantity == null) {
            return null;
        }
        return unitPrice * quantity;
    }
}
