import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import { PortfolioDetail, Report, ReportType } from '../../core/api/models';
import { WealthApi, saveBlob } from '../../core/api/wealth-api';

const TYPES: { type: ReportType; label: string; text: string }[] = [
  {
    type: 'STATEMENT',
    label: 'Client statement',
    text: 'Summary, holdings and recent transactions',
  },
  {
    type: 'VALUATION',
    label: 'Portfolio valuation',
    text: 'Every position at today’s price, and the allocation',
  },
  { type: 'PERFORMANCE', label: 'Performance report', text: '12 months, month by month' },
  { type: 'TRANSACTIONS', label: 'Transaction history', text: 'All executed orders' },
  { type: 'COSTS', label: 'Ex-post costs and charges', text: 'MiFID II yearly cost disclosure' },
];

/** Request statements and reports; they're generated in the background as PDF. */
@Component({
  selector: 'app-reports-page',
  imports: [RouterLink, DatePipe],
  template: `
    @if (portfolio.value(); as p) {
      <a [routerLink]="['/portfolios', p.id]" class="back-link">Back to {{ p.name }}</a>
      <header class="page-header">
        <p class="eyebrow">{{ p.portfolioNumber }} · {{ p.clientName }}</p>
        <h1 class="display">Reports and statements</h1>
      </header>
    }

    <h2 class="section-title">Create a report</h2>
    <div class="portfolio-cards">
      @for (t of types; track t.type) {
        <button type="button" class="card report-card" [disabled]="busy()" (click)="create(t.type)">
          <span class="card-title">{{ t.label }}</span>
          <span class="muted small">{{ t.text }}</span>
          <span class="link-like">Generate PDF</span>
        </button>
      }
    </div>

    @if (error()) {
      <p class="notice notice-danger" role="alert">{{ error() }}</p>
    }

    <h2 class="section-title">Generated reports</h2>
    <div class="card table-card">
      <div class="table-scroll">
        <table class="table">
          <thead>
            <tr>
              <th>Report</th>
              <th>Requested</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (r of reports.value(); track r.id) {
              <tr>
                <td>
                  {{ r.title }}
                  <div class="muted small">{{ r.fileName ?? '' }}</div>
                </td>
                <td>
                  {{ r.requestedAt | date: 'd MMM y, HH:mm' }}
                  <div class="muted small">{{ r.requestedBy }}</div>
                </td>
                <td>
                  @switch (r.status) {
                    @case ('PENDING') {
                      <span class="status status-info">Generating…</span>
                    }
                    @case ('COMPLETED') {
                      <span class="status status-success">Ready</span>
                    }
                    @case ('FAILED') {
                      <span class="status status-danger">Failed</span>
                      <div class="small text-danger">{{ r.error }}</div>
                    }
                  }
                </td>
                <td class="num actions-cell">
                  @if (r.status === 'COMPLETED') {
                    <button type="button" class="btn btn-primary btn-sm" (click)="download(r)">
                      Download
                    </button>
                  }
                  <button type="button" class="btn btn-ghost btn-sm" (click)="remove(r)">
                    Delete
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="4" class="muted">No reports yet. Choose one above.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class ReportsPage {
  private readonly wealth = inject(WealthApi);

  readonly id = input.required<string>();

  protected readonly types = TYPES;
  protected readonly portfolio = httpResource<PortfolioDetail>(() =>
    api(`/portfolios/${this.id()}`),
  );
  protected readonly reports = httpResource<Report[]>(() =>
    api(`/portfolios/${this.id()}/reports`),
  );
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly anyPending = computed(() =>
    (this.reports.value() ?? []).some((r) => r.status === 'PENDING'),
  );

  constructor() {
    // While something is generating, refresh the list every 1.5 seconds.
    const timer = setInterval(() => {
      if (this.anyPending() && !this.reports.isLoading()) {
        this.reports.reload();
      }
    }, 1500);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected async create(type: ReportType): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.wealth.requestReport(Number(this.id()), type));
      this.reports.reload();
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async download(r: Report): Promise<void> {
    try {
      saveBlob(
        await firstValueFrom(this.wealth.downloadReport(r.id)),
        r.fileName ?? `report-${r.id}.pdf`,
      );
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected async remove(r: Report): Promise<void> {
    if (!confirm(`Delete "${r.title}"?`)) {
      return;
    }
    try {
      await firstValueFrom(this.wealth.deleteReport(r.id));
      this.reports.reload();
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }
}
