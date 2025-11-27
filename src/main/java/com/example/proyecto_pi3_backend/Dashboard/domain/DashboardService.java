package com.example.proyecto_pi3_backend.Dashboard.domain;

import com.example.proyecto_pi3_backend.Dashboard.dto.DashboardStatsDTO;
import com.example.proyecto_pi3_backend.MenuItems.infrastructure.MenuItemsRepository;
import com.example.proyecto_pi3_backend.Orders.domain.Orders;
import com.example.proyecto_pi3_backend.Orders.infrastructure.OrdersRepository;
import com.example.proyecto_pi3_backend.Feedback.infrastructure.FeedbackRepository;
import com.example.proyecto_pi3_backend.User.domain.Role;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import com.example.proyecto_pi3_backend.Vendors.infrastructure.VendorsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

/**
 * Servicio para obtener estadísticas del dashboard
 */
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final UserRepository userRepository;
    private final VendorsRepository vendorsRepository;
    private final MenuItemsRepository menuItemsRepository;
    private final OrdersRepository ordersRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * Obtiene las estadísticas generales del sistema
     */
    public DashboardStatsDTO getDashboardStats() {
        // Contar usuarios por rol
        long totalUsers = userRepository.count();
        long totalAdmins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();
        long totalVendors = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.VENDOR)
                .count();
        long totalRegularUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.USER)
                .count();

        // Contar vendors (entidades)
        long totalVendorsEntities = vendorsRepository.count();

        // Contar menús
        long totalMenuItems = menuItemsRepository.count();

        // Contar pedidos (excluyendo cancelados)
        List<Orders> allOrders = ordersRepository.findAll();
        long totalOrders = allOrders.stream()
                .filter(order -> !"CANCELADO".equals(order.getStatus()))
                .count();
        
        // Calcular pedidos de hoy (excluyendo cancelados)
        Calendar calToday = Calendar.getInstance();
        calToday.set(Calendar.HOUR_OF_DAY, 0);
        calToday.set(Calendar.MINUTE, 0);
        calToday.set(Calendar.SECOND, 0);
        calToday.set(Calendar.MILLISECOND, 0);
        Timestamp startOfToday = new Timestamp(calToday.getTimeInMillis());
        
        Calendar calTomorrow = Calendar.getInstance();
        calTomorrow.set(Calendar.HOUR_OF_DAY, 0);
        calTomorrow.set(Calendar.MINUTE, 0);
        calTomorrow.set(Calendar.SECOND, 0);
        calTomorrow.set(Calendar.MILLISECOND, 0);
        calTomorrow.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfTomorrow = new Timestamp(calTomorrow.getTimeInMillis());
        
        long totalOrdersToday = allOrders.stream()
                .filter(order -> !"CANCELADO".equals(order.getStatus()))
                .filter(order -> {
                    if (order.getCreatedAt() == null) return false;
                    Timestamp createdAt = order.getCreatedAt();
                    return createdAt.compareTo(startOfToday) >= 0 && createdAt.before(startOfTomorrow);
                })
                .count();
        
        // Calcular pedidos de esta semana (desde el lunes hasta hoy, excluyendo cancelados)
        Calendar calWeekStart = Calendar.getInstance();
        calWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        calWeekStart.set(Calendar.MINUTE, 0);
        calWeekStart.set(Calendar.SECOND, 0);
        calWeekStart.set(Calendar.MILLISECOND, 0);
        int dayOfWeek = calWeekStart.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        calWeekStart.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        Timestamp startOfWeek = new Timestamp(calWeekStart.getTimeInMillis());
        
        long totalOrdersThisWeek = allOrders.stream()
                .filter(order -> !"CANCELADO".equals(order.getStatus()))
                .filter(order -> {
                    if (order.getCreatedAt() == null) return false;
                    Timestamp createdAt = order.getCreatedAt();
                    return createdAt.compareTo(startOfWeek) >= 0 && createdAt.before(startOfTomorrow);
                })
                .count();

        // Contar feedback
        long totalFeedback = feedbackRepository.count();

        return new DashboardStatsDTO(
                totalUsers,
                totalAdmins,
                totalVendors,
                totalRegularUsers,
                totalVendorsEntities,
                totalMenuItems,
                totalOrders,
                totalOrdersToday,
                totalOrdersThisWeek,
                totalFeedback
        );
    }
}

