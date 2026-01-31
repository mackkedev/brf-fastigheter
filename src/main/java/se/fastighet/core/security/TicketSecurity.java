package se.fastighet.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import se.fastighet.core.entity.Ticket;
import se.fastighet.core.entity.User;
import se.fastighet.core.repository.TicketRepository;

import java.util.UUID;

/**
 * Används för säkerhetskontroller på metodnivå med @PreAuthorize.
 * Exempel: @PreAuthorize("@ticketSecurity.canView(#ticketId)")
 */
@Component("ticketSecurity")
@RequiredArgsConstructor
public class TicketSecurity {

    private final TicketRepository ticketRepository;

    /**
     * Kontrollerar om användaren kan se ärendet.
     * - Admin och styrelsemedlemmar kan se alla ärenden i sina fastigheter
     * - Tekniker kan se ärenden tilldelade till dem
     * - Boende kan bara se sina egna ärenden
     */
    public boolean canView(Long ticketId) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;

        User user = principal.getUser();

        // Admin kan se ärenden för sina fastigheter
        if (user.getRole() == User.Role.ADMIN) {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) return false;
            return isAdminForProperty(user, ticket.getProperty().getId());
        }

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return false;

        return switch (user.getRole()) {
            case BOARD_MEMBER -> isUserInProperty(user, ticket.getProperty().getId());
            case TECHNICIAN -> ticket.getAssignee() != null &&
                    ticket.getAssignee().getId().equals(user.getId());
            case RESIDENT -> ticket.getReporter().getId().equals(user.getId());
            default -> false;
        };
    }

    /**
     * Kontrollerar om användaren kan uppdatera ärendet.
     */
    public boolean canUpdate(Long ticketId) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;

        User user = principal.getUser();

        if (user.getRole() == User.Role.ADMIN) {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) return false;
            return isAdminForProperty(user, ticket.getProperty().getId());
        }

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return false;

        return switch (user.getRole()) {
            case BOARD_MEMBER -> isUserInProperty(user, ticket.getProperty().getId());
            case TECHNICIAN -> ticket.getAssignee() != null &&
                    ticket.getAssignee().getId().equals(user.getId());
            case RESIDENT -> ticket.getReporter().getId().equals(user.getId()) &&
                    ticket.getStatus() == Ticket.Status.NEW; // Boende kan bara uppdatera nya ärenden
            default -> false;
        };
    }

    /**
     * Kontrollerar om användaren är tilldelad ärendet.
     */
    public boolean isAssignedTo(Long ticketId) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.getAssignee() == null) return false;

        return ticket.getAssignee().getId().equals(principal.getId());
    }

    /**
     * Kontrollerar om användaren kan kommentera på ärendet.
     */
    public boolean canComment(Long ticketId) {
        // Samma logik som canView för nu
        return canView(ticketId);
    }

    /**
     * Kontrollerar om användaren kan tilldela ärendet.
     */
    public boolean canAssign(Long ticketId) {
        UserPrincipal principal = getCurrentUser();
        if (principal == null) return false;

        User user = principal.getUser();

        if (user.getRole() == User.Role.ADMIN) {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) return false;
            return isAdminForProperty(user, ticket.getProperty().getId());
        }

        if (user.getRole() != User.Role.BOARD_MEMBER) {
            return false;
        }

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return false;

        return isUserInProperty(user, ticket.getProperty().getId());
    }

    private boolean isUserInProperty(User user, UUID propertyId) {
        return user.getUnits().stream()
                .anyMatch(unit -> unit.getProperty().getId().equals(propertyId));
    }

    private boolean isAdminForProperty(User user, UUID propertyId) {
        return user.getAdminProperties().stream()
                .anyMatch(property -> property.getId().equals(propertyId));
    }

    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }
}
