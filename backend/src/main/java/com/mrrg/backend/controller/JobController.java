package com.mrrg.backend.controller;

import com.mrrg.backend.dto.CallbackFixRequest;
import com.mrrg.backend.model.*;
import com.mrrg.backend.repository.JobRepository;
import com.mrrg.backend.repository.NotificationRepository;
import com.mrrg.backend.repository.UserRepository;
import com.mrrg.backend.security.JwtAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Job>> listJobs(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        Long userId = token.getUserId();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Job> jobs;
        if (user.getRole() == UserRole.EMPLOYEE) {
            jobs = jobRepository.findByStatusInOrderByPriorityLevelDesc(
                    Arrays.asList(JobStatus.PENDING, JobStatus.SCHEDULED, JobStatus.TO_BE_FIXED)
            );
        } else {
            jobs = jobRepository.findAll();
        }

        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Job>> getPendingAndToBeFixed(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        List<Job> jobs = jobRepository.findByStatusInOrderByPriorityLevelDesc(
                Arrays.asList(JobStatus.PENDING, JobStatus.TO_BE_FIXED)
        );
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/done")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Job>> getDoneJobs(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Job> jobs = jobRepository.findByStatusOrderByPriorityLevelDesc(JobStatus.DONE);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/archived")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Job>> getArchivedJobs(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Job> jobs = jobRepository.findByStatusOrderByPriorityLevelDesc(JobStatus.ARCHIVED);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/scheduled")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Job>> getScheduledJobs(
            @RequestParam("weekStart") Long weekStart,
            @RequestParam("weekEnd") Long weekEnd,
            Authentication authentication) {
        List<Job> jobs = jobRepository.findByStatusInAndJobDateBetweenOrderByJobStartHourAsc(
                Arrays.asList(JobStatus.SCHEDULED, JobStatus.READY_FOR_CONFIRMATION),
                weekStart,
                weekEnd);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Job> getJob(@PathVariable("id") Long id, Authentication authentication) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Job job, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        job.setStatus(JobStatus.PENDING);
        job.setCreatedBy(token.getUserId());
        Job savedJob = jobRepository.save(job);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateJob(@PathVariable("id") Long id, @RequestBody Job jobUpdate, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    boolean isManager = user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.ADMIN;
                    boolean isWorker = isAssignedWorker(job, user.getName());

                    if (!isManager && !isWorker) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }

                    if (isManager) {
                        if (job.getStatus() == JobStatus.ARCHIVED || job.getStatus() == JobStatus.DONE) {
                            applyManagerSupplementaryUpdate(job, jobUpdate);
                        } else {
                            applyManagerJobUpdate(job, jobUpdate);
                            if (jobUpdate.getAssignedWorkers() != null && !jobUpdate.getAssignedWorkers().isEmpty()) {
                                notifyAssignedWorkers(job, id, jobUpdate.getAssignedWorkers());
                            }
                        }
                    } else {
                        boolean hasPhotoUpdate =
                                (jobUpdate.getBeforePhotos() != null && !jobUpdate.getBeforePhotos().isEmpty())
                                        || (jobUpdate.getAfterPhotos() != null && !jobUpdate.getAfterPhotos().isEmpty());
                        if (!hasPhotoUpdate) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No photos provided");
                        }
                        applyWorkerPhotoUpdate(job, jobUpdate);
                    }

                    job.setUpdatedAt(System.currentTimeMillis());
                    Job savedJob = jobRepository.save(job);
                    return ResponseEntity.ok(savedJob);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void applyManagerJobUpdate(Job job, Job jobUpdate) {
        if (jobUpdate.getClientName() != null) {
            job.setClientName(jobUpdate.getClientName());
        }
        if (jobUpdate.getClientPhone() != null) {
            job.setClientPhone(jobUpdate.getClientPhone());
        }
        if (jobUpdate.getClientAddress() != null) {
            job.setClientAddress(jobUpdate.getClientAddress());
        }
        if (jobUpdate.getDetails() != null) {
            job.setDetails(jobUpdate.getDetails());
        }
        if (jobUpdate.getPriorityLevel() != null) {
            job.setPriorityLevel(jobUpdate.getPriorityLevel());
        }
        if (jobUpdate.getNotes() != null) {
            job.setNotes(jobUpdate.getNotes());
        }
        if (jobUpdate.getJobTypes() != null) {
            job.setJobTypes(jobUpdate.getJobTypes());
        }
        if (jobUpdate.getStatus() != null) {
            job.setStatus(jobUpdate.getStatus());
        }
        if (jobUpdate.getJobDate() != null) {
            job.setJobDate(jobUpdate.getJobDate());
            if (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.TO_BE_FIXED) {
                job.setStatus(JobStatus.SCHEDULED);
            }
        }
        if (jobUpdate.getJobStartHour() != null) {
            job.setJobStartHour(jobUpdate.getJobStartHour());
        } else if (job.getJobDate() != null && job.getJobStartHour() == null) {
            job.setJobStartHour("07:50");
        }
        if (jobUpdate.getAssignedWorkers() != null) {
            job.setAssignedWorkers(jobUpdate.getAssignedWorkers());
        }
        applyPhotoListUpdate(job, jobUpdate);
    }

    private void applyWorkerPhotoUpdate(Job job, Job jobUpdate) {
        applyPhotoListUpdate(job, jobUpdate);
    }

    private void applyPhotoListUpdate(Job job, Job jobUpdate) {
        if (jobUpdate.getBeforePhotos() != null) {
            job.setBeforePhotos(new ArrayList<>(jobUpdate.getBeforePhotos()));
        }
        if (jobUpdate.getAfterPhotos() != null) {
            job.setAfterPhotos(new ArrayList<>(jobUpdate.getAfterPhotos()));
        }
    }

    private void applyManagerSupplementaryUpdate(Job job, Job jobUpdate) {
        job.setDetails(jobUpdate.getDetails());
        job.setNotes(jobUpdate.getNotes());
        applyPhotoListUpdate(job, jobUpdate);
    }

    @PutMapping("/{id}/assign-workers")
    public ResponseEntity<?> assignWorkers(@PathVariable("id") Long id, @RequestBody Map map, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    String workers = (String) map.get("assignedWorkers");
                    job.setAssignedWorkers(workers);
                    job.setUpdatedAt(System.currentTimeMillis());
                    Job savedJob = jobRepository.save(job);

                    String message = "You have been assigned to job: " + job.getClientName();
                    notifyAssignedWorkers(job, id, workers);

                    return ResponseEntity.ok(savedJob);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void notifyAssignedWorkers(Job job, Long jobId, String assignedWorkers) {
        notifyWorkers(job, jobId, NotificationType.JOB_ASSIGNED,
                "You have been assigned to job: " + job.getClientName(), assignedWorkers);
    }

    private void notifyWorkers(Job job, Long jobId, NotificationType type, String message) {
        notifyWorkers(job, jobId, type, message, job.getAssignedWorkers());
    }

    private void notifyWorkers(Job job, Long jobId, NotificationType type, String message, String assignedWorkers) {
        if (assignedWorkers == null || assignedWorkers.isBlank()) {
            return;
        }

        for (String workerName : assignedWorkers.split(",")) {
            String name = workerName.trim();
            if (name.isEmpty()) {
                continue;
            }
            for (User worker : userRepository.findByName(name)) {
                notificationRepository.save(new Notification(worker.getId(), jobId, type, message));
            }
        }
    }

    private boolean isAssignedWorker(Job job, String workerName) {
        if (job.getAssignedWorkers() == null || workerName == null || workerName.isBlank()) {
            return false;
        }
        for (String assigned : job.getAssignedWorkers().split(",")) {
            if (assigned.trim().equalsIgnoreCase(workerName.trim())) {
                return true;
            }
        }
        return false;
    }

    @PutMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<?> completeJob(@PathVariable("id") Long id, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getStatus() != JobStatus.SCHEDULED && job.getStatus() != JobStatus.TO_BE_FIXED) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Job cannot be completed in its current state");
                    }

                    boolean isManager = user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.ADMIN;
                    if (!isManager && !isAssignedWorker(job, user.getName())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }

                    job.setStatus(JobStatus.READY_FOR_CONFIRMATION);
                    job.setUpdatedAt(System.currentTimeMillis());
                    Job savedJob = jobRepository.save(job);

                    notificationRepository.save(new Notification(
                            job.getCreatedBy(),
                            id,
                            NotificationType.JOB_READY_FOR_CONFIRMATION,
                            "Job " + job.getClientName() + " is ready for confirmation"
                    ));

                    return ResponseEntity.ok(savedJob);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/confirm")
    @Transactional
    public ResponseEntity<?> confirmJob(@PathVariable("id") Long id, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getStatus() != JobStatus.READY_FOR_CONFIRMATION) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Job is not awaiting confirmation");
                    }

                    job.setStatus(JobStatus.DONE);
                    job.setUpdatedAt(System.currentTimeMillis());
                    Job savedJob = jobRepository.save(job);

                    notifyWorkers(job, id, NotificationType.JOB_CONFIRMED,
                            "Job " + job.getClientName() + " has been confirmed");

                    return ResponseEntity.ok(savedJob);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/archive")
    @Transactional
    public ResponseEntity<?> archiveJob(@PathVariable("id") Long id, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getStatus() == JobStatus.ARCHIVED) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job is already archived");
                    }

                    job.setStatus(JobStatus.ARCHIVED);
                    job.setJobDate(null);
                    job.setJobStartHour(null);
                    job.setUpdatedAt(System.currentTimeMillis());
                    Job savedJob = jobRepository.save(job);
                    return ResponseEntity.ok(savedJob);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/callback-fix")
    @Transactional
    public ResponseEntity<?> callbackFix(
            @PathVariable("id") Long id,
            @RequestBody CallbackFixRequest request,
            Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getStatus() != JobStatus.ARCHIVED && job.getStatus() != JobStatus.DONE) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Only archived or done jobs can use Callback Fix");
                    }

                    if (request.getJobDate() != null) {
                        job.setJobDate(request.getJobDate());
                        job.setJobStartHour(
                                request.getJobStartHour() != null ? request.getJobStartHour() : "07:50");
                        job.setStatus(JobStatus.SCHEDULED);
                    } else {
                        job.setJobDate(null);
                        job.setJobStartHour(null);
                        job.setStatus(JobStatus.TO_BE_FIXED);
                    }

                    job.setUpdatedAt(System.currentTimeMillis());
                    return ResponseEntity.ok(jobRepository.save(job));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable("id") Long id, Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return jobRepository.findById(id)
                .map(job -> {
                    jobRepository.delete(job);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

class Map {
    private String assignedWorkers;

    public String getAssignedWorkers() {
        return assignedWorkers;
    }

    public void setAssignedWorkers(String assignedWorkers) {
        this.assignedWorkers = assignedWorkers;
    }

    public Object get(String key) {
        if ("assignedWorkers".equals(key)) {
            return assignedWorkers;
        }
        return null;
    }
}
