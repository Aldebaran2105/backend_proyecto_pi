package com.example.proyecto_pi3_backend.MenuItems.domain;

import com.example.proyecto_pi3_backend.Availability.domain.Availability;
import com.example.proyecto_pi3_backend.Availability.infrastructure.AvailabilityRepository;
import com.example.proyecto_pi3_backend.MenuItems.dto.MenuItemsRequestDTO;
import com.example.proyecto_pi3_backend.MenuItems.dto.MenuItemsResponseDTO;
import com.example.proyecto_pi3_backend.MenuItems.infrastructure.MenuItemsRepository;
import com.example.proyecto_pi3_backend.Vendors.domain.Vendors;
import com.example.proyecto_pi3_backend.Vendors.infrastructure.VendorsRepository;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de items del menú
 * 
 * Funcionalidades para ADMIN:
 * - Crear menús para cada vendedor
 * - Actualizar menús
 * - Eliminar menús
 * 
 * Funcionalidades para USUARIO:
 * - Ver menús del día por vendedor
 * - Ver menús de la semana por vendedor
 * - Ver todos los menús disponibles
 */
@Service
@RequiredArgsConstructor
public class MenuItemsService {
    private final MenuItemsRepository menuItemsRepository;
    private final VendorsRepository vendorsRepository;
    private final AvailabilityRepository availabilityRepository;

    /**
     * ADMIN: Obtiene todos los menús (para gestión)
     */
    public List<MenuItemsResponseDTO> getAllMenuItems() {
        return getMenuItemsByDate(new Date());
    }

    /**
     * VENDOR: Obtiene todos los menús de un vendor con todas sus disponibilidades (incluyendo fechas pasadas)
     * Cada combinación de MenuItem + Availability se devuelve como un registro separado
     */
    public List<MenuItemsResponseDTO> getAllMenuItemsByVendor(Long vendorId) {
        validateVendorExists(vendorId);
        
        List<MenuItems> items = menuItemsRepository.findAll().stream()
                .filter(item -> item.getVendor().getId().equals(vendorId))
                .collect(Collectors.toList());
        
        List<MenuItemsResponseDTO> result = new java.util.ArrayList<>();
        
        for (MenuItems item : items) {
            List<Availability> availabilities = availabilityRepository.findAll().stream()
                    .filter(avail -> avail.getMenuItem().getId().equals(item.getId()))
                    .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate())) // Más recientes primero
                    .collect(Collectors.toList());
            
