package com.yourbank.wealth.proposal;

import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.proposal.ProposalDtos.ProposalDetail;
import com.yourbank.wealth.proposal.ProposalDtos.ProposalRequest;
import com.yourbank.wealth.proposal.ProposalDtos.ProposalSummary;
import com.yourbank.wealth.proposal.ProposalDtos.RejectRequest;
import com.yourbank.wealth.proposal.ProposalDtos.SignRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Proposals", description = "Investment proposals: build, send, client acceptance with signature, execute")
public class ProposalController {

    private final ProposalService proposals;

    public ProposalController(ProposalService proposals) {
        this.proposals = proposals;
    }

    @GetMapping("/api/proposals")
    @Operation(summary = "Proposals the user may see (clients don't see drafts)")
    public List<ProposalSummary> list(@RequestParam(required = false) Long portfolioId, @AuthenticationPrincipal Jwt jwt) {
        return proposals.list(CurrentUser.from(jwt), portfolioId);
    }

    @GetMapping("/api/proposals/{id}")
    @Operation(summary = "One proposal with its lines and the live pre-trade checks of each line")
    public ProposalDetail get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return proposals.get(id, CurrentUser.from(jwt));
    }

    @GetMapping(value = "/api/proposals/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "The proposal as a PDF document")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        byte[] bytes = proposals.pdf(id, CurrentUser.from(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("proposal-" + id + ".pdf").build().toString())
                .body(bytes);
    }

    @PostMapping("/api/rm/proposals")
    @Operation(summary = "Create a proposal (advisory portfolios only)")
    public ProposalDetail create(@Valid @RequestBody ProposalRequest request, @AuthenticationPrincipal Jwt jwt) {
        return proposals.create(request, CurrentUser.from(jwt));
    }

    @PutMapping("/api/rm/proposals/{id}")
    @Operation(summary = "Edit a proposal; a sent one goes back to WORKING")
    public ProposalDetail update(@PathVariable Long id, @Valid @RequestBody ProposalRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        return proposals.update(id, request, CurrentUser.from(jwt));
    }

    @DeleteMapping("/api/rm/proposals/{id}")
    @Operation(summary = "Delete a WORKING proposal")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        proposals.delete(id, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/rm/proposals/{id}/send")
    @Operation(summary = "Send to the client for validation")
    public ProposalDetail send(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return proposals.send(id, CurrentUser.from(jwt));
    }

    @PostMapping("/api/rm/proposals/{id}/execute")
    @Operation(summary = "Place the orders of an accepted proposal")
    public ProposalDetail execute(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return proposals.execute(id, CurrentUser.from(jwt));
    }

    @PostMapping("/api/proposals/{id}/validate")
    @Operation(summary = "Client accepts and signs (or the RM records the client's signature)")
    public ProposalDetail validate(@PathVariable Long id, @Valid @RequestBody SignRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return proposals.validate(id, request.signature(), CurrentUser.from(jwt));
    }

    @PostMapping("/api/proposals/{id}/reject")
    @Operation(summary = "Client declines the proposal")
    public ProposalDetail reject(@PathVariable Long id, @RequestBody(required = false) RejectRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        return proposals.reject(id, request == null ? null : request.reason(), CurrentUser.from(jwt));
    }
}
