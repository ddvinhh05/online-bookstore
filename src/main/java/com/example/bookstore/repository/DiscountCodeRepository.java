package com.example.bookstore.repository;

import com.example.bookstore.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Integer> {
    DiscountCode findByCodeIgnoreCase(String code);
}
