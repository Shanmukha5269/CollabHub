package com.collabHub.admin.dto;

import lombok.*;

/**
 * Admin Statistics DTO
 * Contains aggregate statistics about users
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatisticsDTO {

    private Long totalUsers;           // Total number of users (active + inactive)
    private Long activeUsers;          // Number of ACTIVE users
    private Long inactiveUsers;        // Number of INACTIVE users (soft deleted)
    private Long bannedUsers;          // Number of BANNED users
    private Long adminCount;           // Number of ADMIN users
    private Long regularUserCount;     // Number of regular USER users
    private Long newUsersThisMonth;    // Users registered this month
    private Long newUsersThisWeek;     // Users registered this week
    private Long usersActiveThisWeek;  // Users who logged in this week
    private Long usersActiveThisMonth; // Users who logged in this month


    public Double getActiveUserPercentage() {
        if (totalUsers == 0) return 0.0;
        return (activeUsers * 100.0) / totalUsers;
    }

    public Double getAdminPercentage() {
        if (totalUsers == 0) return 0.0;
        return (adminCount * 100.0) / totalUsers;
    }
}
