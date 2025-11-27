package com.example.proyecto_pi3_backend.Orders.domain;

import com.example.proyecto_pi3_backend.Orders.infrastructure.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSchedulerService {
    
    private final OrdersRepository ordersRepository;
    private final OrdersService ordersService;
    
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredPendingOrders() {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -5);
            Timestamp expirationTime = new Timestamp(cal.getTimeInMillis());
            
            List<Orders> expiredOrders = ordersRepository.findPendingOrdersOlderThan(expirationTime);
            
            if (expiredOrders.isEmpty()) {
                return;
            }
            
            log.info("Encontrados {} pedidos pendientes de pago expirados", expiredOrders.size());
            
            for (Orders order : expiredOrders) {
                try {
                    ordersService.cancelOrderAutomatically(order.getId());
                    log.info("Pedido {} cancelado automáticamente y stock devuelto", order.getId());
                } catch (Exception e) {
                    log.error("Error al cancelar pedido {} automáticamente", order.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error en el proceso de cancelación automática de pedidos", e);
        }
    }
}
