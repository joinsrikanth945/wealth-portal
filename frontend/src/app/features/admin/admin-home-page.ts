import { httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { AdminHome } from '../../core/api/models';
import { SessionStore } from '../../core/auth/session-store';

@Component({
  selector: 'app-admin-home-page',
  imports: [RouterLink],
  template: `
    <header class="page-header">
      <p class="eyebrow">Administration and compliance</p>
      <h1 class="display">Hello, {{ session.displayName() }}</h1>
    </header>

    @if (home.error()) {
      <p class="notice notice-danger">{{ message(home.error()) }}</p>
    } @else if (home.value(); as h) {
      <div class="tiles">
        <a
          class="tile tile-link"
          routerLink="/orders"
          [queryParams]="{ status: 'PENDING_APPROVAL' }"
          [class.tile-warn]="h.ordersAwaitingApproval > 0"
        >
          <span class="tile-label">Orders awaiting approval</span>
          <span class="tile-value">{{ h.ordersAwaitingApproval }}</span>
          <span class="tile-note">Four-eyes rule: you can't approve your own orders</span>
        </a>
        <div class="tile">
          <span class="tile-label">Clients</span>
          <span class="tile-value">{{ h.clients }}</span>
        </div>
        <div class="tile">
          <span class="tile-label">Portfolios</span>
          <span class="tile-value">{{ h.portfolios }}</span>
        </div>
      </div>
      <p class="notice">
        User, questionnaire and instrument administration arrive in a later phase.
      </p>
    } @else {
      <p class="muted">Loading…</p>
    }
  `,
})
export class AdminHomePage {
  protected readonly session = inject(SessionStore);
  protected readonly home = httpResource<AdminHome>(() => api('/admin/home'));

  protected message(e: unknown): string {
    return errorMessage(e);
  }
}
