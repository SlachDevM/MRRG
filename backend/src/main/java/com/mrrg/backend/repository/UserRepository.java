package com.mrrg.backend.repository;

import com.mrrg.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mrrg.backend.model.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRoleInOrderByNameAsc(List<UserRole> roles);

    boolean existsByRole(UserRole role);

    List<User> findByName(String name);
}
