package com.mrrg.backend.repository;

import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatusOrderByPriorityLevelDesc(JobStatus status);
    List<Job> findByStatusInOrderByPriorityLevelDesc(List<JobStatus> statuses);
    List<Job> findByJobDateBetween(LocalDate startDate, LocalDate endDate);

    List<Job> findByStatusAndJobDateBetweenOrderByJobStartHourAsc(JobStatus status, LocalDate startDate, LocalDate endDate);

    List<Job> findByStatusInAndJobDateBetweenOrderByJobStartHourAsc(
            List<JobStatus> statuses, LocalDate startDate, LocalDate endDate);
}
