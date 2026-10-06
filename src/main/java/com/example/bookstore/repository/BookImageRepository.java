package com.example.bookstore.repository;

import com.example.bookstore.entity.BookImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookImageRepository extends JpaRepository<BookImage, Integer> {

    List<BookImage> findByBook_IdOrderBySortOrderAscIdAsc(Integer bookId);

    java.util.Optional<BookImage> findByIdAndBook_Id(Integer id, Integer bookId);

    void deleteByBook_Id(Integer bookId);
}
