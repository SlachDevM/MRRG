package com.mrrg.backend.service;

import com.mrrg.backend.dto.CallbackFixRequest;
import com.mrrg.backend.dto.JobResponseDto;
import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.JobStatus;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.model.UserStatus;
import com.mrrg.backend.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
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

    @Mock
    private com.mrrg.backend.repository.UserRepository userRepository;

    @Mock
    private UserManagementService userManagementService;

    @InjectMocks
    private JobService jobService;

    @Test
    void create_shouldCreatePendingJob_whenJobHasNoDateAndUserIsManager() {
        Job job = sampleJob();

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userManagementService.computeStatus(any(User.class))).thenReturn(UserStatus.ACTIVE);

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
        assignWorkersToJob(job, 2L);

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            savedJob.setId(10L);
            return savedJob;
        });
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(userManagementService.computeStatus(worker)).thenReturn(UserStatus.ACTIVE);

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
        assignWorkersToJob(job, 2L);
        job.setCreatedBy(1L);
        job.setAfterPhotos(List.of("after-photo.jpg"));

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
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
        assignWorkersToJob(job, 3L);  // Worker ID 3

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
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
        assignWorkersToJob(job, 2L);  // Worker ID 2

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(userManagementService.computeStatus(worker)).thenReturn(UserStatus.ACTIVE);

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
    void archive_shouldPreserveAssignedWorkers_forHistoricalTraceability() {
        Job job = sampleJob();
        job.setStatus(JobStatus.DONE);
        assignWorkersToJob(job, 2L, 3L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.archive(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.ARCHIVED);
        assertThat(result.getAssignedWorkers()).isEqualTo("2,3");
    }

    @Test
    void markDone_shouldPreserveAssignedWorkers_forHistoricalTraceability() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.READY_FOR_CONFIRMATION);
        assignWorkersToJob(job, 2L, 3L);

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(userRepository.findById(3L)).thenReturn(Optional.of(workerWithId(3L)));
        when(userManagementService.computeStatus(any(User.class))).thenReturn(UserStatus.ACTIVE);

        Job result = jobService.markDone(10L, 1L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(result.getAssignedWorkers()).isEqualTo("2,3");
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
    void callbackFix_shouldClearAssignedWorkers_whenArchivedJobRestoredToToBeFixed() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.ARCHIVED);
        assignWorkersToJob(job, 2L, 3L);
        job.setDetails("Original details");
        job.setNotes("Original notes");
        job.setBeforePhotos(List.of("before.jpg"));
        job.setAfterPhotos(List.of("after.jpg"));

        CallbackFixRequest request = new CallbackFixRequest();

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.callbackFix(10L, request, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(JobStatus.TO_BE_FIXED);
        assertThat(result.getAssignedWorkers()).isNull();
        assertThat(result.getClientName()).isEqualTo("Test Client");
        assertThat(result.getDetails()).isEqualTo("Original details");
        assertThat(result.getNotes()).isEqualTo("Original notes");
        assertThat(result.getBeforePhotos()).containsExactly("before.jpg");
        assertThat(result.getAfterPhotos()).containsExactly("after.jpg");
        verify(jobRepository).save(job);
        verify(jobRepository, never()).delete(any(Job.class));
    }

    @Test
    void callbackFix_shouldClearAssignedWorkers_whenArchivedJobScheduledWithDate() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.ARCHIVED);
        assignWorkersToJob(job, 2L, 3L);

        CallbackFixRequest request = new CallbackFixRequest();
        request.setJobDate(LocalDate.of(2026, 3, 10));
        request.setJobStartHour("09:30");

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.callbackFix(10L, request, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(result.getAssignedWorkers()).isNull();
        verify(jobRepository).save(job);
    }

    @Test
    void callbackFix_shouldClearAssignedWorkers_whenDoneJobRestored() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.DONE);
        assignWorkersToJob(job, 2L);

        CallbackFixRequest request = new CallbackFixRequest();

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.callbackFix(10L, request, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(JobStatus.TO_BE_FIXED);
        assertThat(result.getAssignedWorkers()).isNull();
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
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();
        update.setBeforePhotos(List.of("before-photo.jpg"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(existingJob)).thenReturn(existingJob);

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getBeforePhotos()).containsExactly("before-photo.jpg");
    }

    @Test
    void update_shouldRejectWorkerUpdateWithoutPhotos() {
        Job existingJob = sampleJob();
        existingJob.setStatus(JobStatus.SCHEDULED);
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.update(10L, update, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void update_shouldPreserveDetailsAndNotes_whenManagerUpdatesArchivedJobWithPartialPayload() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.ARCHIVED);
        existingJob.setDetails("Original details");
        existingJob.setNotes("Original notes");
        existingJob.setBeforePhotos(List.of("before.jpg"));

        Job update = new Job();
        update.setBeforePhotos(List.of("before.jpg", "before-2.jpg"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 1L);

        assertThat(result.getDetails()).isEqualTo("Original details");
        assertThat(result.getNotes()).isEqualTo("Original notes");
        assertThat(result.getBeforePhotos()).containsExactly("before.jpg", "before-2.jpg");
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldPreserveDetailsAndNotes_whenManagerSendsNullTextFieldsOnArchivedJob() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.DONE);
        existingJob.setDetails("Original details");
        existingJob.setNotes("Original notes");

        Job update = new Job();
        update.setDetails(null);
        update.setNotes(null);
        update.setAfterPhotos(List.of("after.jpg"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 1L);

        assertThat(result.getDetails()).isEqualTo("Original details");
        assertThat(result.getNotes()).isEqualTo("Original notes");
        assertThat(result.getAfterPhotos()).containsExactly("after.jpg");
    }

    @Test
    void update_shouldUpdateDetailsAndNotes_whenManagerProvidesNonNullValuesOnArchivedJob() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.ARCHIVED);
        existingJob.setDetails("Original details");
        existingJob.setNotes("Original notes");

        Job update = new Job();
        update.setDetails("Updated details");
        update.setNotes("Updated notes");

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 1L);

        assertThat(result.getDetails()).isEqualTo("Updated details");
        assertThat(result.getNotes()).isEqualTo("Updated notes");
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
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(userManagementService.computeStatus(worker)).thenReturn(UserStatus.ACTIVE);

        Job result = jobService.assignWorkers(10L, "2", 1L);

        assertThat(result.getAssignedWorkers()).isEqualTo("2");

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
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();
        update.setBeforePhotos(List.of("base64EncodedPhoto1", "base64EncodedPhoto2"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getBeforePhotos()).hasSize(2);
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldNotTransitionToInProgress_whenAssignedWorkerUploadsAfterPhotosOnly() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();
        update.setAfterPhotos(List.of("base64EncodedPhoto1"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(result.getAfterPhotos()).hasSize(1);
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldNotTransitionToInProgress_whenAssignedWorkerAddsNotesOnly() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();
        update.setNotes("Job in progress, gutters cleaned");

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(result.getNotes()).isEqualTo("Job in progress, gutters cleaned");
        verify(jobRepository).save(existingJob);
    }

    @Test
    void update_shouldNotChangeStatus_whenAssignedWorkerAddsNotesToInProgressJob() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.IN_PROGRESS);
        assignWorkersToJob(existingJob, 2L);

        Job update = new Job();
        update.setNotes("Updated note");

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Job result = jobService.update(10L, update, 2L);

        assertThat(result.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getNotes()).isEqualTo("Updated note");
    }

    @Test
    void update_shouldTransitionToInProgress_whenAssignedWorkerAddsPhotosAndNotes() {
        Job existingJob = sampleJob();
        existingJob.setId(10L);
        existingJob.setStatus(JobStatus.SCHEDULED);
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();
        update.setBeforePhotos(List.of("base64Photo"));
        update.setNotes("Starting job now");

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
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
        assignWorkersToJob(existingJob, 2L);  // Worker ID 2

        Job update = new Job();
        update.setBeforePhotos(List.of("base64Photo"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(existingJob));
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
        assignWorkersToJob(job, 2L);  // Worker ID 2
        job.setCreatedBy(1L);
        job.setAfterPhotos(List.of("after-photo.jpg"));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
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
    void markReadyForConfirmation_shouldRejectCompletionWithoutAfterPhoto() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.IN_PROGRESS);
        assignWorkersToJob(job, 2L);
        job.setCreatedBy(1L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.markReadyForConfirmation(10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("At least one after photo is required");

        verify(jobRepository, never()).save(any(Job.class));
        verify(notificationService, never()).create(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void markReadyForConfirmation_shouldThrowError_whenJobNotInProgressOrScheduled() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.PENDING);
        assignWorkersToJob(job, 2L);  // Worker ID 2

        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.markReadyForConfirmation(10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void assignWorkers_shouldAssignMultipleWorkers() {
        Job job = sampleJob();
        job.setId(10L);

        User worker2 = workerWithId(2L);
        User worker3 = workerWithId(3L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(worker3));
        when(userManagementService.computeStatus(any(User.class))).thenReturn(UserStatus.ACTIVE);

        Job result = jobService.assignWorkers(10L, "2,3", 1L);

        assertThat(result.getAssignedWorkers()).isEqualTo("2,3");
        verify(notificationService).create(2L, 10L, NotificationType.JOB_ASSIGNED, "You have been assigned to job: Test Client");
        verify(notificationService).create(3L, 10L, NotificationType.JOB_ASSIGNED, "You have been assigned to job: Test Client");
    }

    @Test
    void assignWorkers_shouldRejectDuplicateWorkerIds() {
        User worker = workerWithId(2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
        when(userManagementService.computeStatus(worker)).thenReturn(UserStatus.ACTIVE);

        assertThatThrownBy(() -> jobService.assignWorkers(10L, "2,2", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Duplicate worker assignment");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void userNameChange_shouldNotBreakAssignment() {
        Job job = new Job();
        job.setId(10L);
        job.setClientName("Test Job");
        job.setStatus(JobStatus.SCHEDULED);
        assignWorkersToJob(job, 1L, 3L, 7L);

        User worker = workerWithId(3L);
        worker.setName("John Smith Jr.");

        assertThat(job.isWorkerAssigned(3L)).isTrue();
        assertThat(worker.getName()).isEqualTo("John Smith Jr.");
    }

    @Test
    void assignWorkers_shouldRejectDisabledUser() {
        User disabledWorker = new User("worker@test.com", "password", "Disabled Worker", UserRole.EMPLOYEE);
        disabledWorker.setId(2L);
        disabledWorker.setEnabled(false);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(disabledWorker));
        when(userManagementService.computeStatus(disabledWorker)).thenReturn(UserStatus.DISABLED);

        assertThatThrownBy(() -> jobService.assignWorkers(10L, "2", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Disabled users cannot be assigned");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void assignWorkers_shouldRejectPendingActivationUser() {
        User pendingWorker = new User("worker@test.com", "", "Pending Worker", UserRole.EMPLOYEE);
        pendingWorker.setId(2L);
        pendingWorker.setEnabled(false);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingWorker));
        when(userManagementService.computeStatus(pendingWorker)).thenReturn(UserStatus.PENDING_ACTIVATION);

        assertThatThrownBy(() -> jobService.assignWorkers(10L, "2", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pending activation users cannot be assigned");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void assignWorkers_shouldRejectNonExistingUser() {
        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.assignWorkers(10L, "99", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Worker not found");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void assignWorkers_shouldAllowEmptyWorkers() {
        Job job = sampleJob();
        job.setId(10L);
        job.setStatus(JobStatus.SCHEDULED);
        job.setJobDate(LocalDate.of(2026, 3, 10));
        assignWorkersToJob(job, 2L);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.assignWorkers(10L, "", 1L);

        assertThat(result.getAssignedWorkers()).isNull();
        verify(notificationService, never()).create(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void removeUserFromNonFinalJobs_shouldRemoveUserFromScheduledJob() {
        Job scheduledJob = sampleJob();
        scheduledJob.setId(10L);
        scheduledJob.setStatus(JobStatus.SCHEDULED);
        scheduledJob.setJobDate(LocalDate.of(2026, 3, 10));
        assignWorkersToJob(scheduledJob, 2L, 3L);

        when(jobRepository.findByStatusInOrderByPriorityLevelDesc(anyList()))
                .thenReturn(List.of(scheduledJob));
        when(userRepository.findById(3L)).thenReturn(Optional.of(workerWithId(3L)));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.removeUserFromNonFinalJobs(2L);

        assertThat(scheduledJob.getAssignedWorkers()).isEqualTo("3");
        assertThat(scheduledJob.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        assertThat(scheduledJob.getJobDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        verify(jobRepository).save(scheduledJob);
        verify(notificationService, never()).create(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void removeUserFromNonFinalJobs_shouldLeaveJobEmptyWhenOnlyWorker() {
        Job scheduledJob = sampleJob();
        scheduledJob.setId(10L);
        scheduledJob.setStatus(JobStatus.SCHEDULED);
        scheduledJob.setJobDate(LocalDate.of(2026, 3, 10));
        assignWorkersToJob(scheduledJob, 2L);

        when(jobRepository.findByStatusInOrderByPriorityLevelDesc(anyList()))
                .thenReturn(List.of(scheduledJob));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.removeUserFromNonFinalJobs(2L);

        assertThat(scheduledJob.getAssignedWorkers()).isNull();
        assertThat(scheduledJob.getStatus()).isEqualTo(JobStatus.SCHEDULED);
        verify(jobRepository).save(scheduledJob);
    }

    @Test
    void assignWorkers_shouldRejectNonWorkerRole() {
        User adminWorker = new User("admin@test.com", "password", "Admin User", UserRole.ADMIN);
        adminWorker.setId(5L);
        adminWorker.setEnabled(true);

        when(userService.isManagerOrAdmin(1L)).thenReturn(true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(adminWorker));
        when(userManagementService.computeStatus(adminWorker)).thenReturn(UserStatus.ACTIVE);

        assertThatThrownBy(() -> jobService.assignWorkers(10L, "5", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User cannot be assigned as a worker");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void removeUserFromNonFinalJobs_shouldNotModifyDoneJobs() {
        when(jobRepository.findByStatusInOrderByPriorityLevelDesc(anyList()))
                .thenReturn(List.of());

        jobService.removeUserFromNonFinalJobs(2L);

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void removeUserFromNonFinalJobs_shouldNotModifyArchivedJobs() {
        when(jobRepository.findByStatusInOrderByPriorityLevelDesc(anyList()))
                .thenReturn(List.of());

        jobService.removeUserFromNonFinalJobs(2L);

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void getById_shouldAllowManagerToRetrieveAnyJob() {
        Job job = sampleJob();
        job.setId(10L);
        assignWorkersToJob(job, 99L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.isManagerOrAdmin(1L)).thenReturn(true);

        Job result = jobService.getById(10L, 1L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getById_shouldAllowAdminToRetrieveAnyJob() {
        Job job = sampleJob();
        job.setId(10L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.isManagerOrAdmin(5L)).thenReturn(true);

        Job result = jobService.getById(10L, 5L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getById_shouldAllowAssignedEmployeeToRetrieveJob() {
        Job job = sampleJob();
        job.setId(10L);
        assignWorkersToJob(job, 2L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        Job result = jobService.getById(10L, 2L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getById_shouldReturnForbiddenForUnassignedEmployee() {
        Job job = sampleJob();
        job.setId(10L);
        assignWorkersToJob(job, 3L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userService.isManagerOrAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.getById(10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void getById_shouldReturnNotFoundWhenJobMissing() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getById(99L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void toJobResponse_shouldPopulateAssignedWorkerDetails() {
        Job job = sampleJob();
        job.setId(10L);
        assignWorkersToJob(job, 2L, 3L);

        User worker2 = new User("worker2@test.com", "password", "Alice Worker", UserRole.EMPLOYEE);
        worker2.setId(2L);
        User worker3 = new User("worker3@test.com", "password", "Bob Worker", UserRole.EMPLOYEE);
        worker3.setId(3L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(worker2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(worker3));

        JobResponseDto dto = jobService.toJobResponse(job);

        assertThat(dto.getAssignedWorkers()).isEqualTo("2,3");
        assertThat(dto.getAssignedWorkerDetails()).hasSize(2);
        assertThat(dto.getAssignedWorkerDetails().get(0).getId()).isEqualTo(2L);
        assertThat(dto.getAssignedWorkerDetails().get(0).getName()).isEqualTo("Alice Worker");
        assertThat(dto.getAssignedWorkerDetails().get(1).getId()).isEqualTo(3L);
        assertThat(dto.getAssignedWorkerDetails().get(1).getName()).isEqualTo("Bob Worker");
    }

    @Test
    void toJobResponse_shouldOmitMissingWorkersFromAssignedWorkerDetails() {
        Job job = sampleJob();
        job.setId(10L);
        assignWorkersToJob(job, 2L, 99L);

        User worker2 = new User("worker2@test.com", "password", "Alice Worker", UserRole.EMPLOYEE);
        worker2.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(worker2));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        JobResponseDto dto = jobService.toJobResponse(job);

        assertThat(dto.getAssignedWorkers()).isEqualTo("2,99");
        assertThat(dto.getAssignedWorkerDetails()).hasSize(1);
        assertThat(dto.getAssignedWorkerDetails().get(0).getId()).isEqualTo(2L);
    }

    private User workerWithId(long id) {
        User worker = new User("worker@test.com", "password", "Worker", UserRole.EMPLOYEE);
        worker.setId(id);
        return worker;
    }

    private void assignWorkersToJob(Job job, long... workerIds) {
        List<User> workers = Arrays.stream(workerIds)
                .mapToObj(this::workerWithId)
                .toList();
        job.replaceAssignments(workers);
    }
}