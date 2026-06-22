package com.mrrg.backend.repository;

import com.mrrg.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);
}
