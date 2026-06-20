package com.mrrg.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class CallbackFixRequest {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate jobDate;
    private String jobStartHour;

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
}
