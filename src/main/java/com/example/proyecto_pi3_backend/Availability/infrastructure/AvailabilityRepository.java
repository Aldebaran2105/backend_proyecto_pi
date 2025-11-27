package com.example.proyecto_pi3_backend.Availability.infrastructure;

import com.example.proyecto_pi3_backend.Availability.domain.Availability;
import com.example.proyecto_pi3_backend.MenuItems.domain.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    Optional<Availability> findByMenuItemAndDate(MenuItems menuItem, Date date);
}
