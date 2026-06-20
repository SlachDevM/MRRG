package com.mrrg.backend.service;

import com.mrrg.backend.dto.CallbackFixRequest;
import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private JobService jobService;

    @Test
    void create_shouldCreatePendingJob_whenJobHasNoDateAndUserIsManager() {
        Job job = sampleJob();

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.create(job, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(result.getCreatedBy()).isEqualTo(1L);

        verify(jobRepository).save(job);
    }

    @Test
    void create_shouldCreateScheduledJobAndNotifyWorkers_whenJobHasDateAndWorkersAndUserIsManager() {
        Job job = sampleJob();
        job.setJobDate(LocalDate.of(2026, 3, 10));
        job.setJobStartHour("08:00");
        job.setAssignedWorkers("John Worker");

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            savedJob.setId(10L);
            return savedJob;
        });
        when(userService.findByName("John Worker")).thenReturn(List.of(worker));

        Job result = jobService.create(job, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(result.getJobDate()).isEqualTo(LocalDate.of(2026, 3, 10));

        verify(notificationService).create(
                2L,
                10L,
                NotificationType.JOB_ASSIGNED,
                "You have been assigned to job: Test Client"
        );
    }

    @Test
    void create_shouldThrowForbidden_whenUserIsEmployee() {
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.create(sampleJob(), 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void markReadyForConfirmation_shouldUpdateStatusAndNotifyManager_whenAssignedWorkerCompletesJob() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.SCHEDULED);
        job.setAssignedWorkers("John Worker");
        job.setCreatedBy(1L);

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.markReadyForConfirmation(10L, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.READY_FOR_CONFIRMATION);

        verify(notificationService).create(
                1L,
                10L,
                NotificationType.JOB_READY_FOR_CONFIRMATION,
                "Job Test Client is ready for confirmation"
        );
    }

    @Test
    void markReadyForConfirmation_shouldThrowForbidden_whenUserIsNotAssignedWorker() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.SCHEDULED);
        job.setAssignedWorkers("Another Worker");

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.markReadyForConfirmation(10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void markDone_shouldConfirmJobAndNotifyWorkers_whenUserIsManager() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.READY_FOR_CONFIRMATION);
        job.setAssignedWorkers("John Worker");

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(userService.findByName("John Worker")).thenReturn(List.of(worker));

        Job result = jobService.markDone(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.DONE);

        verify(notificationService).create(
                2L,
                10L,
                NotificationType.JOB_CONFIRMED,
                "Job Test Client has been confirmed"
        );
    }

    @Test
    void markDone_shouldThrowBadRequest_whenJobIsNotWaitingForConfirmation() {
        Job job = sampleJob();
        job.setStatus(JobStatus.SCHEDULED);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.markDone(10L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void archive_shouldArchiveJobAndClearSchedule_whenUserIsManager() {
        Job job = sampleJob();
        job.setStatus(JobStatus.DONE);
        job.setJobDate(LocalDate.of(2026, 3, 10));
        job.setJobStartHour("08:00");

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.archive(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.ARCHIVED);
        assertThat(result.getJobDate()).isNull();
        assertThat(result.getJobStartHour()).isNull();
    }

    @Test
    void archive_shouldThrowBadRequest_whenJobIsAlreadyArchived() {
        Job job = sampleJob();
        job.setStatus(JobStatus.ARCHIVED);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.archive(10L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void callbackFix_shouldMoveArchivedJobToToBeFixed_whenNoDateProvided() {
        Job job = sampleJob();
        job.setStatus(JobStatus.ARCHIVED);

        CallbackFixRequest request = new CallbackFixRequest();

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.callbackFix(10L, request, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.TO_BE_FIXED);
        assertThat(result.getJobDate()).isNull();
        assertThat(result.getJobStartHour()).isNull();
    }

    @Test
    void callbackFix_shouldScheduleArchivedJob_whenDateProvided() {
        Job job = sampleJob();
        job.setStatus(JobStatus.ARCHIVED);

        CallbackFixRequest request = new CallbackFixRequest();
        request.setJobDate(LocalDate.of(2026, 3, 10));
        request.setJobStartHour("09:30");

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.callbackFix(10L, request, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(result.getJobDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(result.getJobStartHour()).isEqualTo("09:30");
    }

    @Test
    void callbackFix_shouldUseDefaultStartHour_whenDateProvidedWithoutStartHour() {
        Job job = sampleJob();
        job.setStatus(JobStatus.DONE);

        CallbackFixRequest request = new CallbackFixRequest();
        request.setJobDate(LocalDate.of(2026, 3, 10));

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.callbackFix(10L, request, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(result.getJobStartHour()).isEqualTo("07:50");
    }

    @Test
    void update_shouldAllowAssignedWorkerToUploadPhotos() {
        Job existingJob = sampleJob();
        existingJob.setStatus(JobStatus.SCHEDULED);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();
        update.setBeforePhotos(List.of("before-photo.jpg"));

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(existingJob)).thenReturn(existingJob);

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getBeforePhotos()).containsExactly("before-photo.jpg");
    }

    @Test
    void update_shouldRejectWorkerUpdateWithoutPhotos() {
        Job existingJob = sampleJob();
        existingJob.setStatus(JobStatus.SCHEDULED);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.update(10L, update, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void assignWorkers_shouldSaveWorkersAndNotifyThem_whenUserIsManager() {
        Job job = sampleJob();
        job.setId(10L);

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(userService.findByName("John Worker")).thenReturn(List.of(worker));

        Job result = jobService.assignWorkers(10L, "John Worker", 1L);

        assertThat(result.getAssignedWorkers()).isEqualTo("John Worker");

        verify(notificationService).create(
                2L,
                10L,
                NotificationType.JOB_ASSIGNED,
                "You have been assigned to job: Test Client"
        );
    }

    private Job sampleJob() {
        Job job = new Job();
        job.setId(10L);
        job.setClientName("Test Client");
        job.setClientPhone("0400000000");
        job.setClientAddress("1 Test Street");
        job.setJobTypes("Gutter repair");
        job.setPriorityLevel(1);
        job.setStatus(JobStatus.PENDING);
        job.setCreatedBy(1L);
        return job;
    }

    // ==================== IN_PROGRESS Feature Tests ====================

    @Test
    void update_shouldTransitionToInProgress_whenAssignedWorkerUploadsBeforePhotos() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();
        update.setBeforePhotos(List.of("base64EncodedPhoto1", "base64EncodedPhoto2"));

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getBeforePhotos()).hasSize(2);
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldTransitionToInProgress_whenAssignedWorkerUploadsAfterPhotos() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();
        update.setAfterPhotos(List.of("base64EncodedPhoto1"));

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getAfterPhotos()).hasSize(1);
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldTransitionToInProgress_whenAssignedWorkerAddsNotes() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();
        update.setNotes("Job in progress, gutters cleaned");

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getNotes()).isEqualTo("Job in progress, gutters cleaned");
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldTransitionToInProgress_whenAssignedWorkerAddsPhotosAndNotes() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();
        update.setBeforePhotos(List.of("base64Photo"));
        update.setNotes("Starting job now");

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getBeforePhotos()).hasSize(1);
        assertThat(result.getNotes()).isEqualTo("Starting job now");
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldNotTransitionToInProgress_whenJobNotScheduled() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.PENDING);
        existingJob.setAssignedWorkers("John Worker");

        Job update = new Job();
        update.setBeforePhotos(List.of("base64Photo"));

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        // PENDING jobs don't transition to IN_PROGRESS
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        verify(jobRepository).save(existingJob);
    }

    @Test
    void markReadyForConfirmation_shouldAcceptInProgressJobs() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setAssignedWorkers("John Worker");
        job.setCreatedBy(1L);

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.getById(2L)).thenReturn(worker);
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.markReadyForConfirmation(10L, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.READY_FOR_CONFIRMATION);

        verify(notificationService).create(
                1L,
                10L,
                NotificationType.JOB_READY_FOR_CONFIRMATION,
                "Job Test Client is ready for confirmation"
        );
    }

    @Test
    void markReadyForConfirmation_shouldThrowError_whenJobNotInProgressOrScheduled() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.PENDING);
        job.setAssignedWorkers("John Worker");

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.markReadyForConfirmation(10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(jobRepository, never()).save(any(Job.class));
    }
}