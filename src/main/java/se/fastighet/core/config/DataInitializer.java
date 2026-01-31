package se.fastighet.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.fastighet.core.entity.Agreement;
import se.fastighet.core.entity.Invoice;
import se.fastighet.core.entity.NewsPost;
import se.fastighet.core.entity.Property;
import se.fastighet.core.entity.Unit;
import se.fastighet.core.entity.User;
import se.fastighet.core.repository.NewsRepository;
import se.fastighet.core.repository.PropertyRepository;
import se.fastighet.core.repository.UnitRepository;
import se.fastighet.core.repository.UserRepository;
import se.fastighet.core.repository.AgreementRepository;
import se.fastighet.core.repository.InvoiceRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final NewsRepository newsRepository;
    private final AgreementRepository agreementRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = ensureUser("admin@test.se", "Test Admin", User.Role.ADMIN);
        User resident = ensureUser("user@test.se", "Test User", User.Role.RESIDENT);
        User admin2 = ensureUser("marcus.leo.karlsson@gmail.com", "Admin Test User", User.Role.RESIDENT);
        User technician = ensureUser("technician@test.com", "Test Technician", User.Role.TECHNICIAN);
        User boardMember = ensureUser("boardmember@test.com", "Test Board Member", User.Role.BOARD_MEMBER);

        Property propertyA = ensureProperty(
                "BRF Solsidan",
                "Storgatan 1",
                "Stockholm",
                "111 22",
                "Solsidan 1:12",
                "769600-1234",
                true,
                "styrelsen@solsidan.se",
                "08-123 45 67"
        );
        Property propertyB = ensureProperty(
                "BRF Ängen",
                "Parkvägen 12",
                "Stockholm",
                "111 23",
                "Ängen 2:8",
                "769600-5678",
                true,
                "styrelsen@angen.se",
                "08-987 65 43"
        );

        Unit unitA = ensureUnit(propertyA, "1101", "1201", 2, 55.0, 1,
                "Storgatan 1, 1101", java.time.LocalDate.of(2021, 3, 15),
                "1,25%", new java.math.BigDecimal("3200"), new java.math.BigDecimal("150"));
        Unit unitB = ensureUnit(propertyA, "1102", "1202", 3, 72.0, 2,
                "Storgatan 1, 1102", java.time.LocalDate.of(2019, 9, 1),
                "1,65%", new java.math.BigDecimal("3950"), new java.math.BigDecimal("150"));
        Unit unitC = ensureUnit(propertyB, "1201", "1301", 2, 58.0, 1,
                "Parkvägen 12, 1201", java.time.LocalDate.of(2020, 6, 10),
                "1,35%", new java.math.BigDecimal("3400"), new java.math.BigDecimal("150"));

        linkUserToUnit(resident, unitA);
        linkUserToUnit(boardMember, unitB);
        linkUserToUnit(admin2, unitA);
        linkAdminToProperty(admin, propertyA);
        linkAdminToProperty(admin2, propertyB);

        ensureNews(boardMember, "Välkommen till området", "Nya regler för soprummet gäller från nästa vecka.");
        ensureAgreements(resident);
        ensureInvoices(resident);
    }

    private User ensureUser(String email, String name, User.Role role) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder().email(email).build());

        user.setName(name);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode("password123"));

        User saved = userRepository.save(user);
        log.info("Ensured test user: {} ({})", email, role);
        return saved;
    }

    private Property ensureProperty(
            String name,
            String address,
            String city,
            String postalCode,
            String propertyDesignation,
            String organizationNumber,
            boolean economicPlanRegistered,
            String contactEmail,
            String contactPhone) {
        Property property = propertyRepository.findByName(name)
                .orElseGet(() -> Property.builder().name(name).build());

        property.setAddress(address);
        property.setCity(city);
        property.setPostalCode(postalCode);
        property.setPropertyDesignation(propertyDesignation);
        property.setOrganizationNumber(organizationNumber);
        property.setEconomicPlanRegistered(economicPlanRegistered);
        property.setContactEmail(contactEmail);
        property.setContactPhone(contactPhone);

        return propertyRepository.save(property);
    }

    private Unit ensureUnit(
            Property property,
            String unitNumber,
            String taxUnitNumber,
            Integer rooms,
            Double squareMeters,
            Integer floor,
            String address,
            java.time.LocalDate acquisitionDate,
            String ownershipShare,
            java.math.BigDecimal monthlyFee,
            java.math.BigDecimal internetFee) {
        return unitRepository.findByPropertyIdAndUnitNumber(property.getId(), unitNumber)
                .orElseGet(() -> unitRepository.save(Unit.builder()
                        .property(property)
                        .unitNumber(unitNumber)
                        .taxUnitNumber(taxUnitNumber)
                        .rooms(rooms)
                        .squareMeters(squareMeters)
                        .floor(floor)
                        .address(address)
                        .acquisitionDate(acquisitionDate)
                        .ownershipShare(ownershipShare)
                        .monthlyFee(monthlyFee)
                        .internetFee(internetFee)
                        .build()));
    }

    private void linkUserToUnit(User user, Unit unit) {
        if (!user.getUnits().contains(unit)) {
            user.getUnits().add(unit);
        }
        if (!unit.getResidents().contains(user)) {
            unit.getResidents().add(user);
        }
        userRepository.save(user);
    }

    private void linkAdminToProperty(User admin, Property property) {
        if (admin.getRole() != User.Role.ADMIN) {
            return;
        }
        if (!property.getAdmins().contains(admin)) {
            property.addAdmin(admin);
            propertyRepository.save(property);
        }
    }

    private void ensureNews(User author, String title, String body) {
        boolean exists = newsRepository.findAll().stream()
                .anyMatch(post -> post.getTitle().equalsIgnoreCase(title));
        if (!exists) {
            newsRepository.save(NewsPost.builder()
                    .title(title)
                    .body(body)
                    .author(author)
                    .build());
        }
    }

    private void ensureAgreements(User resident) {
        if (!agreementRepository.findByUserIdOrderByAgreementDateDesc(resident.getId()).isEmpty()) {
            return;
        }

        agreementRepository.saveAll(List.of(
                Agreement.builder()
                        .title("Hyresavtal")
                        .objectName("Lägenhet " + resident.getUnits().stream().findFirst().map(Unit::getUnitNumber).orElse("1101"))
                        .agreementType("Bostadsrätt")
                        .agreementDate(java.time.LocalDate.of(2021, 3, 15))
                        .user(resident)
                        .build(),
                Agreement.builder()
                        .title("Parkeringsplats")
                        .objectName("Garageplats 12")
                        .agreementType("Parkeringsavtal")
                        .agreementDate(java.time.LocalDate.of(2022, 5, 1))
                        .user(resident)
                        .build()
        ));
    }

    private void ensureInvoices(User resident) {
        if (!invoiceRepository.findByUserIdAndStatusOrderByDueDateDesc(resident.getId(), Invoice.InvoiceStatus.CURRENT).isEmpty()
                || !invoiceRepository.findByUserIdAndStatusOrderByDueDateDesc(resident.getId(), Invoice.InvoiceStatus.PAID).isEmpty()) {
            return;
        }

        invoiceRepository.saveAll(List.of(
                Invoice.builder()
                        .period("2024-11")
                        .paymentMethod("Autogiro")
                        .dueDate(java.time.LocalDate.of(2024, 11, 30))
                        .objectName("Månadsavgift")
                        .amount(new java.math.BigDecimal("3350"))
                        .status(Invoice.InvoiceStatus.CURRENT)
                        .user(resident)
                        .build(),
                Invoice.builder()
                        .period("2024-10")
                        .paymentMethod("Faktura")
                        .dueDate(java.time.LocalDate.of(2024, 10, 30))
                        .objectName("Månadsavgift")
                        .amount(new java.math.BigDecimal("3350"))
                        .paidDate(java.time.LocalDate.of(2024, 10, 28))
                        .status(Invoice.InvoiceStatus.PAID)
                        .user(resident)
                        .build()
        ));
    }
}
