package com.glassstore.lab.repository;

import com.glassstore.lab.entity.LabJobStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabJobStatusHistoryRepository extends JpaRepository<LabJobStatusHistory, Long> {
    List<LabJobStatusHistory> findByLabJobIdOrderByChangedAtAsc(Long labJobId);
}
