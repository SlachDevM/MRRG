package com.mrrg.backend.repository;

import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @EntityGraph(attributePaths = {"assignments", "assignments.user"})
    @Override
    Optional<Job> findById(Long id);

    @EntityGraph(attributePaths = {"assignments", "assignments.user"})
    @Override
    List<Job> findAll();

    @EntityGraph(attributePaths = {"assignments", "assignments.user"})
    List<Job> findByStatusOrderByPriorityLevelDesc(JobStatus status);

    @EntityGraph(attributePaths = {"assignments", "assignments.user"})
    List<Job> findByStatusInOrderByPriorityLevelDesc(List<JobStatus> statuses);

    List<Job> findByJobDateBetween(LocalDate startDate, LocalDate endDate);

    List<Job> findByStatusAndJobDateBetweenOrderByJobStartHourAsc(JobStatus status, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"assignments", "assignments.user"})
    List<Job> findByStatusInAndJobDateBetweenOrderByJobStartHourAsc(
            List<JobStatus> statuses, LocalDate startDate, LocalDate endDate);
}
