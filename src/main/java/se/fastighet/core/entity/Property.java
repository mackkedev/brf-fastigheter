package se.fastighet.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // t.ex. "BRF Solsidan"

    private String address;

    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "property_designation")
    private String propertyDesignation;

    @Column(name = "organization_number")
    private String organizationNumber;

    @Column(name = "economic_plan_registered")
    private Boolean economicPlanRegistered;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Unit> units = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "property_admins",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private java.util.Set<User> admins = new java.util.HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addUnit(Unit unit) {
        units.add(unit);
        unit.setProperty(this);
    }

    public void removeUnit(Unit unit) {
        units.remove(unit);
        unit.setProperty(null);
    }

    public void addAdmin(User user) {
        admins.add(user);
        user.getAdminProperties().add(this);
    }

    public void removeAdmin(User user) {
        admins.remove(user);
        user.getAdminProperties().remove(this);
    }
}
