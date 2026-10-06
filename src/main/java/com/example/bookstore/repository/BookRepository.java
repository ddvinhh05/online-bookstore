package com.example.bookstore.repository;

import com.example.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    @Query("""
            SELECT b FROM Book b
            LEFT JOIN b.category c
            WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<Book> search(@Param("q") String q);

    List<Book> findByCategory_Id(Integer categoryId);

    @Query("""
            SELECT b FROM Book b
            LEFT JOIN b.category c
            WHERE b.category.id = :categoryId
              AND (
                   LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    List<Book> searchInCategory(@Param("q") String q, @Param("categoryId") Integer categoryId);
}