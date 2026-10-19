import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import { ProposalDetail } from '../../core/api/models';
import { WealthApi, saveBlob } from '../../core/api/wealth-api';
import { SessionStore } from '../../core/auth/session-store';
import { SignaturePadComponent } from '../../shared/signature-pad';
import { PROPOSAL_STATUS } from './proposal-status';

/** A proposal: lines with live checks, and the actions allowed for this user and status. */
@Component({
  selector: 'app-proposal-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe, SignaturePadComponent],
  template: `
    <a routerLink="/proposals" class="back-link">Back to proposals</a>
    @if (proposal.isLoading() && !proposal.hasValue()) {
      <p class="muted">Loading…</p>
    } @else if (proposal.error()) {
      <p class="notice notice-danger">{{ message(proposal.error()) }}</p>
    } @else if (proposal.value(); as d) {
      <header class="page-header">
        <p class="eyebrow">
          Proposal #{{ d.summary.id }} · {{ d.summary.clientName }} ·
          <a [routerLink]="['/portfolios', d.summary.portfolioId]"
            >{{ d.summary.portfolioNumber }} {{ d.summary.portfolioName }}</a
          >
        </p>
        <h1 class="display">{{ d.summary.title }}</h1>
        <p>
          <span class="status status-{{ status[d.summary.status].tone }}">{{
            status[d.summary.status].label
          }}</span>
          <span class="muted small">
            by {{ d.summary.createdBy }} on {{ d.summary.createdAt | date: 'd MMM y' }}
            @if (d.summary.sentAt) {
              · sent {{ d.summary.sentAt | date: 'd MMM y' }}
            }
            @if (d.decidedAt) {
              · decided {{ d.decidedAt | date: 'd MMM y' }} by {{ d.decidedBy }}
            }
          </span>
        </p>
      </header>

      @if (d.rationale) {
        <section class="card">
          <h2 class="card-title">Our recommendation</h2>
          <p class="pre-line">{{ d.rationale }}</p>
        </section>
      }

      <h2 class="section-title">Proposed transactions</h2>
      <div class="card table-card">
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>Side</th>
                <th>Instrument</th>
                <th class="num">Quantity</th>
                <th class="num">Price</th>
                <th class="num">Amount</th>
                <th class="num">Est. fees</th>
                <th>Checks</th>
              </tr>
            </thead>
            <tbody>
              @for (l of d.lines; track l.id) {
                <tr>
                  <td>
                    <strong>{{ l.side === 'BUY' ? 'Buy' : 'Sell' }}</strong>
                  </td>
                  <td>
                    {{ l.instrumentName }}
                    <div class="muted small">
                      {{ l.instrumentCode }}{{ l.comment ? ' · ' + l.comment : '' }}
                    </div>
                  </td>
                  <td class="num mono">{{ l.quantity | number: '1.0-2' }}</td>
                  <td class="num mono">{{ l.lastPrice | number: '1.2-2' }}</td>
                  <td class="num mono">
                    {{ l.estimatedAmount | currency: 'EUR' : 'symbol' : '1.0-0' }}
                  </td>
                  <td class="num mono">{{ l.estimatedFees | currency: 'EUR' }}</td>
                  <td>
                    @for (c of problems(l.checks); track c.code) {
                      <div
                        class="small"
                        [class.text-danger]="c.outcome === 'FAIL'"
                        [class.text-warning]="c.outcome === 'WARN'"
                      >
                        {{ c.label }}: {{ c.message }}
                      </div>
                    } @empty {
                      <span class="muted small">{{ l.checks.length ? 'All passed' : '—' }}</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
      <p class="muted small">
        Estimated purchases {{ d.summary.estimatedBuys | currency: 'EUR'
        }}{{
          d.summary.estimatedSells
            ? ' · sales ' + (d.summary.estimatedSells | currency: 'EUR')
            : ''
        }}. Demo prices.
      </p>

      @if (d.hasBlockingChecks) {
        <p class="notice notice-danger">
          Some lines fail the pre-trade checks. Edit the proposal before sending it.
        </p>
      } @else if (d.hasWarnings) {
        <p class="notice notice-warning">
          Some lines raise warnings: their orders will need compliance approval.
        </p>
      }
      @if (d.rejectReason) {
        <p class="notice">Declined: {{ d.rejectReason }}</p>
      }
      @if (d.orderIds.length) {
        <p class="notice">
          Orders placed: {{ d.orderIds.join(', ') }} · <a routerLink="/orders">open the blotter</a>
        </p>
      }
      @if (error()) {
        <p class="notice notice-danger" role="alert">{{ error() }}</p>
      }

      @if (d.summary.status === 'SENT_FOR_VALIDATION' && canDecide()) {
        <section class="card">
          <h2 class="card-title">
            {{ isClient() ? 'Your decision' : "Record the client's decision" }}
          </h2>
          <p class="muted small">
            By signing, the client confirms they have read the proposal, its costs and the
            suitability statement.
          </p>
          <app-signature-pad (signed)="signature.set($event)" />
          <div class="actions">
            <button
              type="button"
              class="btn btn-primary"
              [disabled]="busy() || !signature()"
              (click)="accept(d)"
            >
              Accept and sign
            </button>
            <button type="button" class="btn btn-ghost" [disabled]="busy()" (click)="decline(d)">
              Decline
            </button>
          </div>
        </section>
      }

      <div class="actions page-actions">
        <button type="button" class="btn btn-ghost" (click)="pdf(d)">Download PDF</button>
        @if (isRm()) {
          @if (d.summary.status === 'WORKING' || d.summary.status === 'SENT_FOR_VALIDATION') {
            <a class="btn btn-ghost" [routerLink]="['/proposals', d.summary.id, 'edit']">Edit</a>
          }
          @if (d.summary.status === 'WORKING') {
            <button type="button" class="btn btn-ghost" [disabled]="busy()" (click)="remove(d)">
              Delete
            </button>
            <button
              type="button"
              class="btn btn-primary"
              [disabled]="busy() || d.hasBlockingChecks"
              (click)="send(d)"
            >
              Send to client
            </button>
          }
          @if (d.summary.status === 'VALIDATED') {
            <button type="button" class="btn btn-primary" [disabled]="busy()" (click)="execute(d)">
              Place the orders
            </button>
          }
        }
      </div>
    }
  `,
})
export class ProposalPage {
  private readonly wealth = inject(WealthApi);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly status = PROPOSAL_STATUS;
  protected readonly proposal = httpResource<ProposalDetail>(() => api(`/proposals/${this.id()}`));
  protected readonly isRm = computed(
    () => this.session.hasRole('RM') || this.session.hasRole('ADMIN'),
  );
  protected readonly isClient = computed(() => this.session.hasRole('CLIENT') && !this.isRm());
  protected readonly canDecide = computed(() => true);
  protected readonly signature = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected problems(checks: ProposalDetail['lines'][number]['checks']) {
    return checks.filter((c) => c.outcome !== 'PASS');
  }

