package se.fastighet.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.fastighet.core.dto.request.AddCommentRequest;
import se.fastighet.core.dto.request.CreateTicketRequest;
import se.fastighet.core.dto.request.UpdateTicketRequest;
import se.fastighet.core.dto.response.TicketListResponse;
import se.fastighet.core.dto.response.TicketResponse;
import se.fastighet.core.entity.*;
import se.fastighet.core.event.TicketEventPublisher;
import se.fastighet.core.exception.ResourceNotFoundException;
import se.fastighet.core.exception.UnauthorizedException;
import se.fastighet.core.repository.*;
import se.fastighet.core.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final TicketEventPublisher eventPublisher;

    public TicketResponse createTicket(CreateTicketRequest request, UserPrincipal principal) {
        User reporter = principal.getUser();

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Fastighet hittades inte"));

        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .property(property)
                .reporter(reporter)
                .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte"));
            ticket.setCategory(category);
        }

        if (request.getUnitId() != null) {
            Unit unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lägenhet hittades inte"));
            ticket.setUnit(unit);
        }

        // Lägg till historik
        TicketHistory history = TicketHistory.builder()
                .changeType(TicketHistory.ChangeType.CREATED)
                .newValue(Ticket.Status.NEW.name())
                .changedBy(reporter)
                .description("Ärende skapat")
                .build();
        ticket.addHistoryEntry(history);

        ticket = ticketRepository.save(ticket);

        // Publicera event
        eventPublisher.publishTicketCreated(ticket);

        log.info("Ticket created: {} by user {}", ticket.getId(), reporter.getEmail());
        return mapToResponse(ticket, principal);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ärende hittades inte"));

        return mapToResponse(ticket, principal);
    }

    @Transactional(readOnly = true)
    public Page<TicketListResponse> getTicketsForProperty(UUID propertyId, Pageable pageable) {
        return ticketRepository.findByPropertyId(propertyId, pageable)
                .map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketListResponse> getMyTickets(UserPrincipal principal, Pageable pageable) {
        return ticketRepository.findByReporterId(principal.getId(), pageable)
                .map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketListResponse> getAssignedTickets(UserPrincipal principal, Pageable pageable) {
        return ticketRepository.findByAssigneeId(principal.getId(), pageable)
                .map(this::mapToListResponse);
    }

    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ärende hittades inte"));

        User user = principal.getUser();
        String oldStatus = ticket.getStatus().name();
        boolean statusChanged = false;

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte"));
            ticket.setCategory(category);
        }

        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
            addHistory(ticket, user, TicketHistory.ChangeType.PRIORITY_CHANGED,
                    ticket.getPriority().name(), request.getPriority().name());
        }

        if (request.getStatus() != null && request.getStatus() != ticket.getStatus()) {
            statusChanged = true;
            addHistory(ticket, user, TicketHistory.ChangeType.STATUS_CHANGED,
                    ticket.getStatus().name(), request.getStatus().name());
            ticket.setStatus(request.getStatus());

            if (request.getStatus() == Ticket.Status.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        ticket = ticketRepository.save(ticket);

        if (statusChanged) {
            eventPublisher.publishStatusChanged(ticket, oldStatus, user);
        }

        log.info("Ticket updated: {} by user {}", ticketId, principal.getEmail());
        return mapToResponse(ticket, principal);
    }

    public TicketResponse assignTicket(Long ticketId, UUID assigneeId, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ärende hittades inte"));

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte"));

        User assigner = principal.getUser();
        UUID propertyId = ticket.getProperty().getId();

        if (assigner.getRole() == User.Role.BOARD_MEMBER) {
            if (assignee.getRole() != User.Role.ADMIN || !isAdminForProperty(assignee, propertyId)) {
                throw new UnauthorizedException("Endast fastighetens förvaltare kan tilldelas ärenden");
            }
        } else if (assigner.getRole() == User.Role.ADMIN) {
            if (!isAdminForProperty(assigner, propertyId)) {
                throw new UnauthorizedException("Du har inte behörighet för denna fastighet");
            }
            if (assignee.getRole() != User.Role.TECHNICIAN) {
                throw new UnauthorizedException("Endast tekniker kan tilldelas ärenden");
            }
        } else {
            throw new UnauthorizedException("Du har inte behörighet att tilldela ärenden");
        }

        ticket.setAssignee(assignee);

        if (ticket.getStatus() == Ticket.Status.NEW) {
            ticket.setStatus(Ticket.Status.IN_PROGRESS);
            addHistory(ticket, principal.getUser(), TicketHistory.ChangeType.STATUS_CHANGED,
                    Ticket.Status.NEW.name(), Ticket.Status.IN_PROGRESS.name());
        }

        addHistory(ticket, principal.getUser(), TicketHistory.ChangeType.ASSIGNED,
                null, assignee.getName());

        ticket = ticketRepository.save(ticket);

        eventPublisher.publishTicketAssigned(ticket, principal.getUser());

        log.info("Ticket {} assigned to {} by {}", ticketId, assignee.getEmail(), principal.getEmail());
        return mapToResponse(ticket, principal);
    }

    private boolean isAdminForProperty(User user, UUID propertyId) {
        return user.getAdminProperties().stream()
                .anyMatch(property -> property.getId().equals(propertyId));
    }

    public TicketResponse addComment(Long ticketId, AddCommentRequest request, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ärende hittades inte"));

        // Endast styrelse/admin/tekniker kan skapa interna kommentarer
        if (request.isInternal() && principal.isResident()) {
            throw new UnauthorizedException("Endast styrelse och tekniker kan skapa interna kommentarer");
        }

        TicketComment comment = TicketComment.builder()
                .content(request.getContent())
                .author(principal.getUser())
                .internal(request.isInternal())
                .build();

        ticket.addComment(comment);

        addHistory(ticket, principal.getUser(), TicketHistory.ChangeType.COMMENT_ADDED,
                null, "Kommentar tillagd");

        ticket = ticketRepository.save(ticket);

        eventPublisher.publishCommentAdded(ticket, comment);

        log.info("Comment added to ticket {} by {}", ticketId, principal.getEmail());
        return mapToResponse(ticket, principal);
    }

    private void addHistory(Ticket ticket, User changedBy, TicketHistory.ChangeType changeType,
                            String oldValue, String newValue) {
        TicketHistory history = TicketHistory.builder()
                .changeType(changeType)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .build();
        ticket.addHistoryEntry(history);
    }

    private TicketResponse mapToResponse(Ticket ticket, UserPrincipal principal) {
        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt());

        // Reporter
        builder.reporter(TicketResponse.UserSummary.builder()
                .id(ticket.getReporter().getId())
                .name(ticket.getReporter().getName())
                .email(ticket.getReporter().getEmail())
                .build());

        // Assignee
        if (ticket.getAssignee() != null) {
            builder.assignee(TicketResponse.UserSummary.builder()
                    .id(ticket.getAssignee().getId())
                    .name(ticket.getAssignee().getName())
                    .email(ticket.getAssignee().getEmail())
                    .build());
        }

        // Category
        if (ticket.getCategory() != null) {
            builder.category(TicketResponse.CategoryResponse.builder()
                    .id(ticket.getCategory().getId())
                    .name(ticket.getCategory().getName())
                    .icon(ticket.getCategory().getIcon())
                    .build());
        }

        // Property
        builder.property(TicketResponse.PropertySummary.builder()
                .id(ticket.getProperty().getId())
                .name(ticket.getProperty().getName())
                .address(ticket.getProperty().getAddress())
                .build());

        // Unit
        if (ticket.getUnit() != null) {
            builder.unit(TicketResponse.UnitSummary.builder()
                    .id(ticket.getUnit().getId())
                    .unitNumber(ticket.getUnit().getUnitNumber())
                    .floor(ticket.getUnit().getFloor())
                    .build());
        }

        // Comments - filtrera bort interna kommentarer för boende
        builder.comments(ticket.getComments().stream()
                .filter(c -> !c.isInternal() || !principal.isResident())
                .map(c -> TicketResponse.CommentResponse.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .internal(c.isInternal())
                        .createdAt(c.getCreatedAt())
                        .author(TicketResponse.UserSummary.builder()
                                .id(c.getAuthor().getId())
                                .name(c.getAuthor().getName())
                                .email(c.getAuthor().getEmail())
                                .build())
                        .build())
                .toList());

        // Attachments
        builder.attachments(ticket.getAttachments().stream()
                .map(a -> TicketResponse.AttachmentResponse.builder()
                        .id(a.getId())
                        .fileName(a.getFileName())
                        .contentType(a.getContentType())
                        .fileSize(a.getFileSize())
                        .uploadedAt(a.getUploadedAt())
                        .build())
                .toList());

        return builder.build();
    }

    private TicketListResponse mapToListResponse(Ticket ticket) {
        return TicketListResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .categoryName(ticket.getCategory() != null ? ticket.getCategory().getName() : null)
                .categoryIcon(ticket.getCategory() != null ? ticket.getCategory().getIcon() : null)
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .reporterName(ticket.getReporter().getName())
                .assigneeName(ticket.getAssignee() != null ? ticket.getAssignee().getName() : null)
                .propertyName(ticket.getProperty().getName())
                .unitNumber(ticket.getUnit() != null ? ticket.getUnit().getUnitNumber() : null)
                .commentCount(ticket.getComments().size())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
