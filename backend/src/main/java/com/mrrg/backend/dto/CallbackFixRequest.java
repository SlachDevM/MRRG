package com.mrrg.backend.dto;

public class CallbackFixRequest {
    private Long jobDate;
    private String jobStartHour;

    public Long getJobDate() {
        return jobDate;
    }

    public void setJobDate(Long jobDate) {
        this.jobDate = jobDate;
    }

    public String getJobStartHour() {
        return jobStartHour;
    }

    public void setJobStartHour(String jobStartHour) {
        this.jobStartHour = jobStartHour;
    }
}
