package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_role", columnList = "role")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    /** USER / STAFF / ADMIN */
    @Column(nullable = false, length = 20)
    private String role = "USER";

    /** Quyền STAFF, cách nhau bởi dấu phẩy (ví dụ: ORDER_CONFIRM,ORDER_SHIP) */
    @Column(name = "permissions", length = 500)
    private String permissions;
}
