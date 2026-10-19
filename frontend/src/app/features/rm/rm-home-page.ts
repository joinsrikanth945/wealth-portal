import { CurrencyPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { RmHome } from '../../core/api/models';
import { SessionStore } from '../../core/auth/session-store';
import { MifidBadgeComponent } from '../../shared/status-badge';

@Component({
  selector: 'app-rm-home-page',
  imports: [RouterLink, CurrencyPipe, MifidBadgeComponent],
  template: `
    <header class="page-header">
      <p class="eyebrow">Relationship manager</p>
      <h1 class="display">Good day, {{ session.displayName() }}</h1>
    </header>

    @if (home.isLoading()) {
      <p class="muted">Loading your book…</p>
    } @else if (home.error()) {
      <p class="notice notice-danger">{{ message(home.error()) }}</p>
      <button type="button" class="btn btn-ghost" (click)="home.reload()">Try again</button>
    } @else if (home.value(); as h) {
      <h2 class="section-title">Assets under management</h2>
      <div class="tiles">
        <a class="tile tile-link" routerLink="/rm/clients">
          <span class="tile-label">Total AUM</span>
          <span class="tile-value">{{ h.totalAum | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
        </a>
        <a class="tile tile-link" routerLink="/rm/clients">
          <span class="tile-label">Clients</span>
          <span class="tile-value">{{ h.clients }}</span>
        </a>
        <a class="tile tile-link" routerLink="/rm/clients">
          <span class="tile-label">Portfolios</span>
          <span class="tile-value">{{ h.portfolios }}</span>
        </a>
      </div>

      <h2 class="section-title">Client monitoring</h2>
      <div class="tiles">
        <div class="tile" [class.tile-alert]="h.mifidMissingOrExpired > 0">
          <span class="tile-label">Missing or expired MiFID profiles</span>
          <span class="tile-value">{{ h.mifidMissingOrExpired }}</span>
        </div>
        <div class="tile" [class.tile-warn]="h.mifidPendingValidation > 0">
          <span class="tile-label">Profiles awaiting the client</span>
          <span class="tile-value">{{ h.mifidPendingValidation }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Reviews in progress</span>
          <span class="tile-value">{{ h.mifidReviewsInProgress }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Proposals pending over 5 days</span>
          <span class="tile-value">—</span>
          <span class="tile-note">Arrives with proposals</span>
        </div>
      </div>

      <h2 class="section-title">Portfolio monitoring</h2>
      <div class="tiles">
        <div class="tile" [class.tile-warn]="h.highCashPortfolios > 0">
          <span class="tile-label">Cash ratio above 30%</span>
          <span class="tile-value">{{ h.highCashPortfolios }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Constraint breaches</span>
          <span class="tile-value">—</span>
          <span class="tile-note">Arrives with constraints</span>
        </div>
      </div>

      @if (h.clientsNeedingAttention.length) {
        <h2 class="section-title">Needs your attention</h2>
        <div class="card table-card">
          <div class="table-scroll">
            <table class="table">
              <thead>
                <tr>
                  <th>Client</th>
                  <th>MiFID profile</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (c of h.clientsNeedingAttention; track c.id) {
                  <tr>
                    <td>
                      {{ c.fullName }} <span class="muted small">{{ c.clientNumber }}</span>
                    </td>
                    <td><app-mifid-badge [status]="c.mifidStatus" /></td>
                    <td class="num"><a [routerLink]="['/rm/clients', c.id]">Open</a></td>
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
export class RmHomePage {
  protected readonly session = inject(SessionStore);
  protected readonly home = httpResource<RmHome>(() => api('/rm/home'));

  protected message(error: unknown): string {
    return errorMessage(error);
  }
}
