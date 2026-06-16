package com.mrrg.backend.repository;

import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatusOrderByPriorityLevelDesc(JobStatus status);
    List<Job> findByStatusInOrderByPriorityLevelDesc(List<JobStatus> statuses);
    List<Job> findByJobDateBetween(Long startDate, Long endDate);

    List<Job> findByStatusAndJobDateBetweenOrderByJobStartHourAsc(JobStatus status, Long startDate, Long endDate);

    List<Job> findByStatusInAndJobDateBetweenOrderByJobStartHourAsc(
            List<JobStatus> statuses, Long startDate, Long endDate);
}
