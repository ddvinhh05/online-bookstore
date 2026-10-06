package com.example.bookstore.repository;

import com.example.bookstore.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Integer> {

    @Query("SELECT s FROM WorkShift s LEFT JOIN FETCH s.user ORDER BY s.workDate DESC, s.startTime ASC")
    List<WorkShift> findAllByOrderByWorkDateDescStartTimeAsc();
}
