import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import { Order, OrderStatus } from '../../core/api/models';
import { WealthApi } from '../../core/api/wealth-api';
import { SessionStore } from '../../core/auth/session-store';

const STATUS: Record<OrderStatus, { label: string; tone: string }> = {
  PENDING_APPROVAL: { label: 'Awaiting approval', tone: 'warning' },
  PLACED: { label: 'Placed', tone: 'info' },
  FILLED: { label: 'Filled', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'neutral' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
};

/** The order blotter for every role. Compliance (admin) approves or rejects from here. */
@Component({
  selector: 'app-orders-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe],
  template: `
    <header class="page-header">
      <p class="eyebrow">{{ isAdmin() ? 'Compliance' : 'Trading' }}</p>
      <h1 class="display">
        {{ filter() === 'PENDING_APPROVAL' && isAdmin() ? 'Orders awaiting approval' : 'Orders' }}
      </h1>
    </header>

    <div class="toolbar">
      <div class="field">
        <label for="status">Status</label>
        <select id="status" [value]="filter()" (change)="filter.set($any($event.target).value)">
          <option value="">All</option>
          <option value="PENDING_APPROVAL">Awaiting approval</option>
          <option value="PLACED">Placed</option>
          <option value="FILLED">Filled</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>
      <button type="button" class="btn btn-ghost" (click)="orders.reload()">Refresh</button>
    </div>

    @if (actionError()) {
      <p class="notice notice-danger" role="alert">{{ actionError() }}</p>
    }

    @if (orders.isLoading() && !orders.hasValue()) {
      <p class="muted">Loading orders…</p>
    } @else if (orders.error()) {
      <p class="notice notice-danger">{{ message(orders.error()) }}</p>
    } @else {
      <div class="card table-card">
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Client / portfolio</th>
                <th>Order</th>
                <th class="num">Quantity</th>
                <th>Type</th>
                <th class="num">Net amount</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (o of visible(); track o.id) {
                <tr>
                  <td>
                    {{ o.createdAt | date: 'd MMM, HH:mm' }}
                    <div class="muted small">#{{ o.id }} · {{ o.createdBy }}</div>
                  </td>
                  <td>
                    {{ o.clientName }}
                    <div class="muted small">
                      <a [routerLink]="['/portfolios', o.portfolioId]">{{ o.portfolioNumber }}</a>
                    </div>
                  </td>
                  <td>
                    <strong>{{ o.natureLabel }}</strong> {{ o.instrumentName }}
                    <div class="muted small">
                      {{ o.instrumentCode
                      }}{{ o.proposalId ? ' · from proposal #' + o.proposalId : '' }}
                    </div>
                    @if (o.checkSummary) {
                      <div class="small text-warning">{{ o.checkSummary }}</div>
                    }
                    @if (o.decisionNote && o.status !== 'FILLED') {
                      <div class="muted small">{{ o.decidedBy }}: {{ o.decisionNote }}</div>
                    }
                  </td>
                  <td class="num mono">{{ o.quantity | number: '1.0-2' }}</td>
                  <td>
                    {{ typeLabel(o) }}
                    <div class="muted small">
                      {{ o.timeInForce }}{{ o.validUntil ? ' ' + o.validUntil : '' }}
                    </div>
                  </td>
                  <td class="num mono">
                    {{ o.netAmount | currency: 'EUR' }}
                    @if (o.executedPrice) {
                      <div class="muted small">at {{ o.executedPrice | number: '1.2-2' }}</div>
                    }
                  </td>
                  <td>
                    <span class="status status-{{ status[o.status].tone }}">{{
                      status[o.status].label
                    }}</span>
                  </td>
                  <td class="num actions-cell">
                    @if (o.status === 'PENDING_APPROVAL' && isAdmin() && o.createdBy !== me()) {
                      <button
                        type="button"
                        class="btn btn-primary btn-sm"
                        [disabled]="busy()"
                        (click)="approve(o)"
                      >
                        Approve
                      </button>
                      <button
                        type="button"
                        class="btn btn-ghost btn-sm"
                        [disabled]="busy()"
                        (click)="reject(o)"
                      >
                        Reject
                      </button>
                    } @else if (o.status === 'PENDING_APPROVAL' || o.status === 'PLACED') {
                      <button
                        type="button"
                        class="btn btn-ghost btn-sm"
                        [disabled]="busy()"
                        (click)="cancel(o)"
                      >
                        Cancel
                      </button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="8" class="muted">No orders.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `,
})
export class OrdersPage {
  private readonly wealth = inject(WealthApi);
  private readonly session = inject(SessionStore);

  /** ?status=PENDING_APPROVAL from the admin menu. */
  readonly status_ = input<string | undefined>(undefined, { alias: 'status' });

  protected readonly status = STATUS;
  protected readonly isAdmin = computed(() => this.session.hasRole('ADMIN'));
  protected readonly me = computed(() => this.session.profile()?.username ?? '');
  protected readonly filter = linkedSignal(() => this.status_() ?? '');
  protected readonly orders = httpResource<Order[]>(() => api('/orders'));
  protected readonly busy = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly visible = computed(() => {
    const f = this.filter();
    return (this.orders.value() ?? []).filter((o) => !f || o.status === f);
  });

  protected typeLabel(o: Order): string {
    switch (o.limitType) {
      case 'MARKET':
        return 'Market';
      case 'LIMIT':
        return `Limit ${o.limitPrice}`;
      case 'STOP':
        return `Stop ${o.stopPrice}`;
      default:
        return `Stop ${o.stopPrice} / limit ${o.limitPrice}`;
    }
  }

  protected approve(o: Order): Promise<void> {
    const note = prompt(`Approve order #${o.id}? Optional note:`, 'Approved after review');
    return note === null ? Promise.resolve() : this.run(() => this.wealth.approveOrder(o.id, note));
  }

  protected reject(o: Order): Promise<void> {
    const note = prompt(`Reject order #${o.id}? Reason:`, '');
    return note === null ? Promise.resolve() : this.run(() => this.wealth.rejectOrder(o.id, note));
  }

  protected cancel(o: Order): Promise<void> {
    return confirm(`Cancel order #${o.id}?`)
      ? this.run(() => this.wealth.cancelOrder(o.id))
      : Promise.resolve();
  }

  private async run(call: () => Observable<Order>): Promise<void> {
    this.busy.set(true);
    this.actionError.set(null);
    try {
      await firstValueFrom(call());
      this.orders.reload();
    } catch (e) {
      this.actionError.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected message(e: unknown): string {
    return errorMessage(e);
  }
}
