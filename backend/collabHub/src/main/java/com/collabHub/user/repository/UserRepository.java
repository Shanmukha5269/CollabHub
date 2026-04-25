package com.collabHub.user.repository;

import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * Find all users that are not soft deleted
     * Used for admin to view all users
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllActiveUsers(Pageable pageable);

    /**
     * Find user by email that is not soft deleted
     */
    @Query("SELECT u FROM User u WHERE u.email = ?1 AND u.deletedAt IS NULL")
    Optional<User> findActiveUserByEmail(String email);

    /**
     * Count users by status (not soft deleted)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.deletedAt IS NULL")
    Long countByStatus(@Param("status") UserStatus status);

    /**
     * Count users by role (not soft deleted)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    Long countByRole(@Param("role") Role role);

    /**
     * Count users registered from a specific date onwards
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.createdAt >= :fromDate")
    Long countUsersCreatedAfter(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Count users registered in the current month
     */
    @Query(value = "SELECT COUNT(u.id) FROM users u WHERE u.deleted_at IS NULL AND EXTRACT(YEAR FROM u.created_at) = EXTRACT(YEAR FROM CURRENT_TIMESTAMP) AND EXTRACT(MONTH FROM u.created_at) = EXTRACT(MONTH FROM CURRENT_TIMESTAMP)", nativeQuery = true)
    Long countNewUsersThisMonth();    

    /**
     * Count users registered in the current week (last 7 days)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.createdAt >= :fromDate")
    Long countNewUsersThisWeek(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Count users active (logged in) from a specific date onwards
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.lastLoginAt IS NOT NULL AND u.lastLoginAt >= :fromDate")
    Long countUsersActiveAfter(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Count users active (logged in) in the current month
     */
    @Query(value = "SELECT COUNT(u.id) FROM users u WHERE u.deleted_at IS NULL AND u.last_login_at IS NOT NULL AND EXTRACT(YEAR FROM u.last_login_at) = EXTRACT(YEAR FROM CURRENT_TIMESTAMP) AND EXTRACT(MONTH FROM u.last_login_at) = EXTRACT(MONTH FROM CURRENT_TIMESTAMP)", nativeQuery = true)
    Long countUsersActiveThisMonth();   
}