  protected send(d: ProposalDetail) {
    return this.run(() => this.wealth.sendProposal(d.summary.id));
  }

  protected execute(d: ProposalDetail) {
    return confirm('Place market orders for every line of this proposal?')
      ? this.run(() => this.wealth.executeProposal(d.summary.id))
      : Promise.resolve();
  }

  protected accept(d: ProposalDetail) {
    return this.run(() => this.wealth.validateProposal(d.summary.id, this.signature()!));
  }

  protected decline(d: ProposalDetail) {
    const reason = prompt('Reason for declining (optional):', '');
    return reason === null
      ? Promise.resolve()
      : this.run(() => this.wealth.rejectProposal(d.summary.id, reason));
  }

  protected async remove(d: ProposalDetail): Promise<void> {
    if (!confirm('Delete this draft proposal?')) {
      return;
    }
    try {
      await firstValueFrom(this.wealth.deleteProposal(d.summary.id));
      this.router.navigate(['/proposals']);
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  protected async pdf(d: ProposalDetail): Promise<void> {
    try {
      saveBlob(
        await firstValueFrom(this.wealth.proposalPdf(d.summary.id)),
        `proposal-${d.summary.id}.pdf`,
      );
    } catch (e) {
      this.error.set(errorMessage(e));
    }
  }

  private async run(call: () => Observable<ProposalDetail>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.proposal.set(await firstValueFrom(call()));
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected message(e: unknown): string {
    return errorMessage(e);
  }
}
