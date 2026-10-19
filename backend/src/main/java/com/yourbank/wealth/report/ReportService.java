package com.yourbank.wealth.report;

import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.portfolio.Portfolio;
import com.yourbank.wealth.portfolio.PortfolioService;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {

    public record ReportDto(
            Long id,
            Long portfolioId,
            ReportJob.Type type,
            String title,
            ReportJob.Status status,
            String requestedBy,
            Instant requestedAt,
            Instant completedAt,
            String fileName,
            String error) {

        static ReportDto from(ReportJob r) {
            return new ReportDto(r.getId(), r.getPortfolio().getId(), r.getType(), r.getType().title(), r.getStatus(),
                    r.getRequestedBy(), r.getRequestedAt(), r.getCompletedAt(), r.getFileName(), r.getError());
        }
    }

    public record Download(String fileName, byte[] content) {
    }

    private final ReportJobRepository jobs;
    private final PortfolioService portfolios;
    private final AuditService audit;
    private final Clock clock;

    public ReportService(ReportJobRepository jobs, PortfolioService portfolios, AuditService audit, Clock clock) {
        this.jobs = jobs;
        this.portfolios = portfolios;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReportDto> list(Long portfolioId, CurrentUser user) {
        portfolios.requireVisible(portfolioId, user);
        return jobs.findByPortfolioId(portfolioId).stream().map(ReportDto::from).toList();
    }

    /** Records the request; the caller then starts generation once this transaction has committed. */
    @Transactional
    public ReportDto request(Long portfolioId, ReportJob.Type type, CurrentUser user) {
        Portfolio portfolio = portfolios.requireVisible(portfolioId, user);
        ReportJob job = jobs.save(new ReportJob(portfolio, type, user.username(), clock.instant()));
        audit.record(user.username(), "REPORT_REQUESTED", type + " for " + portfolio.getPortfolioNumber());
        return ReportDto.from(job);
    }

    @Transactional
    public void complete(Long jobId, String fileName, byte[] pdf) {
        jobs.findById(jobId).ifPresent(j -> j.complete(fileName, Base64.getEncoder().encodeToString(pdf), clock.instant()));
    }

    @Transactional
    public void fail(Long jobId, String message) {
        jobs.findById(jobId).ifPresent(j -> j.fail(message, clock.instant()));
    }

    @Transactional(readOnly = true)
    public ReportJob job(Long jobId) {
        return jobs.findById(jobId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Download download(Long reportId, CurrentUser user) {
        ReportJob job = requireVisible(reportId, user);
        if (job.getStatus() != ReportJob.Status.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The report is not ready yet");
        }
        return new Download(job.getFileName(), Base64.getDecoder().decode(job.getContentBase64()));
    }

    @Transactional
    public void delete(Long reportId, CurrentUser user) {
        ReportJob job = requireVisible(reportId, user);
        jobs.delete(job);
        audit.record(user.username(), "REPORT_DELETED", "#" + reportId);
    }

    private ReportJob requireVisible(Long reportId, CurrentUser user) {
        ReportJob job = jobs.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        portfolios.requireVisible(job.getPortfolio().getId(), user);
        return job;
    }
}
