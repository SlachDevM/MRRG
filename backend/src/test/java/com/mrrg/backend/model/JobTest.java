package com.mrrg.backend.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobTest {

    @Test
    void assignments_shouldUseLazyFetch() throws NoSuchFieldException {
        OneToMany mapping = Job.class.getDeclaredField("assignments").getAnnotation(OneToMany.class);

        assertThat(mapping.fetch()).isEqualTo(FetchType.LAZY);
    }

    @Test
    void getAssignedWorkerIds_shouldReturnSortedIdsFromAssignments() {
        Job job = new Job();
        job.replaceAssignments(List.of(userWithId(7L), userWithId(1L), userWithId(3L)));

        assertThat(job.getAssignedWorkerIds()).containsExactly(1L, 3L, 7L);
    }

    @Test
    void getAssignedWorkers_shouldReturnCommaSeparatedIds() {
        Job job = new Job();
        job.replaceAssignments(List.of(userWithId(1L), userWithId(3L), userWithId(7L)));

        assertThat(job.getAssignedWorkers()).isEqualTo("1,3,7");
    }

    @Test
    void getAssignedWorkers_shouldReturnNull_whenNoAssignments() {
        Job job = new Job();

        assertThat(job.getAssignedWorkers()).isNull();
    }

    @Test
    void isWorkerAssigned_shouldUseAssignmentIds() {
        Job job = new Job();
        job.replaceAssignments(List.of(userWithId(3L)));

        assertThat(job.isWorkerAssigned(3L)).isTrue();
        assertThat(job.isWorkerAssigned(2L)).isFalse();
    }

    @Test
    void clearAssignedWorkers_shouldRemoveAllAssignments() {
        Job job = new Job();
        job.replaceAssignments(List.of(userWithId(2L), userWithId(3L)));

        job.clearAssignedWorkers();

        assertThat(job.getAssignedWorkerIds()).isEmpty();
        assertThat(job.getAssignedWorkers()).isNull();
    }

    @Test
    void replaceAssignments_shouldPreventDuplicatesInEntityState() {
        Job job = new Job();
        User worker = userWithId(2L);

        job.replaceAssignments(List.of(worker, worker));

        assertThat(job.getAssignedWorkerIds()).containsExactly(2L);
    }

    @Test
    void setAssignedWorkers_shouldStoreInputForServiceConsumption() {
        Job job = new Job();

        job.setAssignedWorkers("1,3,7");

        assertThat(job.getAssignedWorkersInput()).isEqualTo("1,3,7");
        assertThat(job.getAssignedWorkerIds()).isEmpty();
    }

    private User userWithId(long id) {
        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(id);
        return user;
    }
}
