package com.mrrg.backend.repository;

import com.mrrg.backend.model.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {
    Optional<AccountActivationToken> findByToken(String token);

    /**
     * Find all unused (valid for activation) tokens for a specific user.
     * A token is valid if it hasn't been used (usedAt is null).
     *
     * @param userId the user ID
     * @return list of unused tokens for the user
     */
    @Query("SELECT t FROM AccountActivationToken t WHERE t.user.id = :userId AND t.usedAt IS NULL")
    List<AccountActivationToken> findUnusedByUserId(@Param("userId") Long userId);

    /**
     * Check if a user has any valid (unused and not expired) activation tokens.
     *
     * @param userId the user ID
     * @return true if at least one valid token exists
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM AccountActivationToken t WHERE t.user.id = :userId AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP")
    boolean hasValidTokenByUserId(@Param("userId") Long userId);
}
