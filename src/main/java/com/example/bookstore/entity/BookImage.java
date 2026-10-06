package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "book_images",
        indexes = {
                @Index(name = "idx_book_images_book", columnList = "book_id")
        }
)
public class BookImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Book book;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
