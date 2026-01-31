package se.fastighet.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "unit_number", nullable = false)
    private String unitNumber; // t.ex. "LGH 1201"

    @Column(name = "tax_unit_number")
    private String taxUnitNumber;

    @Column(name = "rooms")
    private Integer rooms;

    private Integer floor;

    @Column(name = "square_meters")
    private Double squareMeters;

    @Column(name = "address")
    private String address;

    @Column(name = "acquisition_date")
    private java.time.LocalDate acquisitionDate;

    @Column(name = "ownership_share")
    private String ownershipShare;

    @Column(name = "monthly_fee")
    private java.math.BigDecimal monthlyFee;

    @Column(name = "internet_fee")
    private java.math.BigDecimal internetFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToMany(mappedBy = "units")
    @Builder.Default
    private Set<User> residents = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
