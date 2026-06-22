package com.mrrg.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class JobRepositoryTest {

    @Test
    void readMethods_shouldFetchAssignmentsForDtoMapping() throws NoSuchMethodException {
        List<Method> methods = Arrays.asList(
                JobRepository.class.getMethod("findById", Long.class),
                JobRepository.class.getMethod("findAll"),
                JobRepository.class.getMethod("findByStatusOrderByPriorityLevelDesc", com.mrrg.backend.model.JobStatus.class),
                JobRepository.class.getMethod("findByStatusInOrderByPriorityLevelDesc", List.class),
                JobRepository.class.getMethod(
                        "findByStatusInAndJobDateBetweenOrderByJobStartHourAsc",
                        List.class,
                        java.time.LocalDate.class,
                        java.time.LocalDate.class
                )
        );

        for (Method method : methods) {
            EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);
            assertThat(entityGraph)
                    .as("Expected @EntityGraph on %s", method.getName())
                    .isNotNull();
            assertThat(entityGraph.attributePaths()).containsExactly("assignments", "assignments.user");
        }
    }

    @Test
    void findById_shouldReturnOptionalJob() throws NoSuchMethodException {
        assertThat(JobRepository.class.getMethod("findById", Long.class).getReturnType())
                .isEqualTo(Optional.class);
    }
}
