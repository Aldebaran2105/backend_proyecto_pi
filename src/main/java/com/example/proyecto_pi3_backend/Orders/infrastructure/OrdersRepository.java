package com.example.proyecto_pi3_backend.Orders.infrastructure;

import com.example.proyecto_pi3_backend.Orders.domain.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    @Query("SELECT o FROM Orders o WHERE o.vendor.id = :vendorId")
    List<Orders> findByVendorId(@Param("vendorId") Long vendorId);
    
    @Query("SELECT o FROM Orders o WHERE o.user.id = :userId")
    List<Orders> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT o FROM Orders o WHERE o.mercadoPagoPreferenceId = :preferenceId")
    java.util.Optional<Orders> findByMercadoPagoPreferenceId(@Param("preferenceId") String preferenceId);
    
    @Query("SELECT o FROM Orders o WHERE o.mercadoPagoPaymentId = :paymentId")
    java.util.Optional<Orders> findByMercadoPagoPaymentId(@Param("paymentId") String paymentId);
    
    @Query("SELECT o FROM Orders o WHERE o.status = 'PENDIENTE_PAGO' AND o.createdAt < :expirationTime")
    List<Orders> findPendingOrdersOlderThan(@Param("expirationTime") java.sql.Timestamp expirationTime);
}
