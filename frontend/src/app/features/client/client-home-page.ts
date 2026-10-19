import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { ClientDetail } from '../../core/api/models';
import { SessionStore } from '../../core/auth/session-store';
import { SERVICE_TYPE_LABELS } from '../../shared/labels';
import { MifidBadgeComponent } from '../../shared/status-badge';

@Component({
  selector: 'app-client-home-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, MifidBadgeComponent],
  template: `
    <header class="page-header">
      <p class="eyebrow">My wealth</p>
      <h1 class="display">Welcome, {{ session.displayName() }}</h1>
    </header>

    @if (me.isLoading()) {
      <p class="muted">Loading…</p>
    } @else if (me.error()) {
      <p class="notice notice-danger">{{ message(me.error()) }}</p>
    } @else if (me.value(); as c) {
      <div class="tiles">
        <div class="tile">
          <span class="tile-label">Total wealth with us</span>
          <span class="tile-value">{{ c.totalValue | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Investor profile</span>
          <span class="tile-value">{{ c.mifid.current?.riskScore ?? '—' }}</span>
          <span class="tile-note">{{ c.mifid.current?.riskLabel ?? 'Not yet assessed' }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Profile valid until</span>
          <span class="tile-value small-value">
            {{
              c.mifid.current?.validUntil ? (c.mifid.current!.validUntil | date: 'd MMM y') : '—'
            }}
          </span>
          @if (c.mifid.current; as cur) {
            <app-mifid-badge [status]="cur.displayStatus" />
          }
        </div>
      </div>

      <h2 class="section-title">My portfolios</h2>
      <div class="portfolio-cards">
        @for (p of c.portfolios; track p.id) {
          <a class="card portfolio-card" [routerLink]="['/portfolios', p.id]">
            <span class="muted small"
              >{{ p.portfolioNumber }} · {{ serviceLabels[p.serviceType] }}</span
            >
            <span class="card-title">{{ p.name }}</span>
            <span class="tile-value">{{
              p.totalValue | currency: p.currency : 'symbol' : '1.0-0'
            }}</span>
            <span class="link-like">View holdings and performance</span>
          </a>
        } @empty {
          <p class="muted">You have no portfolios yet.</p>
        }
      </div>
      <p class="muted small">Your relationship manager: {{ c.rmUsername }}</p>
    }
  `,
})
export class ClientHomePage {
  protected readonly session = inject(SessionStore);
  protected readonly me = httpResource<ClientDetail>(() => api('/client/me'));
  protected readonly serviceLabels = SERVICE_TYPE_LABELS;

  protected message(error: unknown): string {
    return errorMessage(error);
  }
}
