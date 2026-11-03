package com.glassstore.lab.repository;

import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.LabJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabJobRepository extends JpaRepository<LabJob, Long> {
    List<LabJob> findByStatusOrderByCreatedAtDesc(LabJobStatus status);
    List<LabJob> findAllByOrderByCreatedAtDesc();
}
