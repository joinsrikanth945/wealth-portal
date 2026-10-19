import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { PortfolioDetail } from '../../core/api/models';
import { SessionStore } from '../../core/auth/session-store';
import { AllocationBarComponent } from '../../shared/allocation-bar';
import { ASSET_CLASS_LABELS, SERVICE_TYPE_LABELS } from '../../shared/labels';
import { PerformanceChartComponent } from '../../shared/performance-chart';

/** One portfolio, for RMs and clients alike (the server decides who may see it). */
@Component({
  selector: 'app-portfolio-page',
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    AllocationBarComponent,
    PerformanceChartComponent,
  ],
  template: `
    @if (portfolio.isLoading()) {
      <p class="muted">Loading portfolio…</p>
    } @else if (portfolio.error()) {
      <p class="notice notice-danger">{{ message(portfolio.error()) }}</p>
    } @else if (portfolio.value(); as p) {
      <a [routerLink]="backLink(p)" class="back-link">{{
        isRm() ? 'Back to ' + p.clientName : 'Back to home'
      }}</a>

      <header class="page-header">
        <p class="eyebrow">
          {{ p.portfolioNumber }} · {{ serviceLabels[p.serviceType] }} · {{ p.clientName }}
        </p>
        <h1 class="display">{{ p.name }}</h1>
      </header>

      <div class="tiles">
        <div class="tile">
          <span class="tile-label">Total value</span>
          <span class="tile-value">{{
            p.totalValue | currency: p.currency : 'symbol' : '1.0-0'
          }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">12-month performance</span>
          <span
            class="tile-value"
            [class.text-success]="p.performancePercent >= 0"
            [class.text-danger]="p.performancePercent < 0"
          >
            {{ p.performancePercent >= 0 ? '+' : '' }}{{ p.performancePercent | number: '1.2-2' }}%
          </span>
        </div>
        <div class="tile">
          <span class="tile-label">Unrealised gain / loss</span>
          <span
            class="tile-value"
            [class.text-success]="p.unrealizedPnl >= 0"
            [class.text-danger]="p.unrealizedPnl < 0"
          >
            {{ p.unrealizedPnl | currency: p.currency : 'symbol' : '1.0-0' }}
          </span>
        </div>
        <div class="tile">
          <span class="tile-label">Cash</span>
          <span class="tile-value">{{ p.cash | currency: p.currency : 'symbol' : '1.0-0' }}</span>
          <span class="tile-note" [class.text-warning]="p.cashRatioPercent > 30">
            {{ p.cashRatioPercent | number: '1.1-1' }}% of the portfolio
          </span>
        </div>
      </div>

      <div class="grid-2">
        <section class="card">
          <h2 class="card-title">Asset allocation</h2>
          <app-allocation-bar [allocation]="p.allocation" />
        </section>
        <section class="card">
          <h2 class="card-title">Performance</h2>
          <app-performance-chart [history]="p.history" />
        </section>
      </div>

      <h2 class="section-title">Holdings</h2>
      <div class="card table-card">
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>Instrument</th>
                <th>Type</th>
                <th class="num">Quantity</th>
                <th class="num">Avg. cost</th>
                <th class="num">Last price</th>
                <th class="num">Market value</th>
                <th class="num">Gain / loss</th>
                <th class="num">Weight</th>
              </tr>
            </thead>
            <tbody>
              @for (l of p.positions; track l.positionId) {
                <tr>
                  <td>
                    {{ l.instrumentName }}
                    <div class="muted small">
                      {{ l.instrumentCode }}{{ l.complex ? ' · complex' : '' }}
                    </div>
                  </td>
                  <td>{{ assetLabels[l.assetClass] }}</td>
                  <td class="num mono">{{ l.quantity | number: '1.0-2' }}</td>
                  <td class="num mono">{{ l.averageCost | number: '1.2-2' }}</td>
                  <td class="num mono" [title]="'Price date ' + (l.priceDate | date: 'd MMM y')">
                    {{ l.lastPrice | number: '1.2-2' }}
                  </td>
                  <td class="num mono">
                    {{ l.marketValue | currency: p.currency : 'symbol' : '1.0-0' }}
                  </td>
                  <td
                    class="num mono"
                    [class.text-success]="l.unrealizedPnl >= 0"
                    [class.text-danger]="l.unrealizedPnl < 0"
                  >
                    {{ l.unrealizedPnlPercent >= 0 ? '+' : ''
                    }}{{ l.unrealizedPnlPercent | number: '1.1-1' }}%
                  </td>
                  <td class="num">{{ l.weightPercent | number: '1.1-1' }}%</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="8" class="muted">No holdings.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
      <p class="muted small">
        Bond quantities are nominal amounts and prices are % of nominal. Demo prices.
      </p>
    }
  `,
})
export class PortfolioPage {
  private readonly session = inject(SessionStore);

  readonly id = input.required<string>();

  protected readonly portfolio = httpResource<PortfolioDetail>(() =>
    api(`/portfolios/${this.id()}`),
  );
  protected readonly isRm = computed(
    () => this.session.hasRole('RM') || this.session.hasRole('ADMIN'),
  );
  protected readonly serviceLabels = SERVICE_TYPE_LABELS;
  protected readonly assetLabels = ASSET_CLASS_LABELS;

  protected backLink(p: PortfolioDetail): unknown[] {
    return this.isRm() ? ['/rm/clients', p.clientId] : ['/client/home'];
  }

  protected message(error: unknown): string {
    return errorMessage(error);
  }
}
