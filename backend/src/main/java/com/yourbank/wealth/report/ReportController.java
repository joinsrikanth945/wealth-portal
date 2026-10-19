package com.yourbank.wealth.report;

import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.report.ReportService.Download;
import com.yourbank.wealth.report.ReportService.ReportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reports", description = "Client statements and portfolio reports, generated in the background as PDF")
public class ReportController {

    public record ReportRequest(@NotNull ReportJob.Type type) {
    }

    private final ReportService reports;
    private final ReportWorker worker;

    public ReportController(ReportService reports, ReportWorker worker) {
        this.reports = reports;
        this.worker = worker;
    }

    @GetMapping("/api/portfolios/{portfolioId}/reports")
    @Operation(summary = "Reports generated for a portfolio, newest first")
    public List<ReportDto> list(@PathVariable Long portfolioId, @AuthenticationPrincipal Jwt jwt) {
        return reports.list(portfolioId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/portfolios/{portfolioId}/reports")
    @Operation(summary = "Request a report: STATEMENT, VALUATION, PERFORMANCE, TRANSACTIONS or COSTS. Returns PENDING")
    public ResponseEntity<ReportDto> request(@PathVariable Long portfolioId, @Valid @RequestBody ReportRequest body,
                                             @AuthenticationPrincipal Jwt jwt) {
        ReportDto created = reports.request(portfolioId, body.type(), CurrentUser.from(jwt));
        worker.generate(created.id()); // the request has committed, so the worker can see it
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
    }

    @GetMapping(value = "/api/reports/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download a completed report")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Download d = reports.download(id, CurrentUser.from(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.fileName()).build().toString())
                .body(d.content());
    }

    @DeleteMapping("/api/reports/{id}")
    @Operation(summary = "Delete a report")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        reports.delete(id, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
