package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "books",
        indexes = {
                @Index(name = "idx_books_category", columnList = "category_id"),
                @Index(name = "idx_books_title", columnList = "title")
        }
)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    /** Ảnh bìa chính (dùng ở danh sách / card). */
    @Column(name = "image_path", length = 255)
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Không orphanRemoval: form lưu sách không mang collection ảnh, tránh xóa nhầm gallery. */
    @OneToMany(mappedBy = "book", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("sortOrder ASC, id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BookImage> images = new ArrayList<>();
}
