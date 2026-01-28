package se.fastighet.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.fastighet.core.entity.Unit;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByPropertyId(UUID propertyId);

    boolean existsByPropertyIdAndUnitNumber(UUID propertyId, String unitNumber);
}
