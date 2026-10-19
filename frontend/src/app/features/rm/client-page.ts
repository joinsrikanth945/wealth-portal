import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { MifidApi } from '../../core/api/mifid-api';
import { ClientDetail } from '../../core/api/models';
import { CATEGORY_LABELS, SERVICE_TYPE_LABELS } from '../../shared/labels';
import { MifidBadgeComponent } from '../../shared/status-badge';

@Component({
  selector: 'app-client-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe, MifidBadgeComponent],
  template: `
    <a routerLink="/rm/clients" class="back-link">Back to clients</a>

    @if (client.isLoading()) {
      <p class="muted">Loading…</p>
    } @else if (client.error()) {
      <p class="notice notice-danger">{{ message(client.error()) }}</p>
    } @else if (client.value(); as c) {
      <header class="page-header">
        <p class="eyebrow">
          {{ c.clientNumber }} · {{ categoryLabels[c.category] }} · {{ c.country }}
        </p>
        <h1 class="display">{{ c.fullName }}</h1>
      </header>

      <div class="tiles">
        <div class="tile">
          <span class="tile-label">Total value</span>
          <span class="tile-value">{{ c.totalValue | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Portfolios</span>
          <span class="tile-value">{{ c.portfolios.length }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Risk profile</span>
          <span class="tile-value">{{ c.mifid.current?.riskScore ?? '—' }}</span>
          <span class="tile-note">{{ c.mifid.current?.riskLabel ?? 'No valid profile' }}</span>
        </div>
      </div>

      <div class="grid-2">
        <section class="card">
          <h2 class="card-title">MiFID suitability</h2>
          @if (c.mifid.current; as cur) {
            <dl class="details">
              <dt>Status</dt>
              <dd><app-mifid-badge [status]="cur.displayStatus" /></dd>
              <dt>Risk profile</dt>
              <dd>{{ cur.riskScore }} — {{ cur.riskLabel }}</dd>
              <dt>Complex products</dt>
              <dd>{{ cur.complexProductsOk ? 'Appropriate' : 'Not appropriate' }}</dd>
              <dt>Valid until</dt>
              <dd>{{ cur.validUntil | date: 'd MMM y' }}</dd>
              <dt>Signed</dt>
              <dd>{{ cur.signed ? 'Yes' : 'No' }}</dd>
            </dl>
          } @else {
            <p>This client has no valid suitability profile, so no advice can be given yet.</p>
          }

          @if (c.mifid.openReview; as open) {
            <p class="notice">
              <app-mifid-badge [status]="open.displayStatus" />
              Started by {{ open.createdBy }} on {{ open.createdAt | date: 'd MMM y' }} ·
              {{ open.completionPercent | number: '1.0-0' }}% complete
            </p>
          }

          @if (actionError()) {
            <p class="notice notice-danger">{{ actionError() }}</p>
          }
          <div class="actions">
            <button
              type="button"
              class="btn btn-primary"
              [disabled]="busy()"
              (click)="openReview(c.id)"
            >
              {{ actionLabel() }}
            </button>
          </div>
        </section>

        <section class="card">
          <h2 class="card-title">Contact</h2>
          <dl class="details">
            <dt>Email</dt>
            <dd>{{ c.email ?? '—' }}</dd>
            <dt>Phone</dt>
            <dd>{{ c.phone ?? '—' }}</dd>
            <dt>Date of birth</dt>
            <dd>{{ c.birthDate ? (c.birthDate | date: 'd MMM y') : '—' }}</dd>
            <dt>Relationship manager</dt>
            <dd>{{ c.rmUsername }}</dd>
          </dl>
        </section>
      </div>

      <h2 class="section-title">Portfolios</h2>
      <div class="card table-card">
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>Portfolio</th>
                <th>Service</th>
                <th class="num">Value</th>
                <th class="num">Cash</th>
                <th class="num">Cash ratio</th>
              </tr>
            </thead>
            <tbody>
              @for (p of c.portfolios; track p.id) {
                <tr>
                  <td>
                    <a [routerLink]="['/portfolios', p.id]">{{ p.name }}</a>
                    <div class="muted small">{{ p.portfolioNumber }}</div>
                  </td>
                  <td>{{ serviceLabels[p.serviceType] }}</td>
                  <td class="num mono">
                    {{ p.totalValue | currency: 'EUR' : 'symbol' : '1.0-0' }}
                  </td>
                  <td class="num mono">{{ p.cash | currency: 'EUR' : 'symbol' : '1.0-0' }}</td>
                  <td class="num" [class.text-warning]="p.cashRatioPercent > 30">
                    {{ p.cashRatioPercent | number: '1.1-1' }}%
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>

      @if (c.mifid.history.length) {
        <h2 class="section-title">Profile history</h2>
        <div class="card table-card">
          <div class="table-scroll">
            <table class="table">
              <thead>
                <tr>
                  <th>Started</th>
                  <th>Status</th>
                  <th>Risk</th>
                  <th>Completed</th>
                  <th>By</th>
                </tr>
              </thead>
              <tbody>
                @for (h of c.mifid.history; track h.id) {
                  <tr>
                    <td>{{ h.createdAt | date: 'd MMM y' }}</td>
                    <td><app-mifid-badge [status]="h.displayStatus" /></td>
                    <td>{{ h.riskScore ? h.riskScore + ' — ' + h.riskLabel : '—' }}</td>
                    <td>{{ h.completedAt ? (h.completedAt | date: 'd MMM y') : '—' }}</td>
                    <td>{{ h.createdBy }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    }
  `,
})
export class ClientPage {
  private readonly mifidApi = inject(MifidApi);
  private readonly router = inject(Router);

  /** From the route /rm/clients/:clientId */
  readonly clientId = input.required<string>();

  protected readonly client = httpResource<ClientDetail>(() => api(`/clients/${this.clientId()}`));
  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly categoryLabels = CATEGORY_LABELS;
  protected readonly serviceLabels = SERVICE_TYPE_LABELS;

  protected readonly actionLabel = computed(() => {
    switch (this.client.value()?.mifid.nextAction) {
      case 'PROFILE_NOW':
        return 'Profile now';
      case 'REVIEW_NOW':
        return 'Review now';
      case 'FINALIZE_REVIEW':
        return 'Continue review';
      default:
        return 'Start a new review';
    }
  });

  protected openReview(clientId: number): void {
    this.busy.set(true);
    this.actionError.set(null);
    this.mifidApi.startReview(clientId).subscribe({
      next: (review) => this.router.navigate(['/rm/clients', clientId, 'mifid', review.profile.id]),
      error: (e) => {
        this.busy.set(false);
        this.actionError.set(errorMessage(e));
      },
    });
  }

  protected message(error: unknown): string {
    return errorMessage(error);
  }
}