            if (availabilities.isEmpty()) {
                result.add(mapToDTOWithAvailability(item, null, new Date()));
            } else {
                for (Availability availability : availabilities) {
                    result.add(mapToDTOWithAvailability(item, availability, availability.getDate()));
                }
            }
        }
        
        return result;
    }

    /**
     * ADMIN: Crea un nuevo menú para un vendedor
     */
    @Transactional
    public MenuItemsResponseDTO createMenuItem(MenuItemsRequestDTO requestDTO) {
        validateMenuItemRequest(requestDTO);
        
        Vendors vendor = vendorsRepository.findById(requestDTO.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + requestDTO.getVendorId()));

        MenuItems menuItem = new MenuItems();
        menuItem.setItemName(requestDTO.getItemName().trim());
        menuItem.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null);
        menuItem.setPrice(requestDTO.getPrice().trim());
        menuItem.setVendor(vendor);

        menuItem = menuItemsRepository.save(menuItem);

        Date targetDate = requestDTO.getDate() != null && !requestDTO.getDate().trim().isEmpty() 
            ? parseDate(requestDTO.getDate()) 
            : new Date();
        
        validateDateNotPast(targetDate);
        
        createOrUpdateAvailability(menuItem, targetDate, requestDTO.getStock(), requestDTO.getIsAvailable());

        return mapToDTO(menuItem, targetDate);
    }

    /**
     * ADMIN: Actualiza un menú
     */
    @Transactional
    public MenuItemsResponseDTO updateMenuItem(Long id, MenuItemsRequestDTO requestDTO) {
        MenuItems menuItem = menuItemsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item no encontrado con ID: " + id));

        validateMenuItemRequest(requestDTO);

        menuItem.setItemName(requestDTO.getItemName().trim());
        menuItem.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null);
        menuItem.setPrice(requestDTO.getPrice().trim());

        if (requestDTO.getVendorId() != null && !requestDTO.getVendorId().equals(menuItem.getVendor().getId())) {
            Vendors vendor = vendorsRepository.findById(requestDTO.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + requestDTO.getVendorId()));
            menuItem.setVendor(vendor);
        }

        menuItem = menuItemsRepository.save(menuItem);

        if (requestDTO.getStock() != null || requestDTO.getIsAvailable() != null) {
            Date targetDate = requestDTO.getDate() != null && !requestDTO.getDate().trim().isEmpty() 
                ? parseDate(requestDTO.getDate()) 
                : new Date();
            validateDateNotPast(targetDate);
            createOrUpdateAvailability(menuItem, targetDate, requestDTO.getStock(), requestDTO.getIsAvailable());
            return mapToDTO(menuItem, targetDate);
        }

        return mapToDTO(menuItem, new Date());
    }

    /**
     * ADMIN: Elimina un menú
     */
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItems menuItem = menuItemsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item no encontrado con ID: " + id));
        menuItemsRepository.delete(menuItem);
    }

    /**
     * VENDOR: Elimina una disponibilidad específica (fecha) de un menú
     * Si es la última disponibilidad, elimina el menú completo
     */
    @Transactional
    public void deleteMenuItemAvailability(Long menuItemId, String dateString) {
        MenuItems menuItem = menuItemsRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item no encontrado con ID: " + menuItemId));
        
        Date targetDate = parseDate(dateString);
        
        List<Availability> availabilities = availabilityRepository.findAll().stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItemId) && isSameDay(avail.getDate(), targetDate))
                .collect(Collectors.toList());
        
        if (availabilities.isEmpty()) {
            throw new ResourceNotFoundException("No se encontró disponibilidad para la fecha especificada");
        }
        
        for (Availability availability : availabilities) {
            availabilityRepository.delete(availability);
        }
        
        List<Availability> remainingAvailabilities = availabilityRepository.findAll().stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItemId))
                .collect(Collectors.toList());
        
        if (remainingAvailabilities.isEmpty()) {
            menuItemsRepository.delete(menuItem);
        }
    }

    /**
     * USUARIO: Obtiene menús disponibles del día (hoy)
     */
    public List<MenuItemsResponseDTO> getAvailableMenuItemsToday() {
        return getMenuItemsByDate(new Date());
    }

    /**
     * USUARIO: Obtiene menús disponibles por vendor del día
     */
    public List<MenuItemsResponseDTO> getAvailableMenuItemsByVendorToday(Long vendorId) {
        validateVendorExists(vendorId);
        return getMenuItemsByVendorAndDate(vendorId, new Date());
    }

    /**
     * USUARIO: Obtiene menús disponibles por fecha
     */
    public List<MenuItemsResponseDTO> getAvailableMenuItemsByDate(String dateString) {
        Date targetDate = parseDate(dateString);
        return getMenuItemsByDate(targetDate);
    }

    /**
     * USUARIO: Obtiene menús disponibles por vendor y fecha
     */
    public List<MenuItemsResponseDTO> getAvailableMenuItemsByVendorAndDate(Long vendorId, String dateString) {
        validateVendorExists(vendorId);
        Date targetDate = parseDate(dateString);
        return getMenuItemsByVendorAndDate(vendorId, targetDate);
    }

    /**
     * USUARIO: Obtiene menús disponibles de la semana
     */
    public List<MenuItemsResponseDTO> getAvailableMenuItemsByWeek(String weekStartDateString) {
        Date weekStartDate = parseDate(weekStartDateString);
        Date weekEndDate = addDays(weekStartDate, 6);
        
        List<MenuItems> allItems = menuItemsRepository.findAll();
        
        return allItems.stream()
                .map(item -> {
                    Availability availability = findAvailabilityInRange(item.getId(), weekStartDate, weekEndDate);
                    // Usar la fecha de availability si existe, sino usar la fecha de inicio de semana
                    Date date = availability != null ? availability.getDate() : weekStartDate;
                    return mapToDTOWithAvailability(item, availability, date);
                })
                .filter(dto -> Boolean.TRUE.equals(dto.getIsAvailable()))
                .collect(Collectors.toList());
    }

    /**
     * USUARIO: Obtiene menús disponibles por vendor y semana
     */
    public List<MenuItemsResponseDTO> getAvailableMenuItemsByVendorAndWeek(Long vendorId, String weekStartDateString) {
        validateVendorExists(vendorId);
        Date weekStartDate = parseDate(weekStartDateString);
        Date weekEndDate = addDays(weekStartDate, 6);
        
        List<MenuItems> items = menuItemsRepository.findAll().stream()
                .filter(item -> item.getVendor().getId().equals(vendorId))
                .collect(Collectors.toList());
        
        return items.stream()
                .map(item -> {
                    Availability availability = findAvailabilityInRange(item.getId(), weekStartDate, weekEndDate);
                    // Usar la fecha de availability si existe, sino usar la fecha de inicio de semana
                    Date date = availability != null ? availability.getDate() : weekStartDate;
                    return mapToDTOWithAvailability(item, availability, date);
                })
                .filter(dto -> Boolean.TRUE.equals(dto.getIsAvailable()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un menú por ID
     */
    public MenuItemsResponseDTO getMenuItemById(Long id) {
        MenuItems item = menuItemsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item no encontrado con ID: " + id));
        
        return mapToDTO(item, new Date());
    }

    // ========== MÉTODOS PRIVADOS DE APOYO ==========

    private List<MenuItemsResponseDTO> getMenuItemsByDate(Date date) {
        List<MenuItems> allItems = menuItemsRepository.findAll();
        return allItems.stream()
                .map(item -> mapToDTO(item, date))
                .collect(Collectors.toList());
    }

    private List<MenuItemsResponseDTO> getMenuItemsByVendorAndDate(Long vendorId, Date date) {
        List<MenuItems> items = menuItemsRepository.findAll().stream()
                .filter(item -> item.getVendor().getId().equals(vendorId))
                .collect(Collectors.toList());
        
        return items.stream()
                .map(item -> mapToDTO(item, date))
                .collect(Collectors.toList());
    }

    private MenuItemsResponseDTO mapToDTO(MenuItems item, Date date) {
        Availability availability = findAvailabilityForDate(item.getId(), date);
        return mapToDTOWithAvailability(item, availability, date);
    }

    private MenuItemsResponseDTO mapToDTOWithAvailability(MenuItems item, Availability availability, Date date) {
        boolean isAvailable = availability != null && Boolean.TRUE.equals(availability.getIsAvailable());
        int stock = availability != null && availability.getStock() != null ? availability.getStock() : 0;
        // Usar la fecha de availability si existe, sino usar la fecha pasada como parámetro
        Date menuDate = availability != null ? availability.getDate() : date;
        
        return new MenuItemsResponseDTO(
                item.getId(),
                item.getItemName(),
                item.getDescription(),
                item.getPrice(),
                item.getVendor().getId(),
                item.getVendor().getName(),
                isAvailable,
                stock,
                menuDate
        );
    }

    private Availability findAvailabilityForDate(Long menuItemId, Date date) {
        MenuItems menuItem = menuItemsRepository.findById(menuItemId).orElse(null);
        if (menuItem == null) {
            return null;
        }
        
        return availabilityRepository.findAll().stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItemId) && isSameDay(avail.getDate(), date))
                .findFirst()
                .orElse(null);
    }

    private Availability findAvailabilityInRange(Long menuItemId, Date startDate, Date endDate) {
        List<Availability> availabilities = availabilityRepository.findAll().stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItemId) &&
                               !avail.getDate().before(startDate) &&
                               !avail.getDate().after(endDate) &&
                               Boolean.TRUE.equals(avail.getIsAvailable()))
                .collect(Collectors.toList());
        
        return availabilities.isEmpty() ? null : availabilities.get(0);
    }

    private void validateVendorExists(Long vendorId) {
        vendorsRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + vendorId));
    }

    private void validateMenuItemRequest(MenuItemsRequestDTO requestDTO) {
        if (requestDTO.getItemName() == null || requestDTO.getItemName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del item es requerido");
        }
        if (requestDTO.getPrice() == null || requestDTO.getPrice().trim().isEmpty()) {
            throw new RuntimeException("El precio es requerido");
        }
        if (requestDTO.getVendorId() == null) {
            throw new RuntimeException("El vendor es requerido");
        }
    }

    private void validateDateNotPast(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();
        
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        calDate.set(Calendar.HOUR_OF_DAY, 0);
        calDate.set(Calendar.MINUTE, 0);
        calDate.set(Calendar.SECOND, 0);
        calDate.set(Calendar.MILLISECOND, 0);
        Date normalizedDate = calDate.getTime();
        
        if (normalizedDate.before(today)) {
            throw new RuntimeException("No se pueden crear menús para fechas pasadas. Por favor selecciona una fecha de hoy en adelante.");
        }
    }

    private Date parseDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("Formato de fecha inválido. Use yyyy-MM-dd");
        }
    }

    /**
     * Crea o actualiza la disponibilidad de un menú para una fecha específica
     */
    private void createOrUpdateAvailability(MenuItems menuItem, Date date, Integer stock, Boolean isAvailable) {
        // Normalizar la fecha (solo día, sin hora)
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date normalizedDate = cal.getTime();
        
        // Buscar disponibilidad existente para esta fecha usando búsqueda manual
        // porque findByMenuItemAndDate puede no funcionar correctamente con comparación de fechas
        List<Availability> allAvailabilities = availabilityRepository.findAll();
        Optional<Availability> existingAvailability = allAvailabilities.stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItem.getId()) && 
                               isSameDay(avail.getDate(), normalizedDate))
                .findFirst();
        
        Availability availability;
        if (existingAvailability.isPresent()) {
            // Actualizar disponibilidad existente
            availability = existingAvailability.get();
        } else {
            // Crear nueva disponibilidad
            availability = new Availability();
            availability.setMenuItem(menuItem);
            availability.setDate(normalizedDate);
        }
        
        // Actualizar stock si se proporciona
        if (stock != null) {
            availability.setStock(stock);
        }
        
        // Actualizar disponibilidad si se proporciona
        if (isAvailable != null) {
            availability.setIsAvailable(isAvailable);
        }
        
        // Si no se proporciona stock ni disponibilidad y es nueva, establecer valores por defecto
        if (stock == null && isAvailable == null && !existingAvailability.isPresent()) {
            availability.setStock(0);
            availability.setIsAvailable(false);
        }
        
        availabilityRepository.save(availability);
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
