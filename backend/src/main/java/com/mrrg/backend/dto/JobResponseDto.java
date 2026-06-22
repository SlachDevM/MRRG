package com.mrrg.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for Job with resolved worker display names.
 * Provides both assignedWorkerIds and assignedWorkerDetails for clients.
 */
public class JobResponseDto {
    private Long id;
    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private String jobTypes;
    private JobStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate jobDate;

    private String jobStartHour;
    private String assignedWorkers;  // Comma-separated user IDs: "1,3,7"
    private List<UserSummary> assignedWorkerDetails;  // Display names resolved from User table
    private String details;
    private List<String> beforePhotos;
    private List<String> afterPhotos;
    private String notes;
    private Integer priorityLevel;
    private Long createdBy;
    private Long createdAt;
    private Long updatedAt;

    public JobResponseDto() {
    }

    /**
     * Convert Job entity to response DTO.
     * Note: assignedWorkerDetails is populated by JobService when building the response DTO.
     */
    public JobResponseDto(Job job) {
        this.id = job.getId();
        this.clientName = job.getClientName();
        this.clientPhone = job.getClientPhone();
        this.clientAddress = job.getClientAddress();
        this.jobTypes = job.getJobTypes();
        this.status = job.getStatus();
        this.jobDate = job.getJobDate();
        this.jobStartHour = job.getJobStartHour();
        this.assignedWorkers = job.getAssignedWorkers();  // Raw ID string
        this.details = job.getDetails();
        this.beforePhotos = job.getBeforePhotos();
        this.afterPhotos = job.getAfterPhotos();
        this.notes = job.getNotes();
        this.priorityLevel = job.getPriorityLevel();
        this.createdBy = job.getCreatedBy();
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();
    }

    // Getters and setters
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

    public List<UserSummary> getAssignedWorkerDetails() {
        return assignedWorkerDetails;
    }

    public void setAssignedWorkerDetails(List<UserSummary> assignedWorkerDetails) {
        this.assignedWorkerDetails = assignedWorkerDetails;
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
        this.beforePhotos = beforePhotos;
    }

    public List<String> getAfterPhotos() {
        return afterPhotos;
    }

    public void setAfterPhotos(List<String> afterPhotos) {
        this.afterPhotos = afterPhotos;
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
