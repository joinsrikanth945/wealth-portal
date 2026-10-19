import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { ProposalSummary } from '../../core/api/models';
import { SessionStore } from '../../core/auth/session-store';
import { PROPOSAL_STATUS } from './proposal-status';

@Component({
  selector: 'app-proposals-page',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  template: `
    <header class="page-header row-between">
      <div>
        <p class="eyebrow">Advice</p>
        <h1 class="display">Investment proposals</h1>
      </div>
    </header>
    @if (isRm()) {
      <p class="muted">
        To create a proposal, open an advisory portfolio and click <strong>New proposal</strong>.
      </p>
    }

    @if (proposals.isLoading() && !proposals.hasValue()) {
      <p class="muted">Loading…</p>
    } @else if (proposals.error()) {
      <p class="notice notice-danger">{{ message(proposals.error()) }}</p>
    } @else {
      <div class="card table-card">
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>Proposal</th>
                <th>Client / portfolio</th>
                <th class="num">Lines</th>
                <th class="num">Purchases</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              @for (p of proposals.value(); track p.id) {
                <tr>
                  <td>
                    <a [routerLink]="['/proposals', p.id]">{{ p.title }}</a>
                    <div class="muted small">#{{ p.id }} · {{ p.createdBy }}</div>
                  </td>
                  <td>
                    {{ p.clientName }}
                    <div class="muted small">{{ p.portfolioNumber }} {{ p.portfolioName }}</div>
                  </td>
                  <td class="num">{{ p.lineCount }}</td>
                  <td class="num mono">
                    {{ p.estimatedBuys | currency: 'EUR' : 'symbol' : '1.0-0' }}
                  </td>
                  <td>
                    <span class="status status-{{ status[p.status].tone }}">{{
                      status[p.status].label
                    }}</span>
                    @if (p.pendingOver5Days) {
                      <div class="small text-warning">Waiting over 5 days</div>
                    }
                  </td>
                  <td>{{ p.createdAt | date: 'd MMM y' }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" class="muted">No proposals yet.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `,
})
export class ProposalsPage {
  private readonly session = inject(SessionStore);
  protected readonly status = PROPOSAL_STATUS;
  protected readonly isRm = computed(
    () => this.session.hasRole('RM') || this.session.hasRole('ADMIN'),
  );
  protected readonly proposals = httpResource<ProposalSummary[]>(() => api('/proposals'));

  protected message(e: unknown): string {
    return errorMessage(e);
  }
}
