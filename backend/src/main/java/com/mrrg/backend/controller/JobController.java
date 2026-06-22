package com.mrrg.backend.controller;

import com.mrrg.backend.dto.AssignWorkersRequest;
import com.mrrg.backend.dto.CallbackFixRequest;
import com.mrrg.backend.dto.JobResponseDto;
import com.mrrg.backend.model.Job;
import com.mrrg.backend.security.JwtAuthenticationToken;
import com.mrrg.backend.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<JobResponseDto>> listJobs(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponses(jobService.listJobs(userId)));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<JobResponseDto>> getPendingAndToBeFixed(Authentication authentication) {
        return ResponseEntity.ok(jobService.toJobResponses(jobService.getPendingAndToBeFixed()));
    }

    @GetMapping("/done")
    public ResponseEntity<List<JobResponseDto>> getDoneJobs(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponses(jobService.getDoneJobs(userId)));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<JobResponseDto>> getArchivedJobs(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponses(jobService.getArchivedJobs(userId)));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<JobResponseDto>> getScheduledJobs(
            @RequestParam("weekStart") String weekStart,
            @RequestParam("weekEnd") String weekEnd,
            Authentication authentication) {
        java.time.LocalDate start = java.time.LocalDate.parse(weekStart);
        java.time.LocalDate end = java.time.LocalDate.parse(weekEnd);
        return ResponseEntity.ok(jobService.toJobResponses(jobService.getScheduledJobs(start, end)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.getById(id, userId)));
    }

    @PostMapping
    public ResponseEntity<JobResponseDto> createJob(@RequestBody Job job, Authentication authentication) {
        Long userId = getUserId(authentication);
        Job savedJob = jobService.create(job, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.toJobResponse(savedJob));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDto> updateJob(
            @PathVariable("id") Long id,
            @RequestBody Job jobUpdate,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.update(id, jobUpdate, userId)));
    }

    @PutMapping("/{id}/assign-workers")
    public ResponseEntity<JobResponseDto> assignWorkers(
            @PathVariable("id") Long id,
            @RequestBody AssignWorkersRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.assignWorkers(id, request.getAssignedWorkers(), userId)));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<JobResponseDto> completeJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.markReadyForConfirmation(id, userId)));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<JobResponseDto> confirmJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.markDone(id, userId)));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<JobResponseDto> archiveJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.archive(id, userId)));
    }

    @PutMapping("/{id}/callback-fix")
    public ResponseEntity<JobResponseDto> callbackFix(
            @PathVariable("id") Long id,
            @RequestBody CallbackFixRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.toJobResponse(jobService.callbackFix(id, request, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        jobService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication authentication) {
        return ((JwtAuthenticationToken) authentication).getUserId();
    }
}
