package com.example.proyecto_pi3_backend.User.infrastructure;

import com.example.proyecto_pi3_backend.User.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para acceso a datos de usuarios
 * Solo queries básicas, sin lógica de negocio
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    /**
     * Busca un usuario por email
     * @param email Email del usuario
     * @return Usuario encontrado o empty
     */
    Optional<Users> findByEmail(String email);
    
    /**
     * Obtiene todos los usuarios usando query nativa
     * Esto evita problemas con roles inválidos en la BD
     * @return Lista de arrays de objetos con los datos de usuarios
     */
    @Query(value = "SELECT u.id, u.first_name, u.last_name, u.email, u.role, u.vendor_id FROM users_table u ORDER BY u.id", nativeQuery = true)
    List<Object[]> findAllUsersRaw();
    
    /**
     * Actualiza el rol de un usuario usando query nativa
     * Intenta actualizar directamente (funciona si la columna es VARCHAR)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users_table SET role = :role WHERE id = :userId", nativeQuery = true)
    void updateUserRoleNative(@Param("userId") Long userId, @Param("role") String role);
    
    /**
     * Actualiza el rol convirtiendo el string a número (para columnas smallint)
     * Mapeo: USER=0, ADMIN=1, VENDOR=2
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users_table SET role = CASE WHEN :role = 'USER' THEN 0 WHEN :role = 'ADMIN' THEN 1 WHEN :role = 'VENDOR' THEN 2 ELSE 0 END WHERE id = :userId", nativeQuery = true)
    void updateUserRoleNativeAsInt(@Param("userId") Long userId, @Param("role") String role);
    
    /**
     * Actualiza el vendor de un usuario usando query nativa
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users_table SET vendor_id = :vendorId WHERE id = :userId", nativeQuery = true)
    void updateUserVendorNative(@Param("userId") Long userId, @Param("vendorId") Long vendorId);
    
    /**
     * Limpia el vendor de un usuario usando query nativa
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users_table SET vendor_id = NULL WHERE id = :userId", nativeQuery = true)
    void clearUserVendorNative(@Param("userId") Long userId);
    
    /**
     * Inserta un usuario usando query nativa (para evitar problemas con tipos de columna)
     * Si la columna es smallint, convierte el string a número; si es VARCHAR, lo inserta directamente
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO users_table (first_name, last_name, email, password, role) VALUES (:firstName, :lastName, :email, :password, :role)", nativeQuery = true)
    void insertUserWithRoleNative(
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("email") String email,
        @Param("password") String password,
        @Param("role") String role
    );
}
