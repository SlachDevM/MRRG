package com.mrrg.backend.dto;

/**
 * Request DTO for assigning workers to a job.
 * Workers are specified by user IDs, not names.
 * 
 * Example: assignedWorkers = "1,3,7" (comma-separated user IDs)
 */
public class AssignWorkersRequest {
    private String assignedWorkers;

    public String getAssignedWorkers() {
        return assignedWorkers;
    }

    public void setAssignedWorkers(String assignedWorkers) {
        this.assignedWorkers = assignedWorkers;
    }
}
