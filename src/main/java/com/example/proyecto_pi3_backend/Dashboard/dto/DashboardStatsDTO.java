package com.example.proyecto_pi3_backend.Dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Long totalUsers;
    private Long totalAdmins;
    private Long totalVendors;
    private Long totalRegularUsers;
    private Long totalVendorsEntities;
    private Long totalMenuItems;
    private Long totalOrders;
    private Long totalOrdersToday;
    private Long totalOrdersThisWeek;
    private Long totalFeedback;
}

