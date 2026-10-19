package com.yourbank.wealth.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Generates reports in the background, so the API answers straight away with
 * a PENDING report and the page refreshes until it is COMPLETED.
 * On Azure this becomes a separate Container Apps job reading a queue.
 */
@Component
public class ReportWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportWorker.class);

    private final ReportService reports;
    private final ReportContent content;

    public ReportWorker(ReportService reports, ReportContent content) {
        this.reports = reports;
        this.content = content;
    }

    @Async
    public void generate(Long jobId) {
        try {
            ReportJob job = reports.job(jobId);
            ReportContent.Built built = content.build(job.getPortfolio().getId(), job.getType());
            reports.complete(jobId, built.fileName(), built.pdf());
        } catch (RuntimeException e) {
            log.error("Report {} failed", jobId, e);
            reports.fail(jobId, e.getMessage());
        }
    }
}
