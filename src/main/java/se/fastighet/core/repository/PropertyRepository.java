package se.fastighet.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.fastighet.core.entity.Property;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByName(String name);

    boolean existsByName(String name);
}
