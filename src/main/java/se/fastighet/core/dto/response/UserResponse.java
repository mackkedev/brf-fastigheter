package se.fastighet.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.fastighet.core.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private String phone;
    private User.Role role;
    private List<UnitInfo> units;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitInfo {
        private UUID id;
        private String unitNumber;
        private String taxUnitNumber;
        private Integer rooms;
        private Double squareMeters;
        private Integer floor;
        private String address;
        private java.time.LocalDate acquisitionDate;
        private String ownershipShare;
        private java.math.BigDecimal monthlyFee;
        private java.math.BigDecimal internetFee;
        private String propertyName;
        private String propertyDesignation;
        private String organizationNumber;
        private Boolean economicPlanRegistered;
        private String contactEmail;
        private String contactPhone;
        private UUID propertyId;
    }
}
