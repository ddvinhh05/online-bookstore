package com.example.bookstore.repository;

import com.example.bookstore.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer> {

    @Query("SELECT r FROM ReturnRequest r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.order ORDER BY r.createdAt DESC")
    List<ReturnRequest> findAllWithDetails();

    List<ReturnRequest> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    List<ReturnRequest> findByOrder_IdOrderByCreatedAtDesc(Integer orderId);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(r.order.totalAmount), 0) FROM ReturnRequest r WHERE r.status = :status")
    Double sumOrderAmountByStatus(@Param("status") String status);

    boolean existsByOrder_IdAndStatus(Integer orderId, String status);

    boolean existsByOrder_IdAndStatusIn(Integer orderId, List<String> statuses);

    @Query("SELECT r FROM ReturnRequest r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.order WHERE r.id = :id")
    Optional<ReturnRequest> findByIdWithDetails(@Param("id") Integer id);
}
