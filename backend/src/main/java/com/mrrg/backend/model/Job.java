package com.mrrg.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String clientPhone;

    @Column(nullable = false)
    private String clientAddress;

    @Column(name = "job_types", nullable = false, columnDefinition = "TEXT")
    private String jobTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "job_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate jobDate;

    @Column(name = "job_start_hour")
    private String jobStartHour;

    @Column(columnDefinition = "TEXT")
    private String assignedWorkers;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "before_photos", columnDefinition = "TEXT")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> beforePhotos = new ArrayList<>();

    @Column(name = "after_photos", columnDefinition = "TEXT")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> afterPhotos = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Integer priorityLevel;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    public Job() {
        this.status = JobStatus.PENDING;
        this.priorityLevel = 1;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Parses assigned worker IDs from the comma-separated string.
     * Format: "1,3,7"
     * 
     * Backward compatibility: If assignedWorkers contains non-numeric values (names from old format),
     * those entries are silently skipped during the migration period.
     * 
     * @return List of user IDs, empty list if none assigned or all values are non-numeric
     */
    public List<Long> getAssignedWorkerIds() {
        if (assignedWorkers == null || assignedWorkers.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(assignedWorkers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToLong(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        // Backward compatibility: skip non-numeric values (old worker names)
                        // These should be migrated to IDs separately
                        return -1L;
                    }
                })
                .filter(id -> id >= 0)
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Sets assigned workers from a list of user IDs.
     * Stores them in comma-separated format: "1,3,7"
     * 
     * @param workerIds list of user IDs
     */
    public void setAssignedWorkerIds(List<Long> workerIds) {
        if (workerIds == null || workerIds.isEmpty()) {
            this.assignedWorkers = null;
        } else {
            this.assignedWorkers = workerIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
    }

    /**
     * Checks if a user ID is in the assigned workers list.
     * Used for permission checks.
     * 
     * @param userId the user ID to check
     * @return true if the user is assigned to this job
     */
    public boolean isWorkerAssigned(Long userId) {
        if (userId == null) {
            return false;
        }
        return getAssignedWorkerIds().contains(userId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getJobTypes() {
        return jobTypes;
    }

    public void setJobTypes(String jobTypes) {
        this.jobTypes = jobTypes;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDate getJobDate() {
        return jobDate;
    }

    public void setJobDate(LocalDate jobDate) {
        this.jobDate = jobDate;
    }

    public String getJobStartHour() {
        return jobStartHour;
    }

    public void setJobStartHour(String jobStartHour) {
        this.jobStartHour = jobStartHour;
    }

    public String getAssignedWorkers() {
        return assignedWorkers;
    }

    public void setAssignedWorkers(String assignedWorkers) {
        this.assignedWorkers = assignedWorkers;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<String> getBeforePhotos() {
        return beforePhotos;
    }

    public void setBeforePhotos(List<String> beforePhotos) {
        this.beforePhotos = beforePhotos != null ? beforePhotos : new ArrayList<>();
    }

    public List<String> getAfterPhotos() {
        return afterPhotos;
    }

    public void setAfterPhotos(List<String> afterPhotos) {
        this.afterPhotos = afterPhotos != null ? afterPhotos : new ArrayList<>();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
