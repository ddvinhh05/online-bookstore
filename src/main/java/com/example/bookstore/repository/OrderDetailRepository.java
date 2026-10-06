package com.example.bookstore.repository;

import com.example.bookstore.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    List<OrderDetail> findByOrder_Id(Integer orderId);

    @Query("SELECT d FROM OrderDetail d LEFT JOIN FETCH d.book WHERE d.order.id = :orderId")
    List<OrderDetail> findDetailsWithBook(@Param("orderId") Integer orderId);
}
