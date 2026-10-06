package com.example.bookstore.repository;

import com.example.bookstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUser_IdOrderByOrderDateDesc(Integer userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user ORDER BY o.orderDate DESC")
    List<Order> findAllWithUser();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.id = :id")
    Optional<Order> findByIdWithUser(@Param("id") Integer id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses")
    Double sumRevenueByStatuses(@Param("statuses") List<String> statuses);

    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<String> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses AND o.orderDate BETWEEN :from AND :to")
    Double sumRevenueByStatusesAndDateRange(@Param("statuses") List<String> statuses,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    long countByStatusAndOrderDateBetween(String status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses AND o.orderDate BETWEEN :from AND :to")
    long countByStatusInAndOrderDateBetween(@Param("statuses") List<String> statuses,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.status IN :statuses AND o.orderDate BETWEEN :from AND :to ORDER BY o.orderDate DESC")
    List<Order> findByStatusesAndDateRange(@Param("statuses") List<String> statuses,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders();
}
