package com.mrrg.backend.controller;

import com.mrrg.backend.dto.AssignWorkersRequest;
import com.mrrg.backend.dto.CallbackFixRequest;
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
    public ResponseEntity<List<Job>> listJobs(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.listJobs(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Job>> getPendingAndToBeFixed(Authentication authentication) {
        return ResponseEntity.ok(jobService.getPendingAndToBeFixed());
    }

    @GetMapping("/done")
    public ResponseEntity<List<Job>> getDoneJobs(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.getDoneJobs(userId));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<Job>> getArchivedJobs(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.getArchivedJobs(userId));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<Job>> getScheduledJobs(
            @RequestParam("weekStart") String weekStart,
            @RequestParam("weekEnd") String weekEnd,
            Authentication authentication) {
        java.time.LocalDate start = java.time.LocalDate.parse(weekStart);
        java.time.LocalDate end = java.time.LocalDate.parse(weekEnd);
        return ResponseEntity.ok(jobService.getScheduledJobs(start, end));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable("id") Long id, Authentication authentication) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job, Authentication authentication) {
        Long userId = getUserId(authentication);
        Job savedJob = jobService.create(job, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(
            @PathVariable("id") Long id,
            @RequestBody Job jobUpdate,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.update(id, jobUpdate, userId));
    }

    @PutMapping("/{id}/assign-workers")
    public ResponseEntity<Job> assignWorkers(
            @PathVariable("id") Long id,
            @RequestBody AssignWorkersRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.assignWorkers(id, request.getAssignedWorkers(), userId));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Job> completeJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.markReadyForConfirmation(id, userId));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Job> confirmJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.markDone(id, userId));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Job> archiveJob(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.archive(id, userId));
    }

    @PutMapping("/{id}/callback-fix")
    public ResponseEntity<Job> callbackFix(
            @PathVariable("id") Long id,
            @RequestBody CallbackFixRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(jobService.callbackFix(id, request, userId));
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
