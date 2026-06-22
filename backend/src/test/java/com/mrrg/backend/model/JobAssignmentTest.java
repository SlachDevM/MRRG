package com.mrrg.backend.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobAssignmentTest {

    @Test
    void getUserId_shouldReturnUserId_whenUserReferenceSet() {
        User user = userWithId(5L);
        Job job = new Job();
        JobAssignment assignment = new JobAssignment(job, user);

        assertThat(assignment.getUserId()).isEqualTo(5L);
        assertThat(assignment.getUser()).isSameAs(user);
        assertThat(assignment.getJob()).isSameAs(job);
    }

    private User userWithId(long id) {
        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(id);
        return user;
    }
}
