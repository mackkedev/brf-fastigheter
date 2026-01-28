package se.fastighet.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import se.fastighet.core.dto.request.AddCommentRequest;
import se.fastighet.core.dto.request.CreateTicketRequest;
import se.fastighet.core.dto.request.UpdateTicketRequest;
import se.fastighet.core.dto.response.TicketListResponse;
import se.fastighet.core.dto.response.TicketResponse;
import se.fastighet.core.security.UserPrincipal;
import se.fastighet.core.service.TicketService;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "API för ärendehantering")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Skapa nytt ärende")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TicketResponse response = ticketService.createTicket(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ticketSecurity.canView(#id)")
    @Operation(summary = "Hämta ärende")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        TicketResponse response = ticketService.getTicket(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Hämta mina ärenden")
    public ResponseEntity<Page<TicketListResponse>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TicketListResponse> tickets = ticketService.getMyTickets(principal, pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'BOARD_MEMBER', 'ADMIN')")
    @Operation(summary = "Hämta tilldelade ärenden")
    public ResponseEntity<Page<TicketListResponse>> getAssignedTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TicketListResponse> tickets = ticketService.getAssignedTickets(principal, pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('BOARD_MEMBER', 'ADMIN')")
    @Operation(summary = "Hämta ärenden för fastighet")
    public ResponseEntity<Page<TicketListResponse>> getTicketsForProperty(
            @PathVariable UUID propertyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TicketListResponse> tickets = ticketService.getTicketsForProperty(propertyId, pageable);
        return ResponseEntity.ok(tickets);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@ticketSecurity.canUpdate(#id)")
    @Operation(summary = "Uppdatera ärende")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TicketResponse response = ticketService.updateTicket(id, request, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@ticketSecurity.canAssign(#id)")
    @Operation(summary = "Tilldela ärende till tekniker/styrelsemedlem")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long id,
            @RequestParam UUID assigneeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        TicketResponse response = ticketService.assignTicket(id, assigneeId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("@ticketSecurity.canComment(#id)")
    @Operation(summary = "Lägg till kommentar")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        TicketResponse response = ticketService.addComment(id, request, principal);
        return ResponseEntity.ok(response);
    }
}
