package se.fastighet.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.fastighet.core.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u JOIN u.units unit WHERE unit.property.id = :propertyId")
    List<User> findByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT u FROM User u JOIN u.units unit WHERE unit.property.id = :propertyId AND u.role = :role")
    List<User> findByPropertyIdAndRole(@Param("propertyId") UUID propertyId, @Param("role") User.Role role);

    @Query("SELECT u FROM User u JOIN u.adminProperties p WHERE p.id = :propertyId AND u.role = :role")
    List<User> findByAdminPropertyIdAndRole(@Param("propertyId") UUID propertyId, @Param("role") User.Role role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.units units LEFT JOIN FETCH units.property WHERE u.id = :id")
    Optional<User> findByIdWithUnits(@Param("id") UUID id);

    boolean existsByEmail(String email);
}
