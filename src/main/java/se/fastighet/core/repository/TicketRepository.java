package se.fastighet.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.fastighet.core.entity.Property;
import se.fastighet.core.entity.Ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // Hitta ärenden för en specifik rapportör
    Page<Ticket> findByReporterId(UUID reporterId, Pageable pageable);

    // Hitta ärenden tilldelade en specifik tekniker
    Page<Ticket> findByAssigneeId(UUID assigneeId, Pageable pageable);

    // Hitta ärenden för en fastighet
    Page<Ticket> findByPropertyId(UUID propertyId, Pageable pageable);

    // Hitta ärenden efter status
    Page<Ticket> findByStatus(Ticket.Status status, Pageable pageable);

    // Hitta ärenden för en fastighet med specifik status
    Page<Ticket> findByPropertyIdAndStatus(UUID propertyId, Ticket.Status status, Pageable pageable);

    // Hitta ej tilldelade ärenden
    @Query("SELECT t FROM Ticket t WHERE t.assignee IS NULL AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Ticket> findUnassignedOpenTickets();

    // Hitta gamla olösta ärenden (för eskalering)
    @Query("SELECT t FROM Ticket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') AND t.createdAt < :threshold")
    List<Ticket> findOldUnresolvedTickets(@Param("threshold") LocalDateTime threshold);

    // Statistik: Antal ärenden per status för en fastighet
    @Query("SELECT t.status, COUNT(t) FROM Ticket t WHERE t.property.id = :propertyId GROUP BY t.status")
    List<Object[]> countByStatusForProperty(@Param("propertyId") UUID propertyId);

    // Statistik: Antal ärenden per kategori för en fastighet
    @Query("SELECT t.category.name, COUNT(t) FROM Ticket t WHERE t.property.id = :propertyId AND t.category IS NOT NULL GROUP BY t.category.name")
    List<Object[]> countByCategoryForProperty(@Param("propertyId") UUID propertyId);

    // Sök i ärenden
    @Query("SELECT t FROM Ticket t WHERE t.property.id = :propertyId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Ticket> searchInProperty(@Param("propertyId") UUID propertyId,
                                   @Param("searchTerm") String searchTerm,
                                   Pageable pageable);

    @Query("SELECT DISTINCT t.property FROM Ticket t WHERE t.assignee.id = :assigneeId")
    List<Property> findDistinctPropertiesByAssigneeId(@Param("assigneeId") UUID assigneeId);
}
