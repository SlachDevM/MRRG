package com.mrrg.backend.service;

import com.mrrg.backend.dto.CallbackFixRequest;
import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.model.UserStatus;
import com.mrrg.backend.repository.JobRepository;
import com.mrrg.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class JobService {

    private static final List<JobStatus> NON_FINAL_STATUSES = Arrays.asList(
            JobStatus.PENDING,
            JobStatus.SCHEDULED,
            JobStatus.IN_PROGRESS,
            JobStatus.READY_FOR_CONFIRMATION,
            JobStatus.TO_BE_FIXED
    );

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserManagementService userManagementService;
    private final NotificationService notificationService;

    public JobService(
            JobRepository jobRepository,
            UserRepository userRepository,
            UserService userService,
            UserManagementService userManagementService,
            NotificationService notificationService
    ) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.userManagementService = userManagementService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Job> listJobs(Long userId) {
        User user = userService.getById(userId);
        if (user.getRole() == UserRole.EMPLOYEE) {
            return jobRepository.findByStatusInOrderByPriorityLevelDesc(
                    Arrays.asList(JobStatus.PENDING, JobStatus.SCHEDULED, JobStatus.IN_PROGRESS, JobStatus.TO_BE_FIXED)
            );
        }
        return jobRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Job> getPendingAndToBeFixed() {
        return jobRepository.findByStatusInOrderByPriorityLevelDesc(
                Arrays.asList(JobStatus.PENDING, JobStatus.TO_BE_FIXED)
        );
    }

    @Transactional(readOnly = true)
    public List<Job> getDoneJobs(Long userId) {
        requireManagerOrAdmin(userId);
        return jobRepository.findByStatusOrderByPriorityLevelDesc(JobStatus.DONE);
    }

    @Transactional(readOnly = true)
    public List<Job> getArchivedJobs(Long userId) {
        requireManagerOrAdmin(userId);
        return jobRepository.findByStatusOrderByPriorityLevelDesc(JobStatus.ARCHIVED);
    }

    @Transactional(readOnly = true)
    public List<Job> getScheduledJobs(LocalDate weekStart, LocalDate weekEnd) {
        return jobRepository.findByStatusInAndJobDateBetweenOrderByJobStartHourAsc(
                Arrays.asList(JobStatus.SCHEDULED, JobStatus.IN_PROGRESS, JobStatus.READY_FOR_CONFIRMATION),
                weekStart,
                weekEnd
        );
    }

    @Transactional(readOnly = true)
    public Job getById(Long id) {
        return getJobOrThrow(id);
    }

    public Job create(Job job, Long createdBy) {
        requireManagerOrAdmin(createdBy);
        validateAssignableWorkers(job.getAssignedWorkers());

        boolean hasDate = job.getJobDate() != null;
        boolean hasWorkers = job.getAssignedWorkers() != null && !job.getAssignedWorkers().isBlank();
        
        if (hasDate) {
            job.setStatus(JobStatus.SCHEDULED);
        } else {
            job.setStatus(JobStatus.PENDING);
        }
        
        job.setCreatedBy(createdBy);
        Job savedJob = jobRepository.save(job);
        
        if (hasDate && hasWorkers) {
            notifyAssignedWorkers(savedJob, savedJob.getId(), job.getAssignedWorkers());
        }
        
        return savedJob;
    }

    public Job update(Long id, Job jobUpdate, Long userId) {
        Job job = getJobOrThrow(id);
        boolean isManager = userService.isManagerOrAdmin(userId);
        // For backward compatibility: check both ID and name format
        boolean isWorker = job.isWorkerAssigned(userId) || isAssignedWorkerByName(job, userId);

        if (!isManager && !isWorker) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (isManager) {
            if (job.getStatus() == JobStatus.ARCHIVED || job.getStatus() == JobStatus.DONE) {
                applyManagerSupplementaryUpdate(job, jobUpdate);
            } else {
                boolean wasPending = (job.getJobDate() == null);
                applyManagerJobUpdate(job, jobUpdate);
                boolean isNowScheduled = (job.getJobDate() != null);
                
                if (jobUpdate.getAssignedWorkers() != null && !jobUpdate.getAssignedWorkers().isEmpty()) {
                    notifyAssignedWorkers(job, id, jobUpdate.getAssignedWorkers());
                } else if (wasPending && isNowScheduled && job.getAssignedWorkers() != null && !job.getAssignedWorkers().isEmpty()) {
                    notifyAssignedWorkers(job, id, job.getAssignedWorkers());
                }
            }
        } else {
            boolean hasBeforePhotoUpdate = hasBeforePhotos(jobUpdate);
            boolean hasAfterPhotoUpdate = hasAfterPhotos(jobUpdate);
            boolean hasNotesUpdate = jobUpdate.getNotes() != null;
            if (!hasBeforePhotoUpdate && !hasAfterPhotoUpdate && !hasNotesUpdate) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No updates provided");
            }
            applyWorkerPhotoUpdate(job, jobUpdate);

            if (job.getStatus() == JobStatus.SCHEDULED && hasBeforePhotoUpdate) {
                job.setStatus(JobStatus.IN_PROGRESS);
            }
        }

        job.setUpdatedAt(System.currentTimeMillis());
        return jobRepository.save(job);
    }

    public Job assignWorkers(Long id, String assignedWorkers, Long userId) {
        requireManagerOrAdmin(userId);
        validateAssignableWorkers(assignedWorkers);
        Job job = getJobOrThrow(id);
        job.setAssignedWorkers(assignedWorkers);  // Format: "1,3,7"
        job.setUpdatedAt(System.currentTimeMillis());
        Job savedJob = jobRepository.save(job);
        notifyAssignedWorkers(job, id, assignedWorkers);
        return savedJob;
    }

    public Job markReadyForConfirmation(Long id, Long userId) {
        Job job = getJobOrThrow(id);
        if (job.getStatus() != JobStatus.SCHEDULED && job.getStatus() != JobStatus.IN_PROGRESS && job.getStatus() != JobStatus.TO_BE_FIXED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job cannot be completed in its current state");
        }

        boolean isManager = userService.isManagerOrAdmin(userId);
        // For backward compatibility: check both ID format and name format
        boolean isWorker = job.isWorkerAssigned(userId) || isAssignedWorkerByName(job, userId);
        
        if (!isManager && !isWorker) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (!hasAfterPhotos(job)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one after photo is required before completing the job."
            );
        }

        job.setStatus(JobStatus.READY_FOR_CONFIRMATION);
        job.setUpdatedAt(System.currentTimeMillis());
        Job savedJob = jobRepository.save(job);

        notificationService.create(
                job.getCreatedBy(),
                id,
                NotificationType.JOB_READY_FOR_CONFIRMATION,
                "Job " + job.getClientName() + " is ready for confirmation"
        );

        return savedJob;
    }

    /**
     * Backward compatibility: checks if user (by name) is assigned to job.
     * Used during migration from name-based to ID-based storage.
     * @deprecated Use job.isWorkerAssigned(userId) for new code
     */
    private boolean isAssignedWorkerByName(Job job, Long userId) {
        User user = userService.getById(userId);
        if (job.getAssignedWorkers() == null || user.getName() == null || user.getName().isBlank()) {
            return false;
        }
        for (String assigned : job.getAssignedWorkers().split(",")) {
            if (assigned.trim().equalsIgnoreCase(user.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    public Job markDone(Long id, Long userId) {
        requireManagerOrAdmin(userId);
        Job job = getJobOrThrow(id);
        if (job.getStatus() != JobStatus.READY_FOR_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is not awaiting confirmation");
        }

        job.setStatus(JobStatus.DONE);
        job.setUpdatedAt(System.currentTimeMillis());
        Job savedJob = jobRepository.save(job);

        notifyWorkers(
                job,
                id,
                NotificationType.JOB_CONFIRMED,
                "Job " + job.getClientName() + " has been confirmed"
        );

        return savedJob;
    }

    public Job archive(Long id, Long userId) {
        requireManagerOrAdmin(userId);
        Job job = getJobOrThrow(id);
        if (job.getStatus() == JobStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is already archived");
        }

        job.setStatus(JobStatus.ARCHIVED);
        job.setJobDate(null);
        job.setJobStartHour(null);
        job.setUpdatedAt(System.currentTimeMillis());
        return jobRepository.save(job);
    }

    public Job callbackFix(Long id, CallbackFixRequest request, Long userId) {
        requireManagerOrAdmin(userId);
        Job job = getJobOrThrow(id);
        if (job.getStatus() != JobStatus.ARCHIVED && job.getStatus() != JobStatus.DONE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only archived or done jobs can use Callback Fix"
            );
        }

        if (request.getJobDate() != null) {
            job.setJobDate(request.getJobDate());
            job.setJobStartHour(request.getJobStartHour() != null ? request.getJobStartHour() : "07:50");
            job.setStatus(JobStatus.SCHEDULED);
        } else {
            job.setJobDate(null);
            job.setJobStartHour(null);
            job.setStatus(JobStatus.TO_BE_FIXED);
        }

        job.setUpdatedAt(System.currentTimeMillis());
        return jobRepository.save(job);
    }

    public void delete(Long id, Long userId) {
        requireManagerOrAdmin(userId);
        Job job = getJobOrThrow(id);
        jobRepository.delete(job);
    }

    /**
     * Removes a user from assignedWorkers on all non-final jobs.
     * Does not change job status, schedule, or send notifications.
     */
    public void removeUserFromNonFinalJobs(Long userId) {
        List<Job> jobs = jobRepository.findByStatusInOrderByPriorityLevelDesc(NON_FINAL_STATUSES);
        for (Job job : jobs) {
            List<Long> workerIds = job.getAssignedWorkerIds();
            if (!workerIds.contains(userId)) {
                continue;
            }
            List<Long> remaining = workerIds.stream()
                    .filter(id -> !id.equals(userId))
                    .collect(Collectors.toList());
            job.setAssignedWorkerIds(remaining);
            job.setUpdatedAt(System.currentTimeMillis());
            jobRepository.save(job);
        }
    }

    private Job getJobOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireManagerOrAdmin(Long userId) {
        if (!userService.isManagerOrAdmin(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
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
            validateAssignableWorkers(jobUpdate.getAssignedWorkers());
            job.setAssignedWorkers(jobUpdate.getAssignedWorkers());
        }
        applyPhotoListUpdate(job, jobUpdate);
    }

    private void validateAssignableWorkers(String assignedWorkers) {
        if (assignedWorkers == null || assignedWorkers.isBlank()) {
            return;
        }

        for (String workerIdOrName : assignedWorkers.split(",")) {
            String value = workerIdOrName.trim();
            if (value.isEmpty()) {
                continue;
            }

            Long workerId;
            try {
                workerId = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid worker ID: " + value);
            }

            User worker = userRepository.findById(workerId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Worker not found: " + workerId));

            UserStatus status = userManagementService.computeStatus(worker);
            if (status == UserStatus.PENDING_ACTIVATION) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Pending activation users cannot be assigned to jobs.");
            }
            if (status == UserStatus.DISABLED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Disabled users cannot be assigned to jobs.");
            }

            if (worker.getRole() != UserRole.EMPLOYEE && worker.getRole() != UserRole.MANAGER) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "User cannot be assigned as a worker.");
            }
        }
    }

    private void applyWorkerPhotoUpdate(Job job, Job jobUpdate) {
        applyPhotoListUpdate(job, jobUpdate);

        if (jobUpdate.getNotes() != null) {
            job.setNotes(jobUpdate.getNotes());
        }
    }

    private boolean hasBeforePhotos(Job job) {
        return job.getBeforePhotos() != null && !job.getBeforePhotos().isEmpty();
    }

    private boolean hasAfterPhotos(Job job) {
        return job.getAfterPhotos() != null && !job.getAfterPhotos().isEmpty();
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

    private void notifyAssignedWorkers(Job job, Long jobId, String assignedWorkers) {
        notifyWorkers(
                job,
                jobId,
                NotificationType.JOB_ASSIGNED,
                "You have been assigned to job: " + job.getClientName(),
                assignedWorkers
        );
    }

    private void notifyWorkers(Job job, Long jobId, NotificationType type, String message) {
        notifyWorkers(job, jobId, type, message, job.getAssignedWorkers());
    }

    private void notifyWorkers(
            Job job,
            Long jobId,
            NotificationType type,
            String message,
            String assignedWorkers
    ) {
        if (assignedWorkers == null || assignedWorkers.isBlank()) {
            return;
        }

        // Parse comma-separated values (could be user IDs or names for backward compatibility)
        for (String workerIdOrName : assignedWorkers.split(",")) {
            String value = workerIdOrName.trim();
            if (value.isEmpty()) {
                continue;
            }
            
            // Try to parse as ID first
            try {
                Long workerId = Long.parseLong(value);
                Optional<User> worker = userRepository.findById(workerId);
                if (worker.isPresent()
                        && userManagementService.computeStatus(worker.get()) == UserStatus.ACTIVE) {
                    notificationService.create(worker.get().getId(), jobId, type, message);
                }
            } catch (NumberFormatException e) {
                // Backward compatibility: try to find by name (during migration)
                log.debug("Worker value is not an ID, skipping: {}", value);
                // Do not throw - just skip invalid entries during migration
            }
        }
    }
}
