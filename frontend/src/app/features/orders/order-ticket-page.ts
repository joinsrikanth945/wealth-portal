import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormField, form, hidden, min, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import {
  Instrument,
  LimitType,
  Order,
  OrderPreview,
  OrderRequest,
  PortfolioDetail,
  Side,
  TimeInForce,
} from '../../core/api/models';
import { WealthApi } from '../../core/api/wealth-api';
import { InstrumentPickerComponent } from '../../shared/instrument-picker';
import { SERVICE_TYPE_LABELS } from '../../shared/labels';

interface Ticket {
  side: Side;
  quantity: number | null;
  limitType: LimitType;
  limitPrice: number | null;
  stopPrice: number | null;
  timeInForce: TimeInForce;
  validUntil: string;
  negotiate: boolean;
  negotiatedFee: number | null;
}

const NEEDS_LIMIT: LimitType[] = ['LIMIT', 'STOP_LIMIT'];
const NEEDS_STOP: LimitType[] = ['STOP', 'STOP_LIMIT'];

/**
 * One ticket for securities and listed derivatives. Signal Forms drive the
 * field rules (limit price only for limit orders, a date only for good-till-date…);
 * a POST httpResource re-runs the server's pre-trade checks as the user types.
 */
@Component({
  selector: 'app-order-ticket-page',
  imports: [RouterLink, FormField, CurrencyPipe, DecimalPipe, InstrumentPickerComponent],
  template: `
    @if (portfolio.value(); as p) {
      <a [routerLink]="['/portfolios', p.id]" class="back-link">Back to {{ p.name }}</a>
      <header class="page-header">
        <p class="eyebrow">
          {{ p.portfolioNumber }} · {{ serviceLabels[p.serviceType] }} · {{ p.clientName }}
        </p>
        <h1 class="display">New order</h1>
      </header>

      @if (p.serviceType === 'DISCRETIONARY') {
        <p class="notice notice-warning">
          This is a discretionary portfolio: the bank manages it, so manual orders are not allowed.
        </p>
      }

      <div class="wizard">
        <form class="card wizard-main" (submit)="$event.preventDefault(); placeOrder(p)">
          <div class="field">
            <span class="legend-like">Instrument</span>
            <app-instrument-picker [selected]="instrument()" (picked)="instrument.set($event)" />
            @if (submitted() && !instrument()) {
              <p class="field-error">Choose an instrument.</p>
            }
          </div>

          <div class="segmented" role="group" aria-label="Buy or sell">
            <button
              type="button"
              [class.selected]="model().side === 'BUY'"
              [attr.aria-pressed]="model().side === 'BUY'"
              (click)="setSide('BUY')"
            >
              {{ isDerivative() ? 'Buy to open' : 'Buy' }}
            </button>
            <button
              type="button"
              [class.selected]="model().side === 'SELL'"
              [attr.aria-pressed]="model().side === 'SELL'"
              (click)="setSide('SELL')"
            >
              {{ isDerivative() ? 'Sell to open' : 'Sell' }}
            </button>
          </div>

          <div class="form-grid">
            <div class="field">
              <label for="qty">{{ quantityLabel() }}</label>
              <input id="qty" type="number" step="any" [formField]="ticket.quantity" />
              @if (showError(ticket.quantity().touched())) {
                <p class="field-error">{{ firstError(ticket.quantity().errors()) }}</p>
              }
            </div>

            <div class="field">
              <label for="limitType">Order type</label>
              <select id="limitType" [formField]="ticket.limitType">
                <option value="MARKET">Market</option>
                <option value="LIMIT">Limit</option>
                <option value="STOP">Stop</option>
                <option value="STOP_LIMIT">Stop limit</option>
              </select>
            </div>

            @if (!ticket.limitPrice().hidden()) {
              <div class="field">
                <label for="limitPrice">Limit price</label>
                <input
                  id="limitPrice"
                  type="number"
                  step="any"
                  [formField]="ticket.limitPrice"
                />
                @if (showError(ticket.limitPrice().touched())) {
                  <p class="field-error">{{ firstError(ticket.limitPrice().errors()) }}</p>
                }
              </div>
            }

            @if (!ticket.stopPrice().hidden()) {
              <div class="field">
                <label for="stopPrice">Stop price</label>
                <input
                  id="stopPrice"
                  type="number"
                  step="any"
                  [formField]="ticket.stopPrice"
                />
                @if (showError(ticket.stopPrice().touched())) {
                  <p class="field-error">{{ firstError(ticket.stopPrice().errors()) }}</p>
                }
              </div>
            }

            <div class="field">
              <label for="tif">Time in force</label>
              <select id="tif" [formField]="ticket.timeInForce">
                <option value="DAY">Day</option>
                <option value="GTC">Good till cancelled</option>
                <option value="GTD">Good till date</option>
              </select>
            </div>

            @if (!ticket.validUntil().hidden()) {
              <div class="field">
                <label for="validUntil">Valid until</label>
                <input id="validUntil" type="date" [formField]="ticket.validUntil" />
                @if (showError(ticket.validUntil().touched())) {
                  <p class="field-error">{{ firstError(ticket.validUntil().errors()) }}</p>
                }
              </div>
            }
          </div>

          <label class="check-row">
            <input type="checkbox" [formField]="ticket.negotiate" />
            Negotiated commission
          </label>
          @if (!ticket.negotiatedFee().hidden()) {
            <div class="field narrow-field">
              <label for="fee">Commission amount (EUR)</label>
              <input
                id="fee"
                type="number"
                step="0.01"
                [formField]="ticket.negotiatedFee"
              />
              @if (showError(ticket.negotiatedFee().touched())) {
                <p class="field-error">{{ firstError(ticket.negotiatedFee().errors()) }}</p>
              }
            </div>
          }

          @if (error()) {
            <p class="notice notice-danger" role="alert">{{ error() }}</p>
          }
          @if (placed(); as o) {
            <p class="notice" role="status">
              Order #{{ o.id }}
              {{
                o.status === 'FILLED'
                  ? 'filled at ' + (o.executedPrice | number: '1.2-2')
                  : o.status === 'PENDING_APPROVAL'
                    ? 'sent to compliance for approval'
                    : 'placed, waiting for its price'
              }}.
              <a routerLink="/orders">Open the order blotter</a>
            </p>
          }

          <div class="wizard-actions">
            <a class="btn btn-ghost" [routerLink]="['/portfolios', p.id]">Cancel</a>
            <button
              type="submit"
              class="btn btn-primary"
              [disabled]="busy() || (preview.value() && !preview.value()!.canSubmit)"
            >
              {{
                busy()
                  ? 'Placing…'
                  : preview.value()?.needsApproval
                    ? 'Submit for approval'
                    : 'Place order'
              }}
            </button>
          </div>
        </form>

        <aside class="wizard-side">
          <section class="card">
            <h2 class="card-title">Pre-trade checks</h2>
            @if (!request()) {
              <p class="muted small">Choose an instrument and quantity to run the checks.</p>
            } @else if (preview.error()) {
              <p class="notice notice-danger">{{ message(preview.error()) }}</p>
            } @else if (preview.value(); as pv) {
              <ul class="checks" [class.stale]="preview.isLoading()">
                @for (c of pv.checks; track c.code) {
                  <li class="check check-{{ c.outcome.toLowerCase() }}">
                    <span class="check-label">{{ c.label }}</span>
                    <span class="check-outcome">{{ outcomeLabel(c.outcome) }}</span>
                    @if (c.message) {
                      <span class="check-message">{{ c.message }}</span>
                    }
                  </li>
                }
              </ul>
              @if (!pv.canSubmit) {
                <p class="notice notice-danger">
                  This order can't be placed until the failed checks are fixed.
                </p>
              } @else if (pv.needsApproval) {
                <p class="notice notice-warning">
                  Warnings: compliance must approve this order before it goes to the market.
                </p>
              }
            } @else {
              <p class="muted small">Checking…</p>
            }
          </section>

          @if (preview.value(); as pv) {
            <section class="card">
              <h2 class="card-title">Estimated costs</h2>
              <dl class="details">
                <dt>{{ pv.natureLabel }} at</dt>
                <dd class="mono">{{ pv.estimatedPrice | number: '1.2-4' }}</dd>
                <dt>Gross amount</dt>
                <dd class="mono">{{ pv.grossAmount | currency: 'EUR' }}</dd>
                <dt>Commission</dt>
                <dd class="mono">
                  {{ pv.fees | currency: 'EUR' }}
                  @if (pv.fees !== pv.standardFees) {
                    <span class="muted small"
                      >(standard {{ pv.standardFees | currency: 'EUR' }})</span
                    >
                  }
                </dd>
                <dt>{{ model().side === 'BUY' ? 'You pay' : 'You receive' }}</dt>
                <dd class="mono">
                  <strong>{{ pv.netAmount | currency: 'EUR' }}</strong>
                </dd>
                <dt>Cash available</dt>
                <dd class="mono">{{ pv.cashAvailable | currency: 'EUR' }}</dd>
                <dt>Currently held</dt>
                <dd class="mono">{{ pv.quantityHeld | number: '1.0-2' }}</dd>
              </dl>
              <p class="muted small">
                Ex-ante cost disclosure (MiFID II). Execution is simulated at demo prices.
              </p>
            </section>
          }
        </aside>
      </div>
    } @else if (portfolio.error()) {
      <p class="notice notice-danger">{{ message(portfolio.error()) }}</p>
    } @else {
      <p class="muted">Loading…</p>
    }
  `,
})
export class OrderTicketPage {
  private readonly wealth = inject(WealthApi);
  private readonly router = inject(Router);

  /** Route /portfolios/:id/order?instrumentId=..&side=.. */
  readonly id = input.required<string>();
  readonly instrumentId = input<string | undefined>();
  readonly side = input<string | undefined>();

  protected readonly serviceLabels = SERVICE_TYPE_LABELS;
  protected readonly portfolio = httpResource<PortfolioDetail>(() =>
    api(`/portfolios/${this.id()}`),
  );
  private readonly preselected = httpResource<Instrument>(() =>
    this.instrumentId() ? api(`/instruments/${this.instrumentId()}`) : undefined,
  );

  protected readonly instrument = signal<Instrument | null>(null);
  protected readonly model = signal<Ticket>({
    side: 'BUY',
    quantity: null,
    limitType: 'MARKET',
    limitPrice: null,
    stopPrice: null,
    timeInForce: 'DAY',
    validUntil: '',
    negotiate: false,
    negotiatedFee: null,
  });

  /** The form and its rules. Hidden fields don't count towards validity. */
  protected readonly ticket = form(this.model, (t) => {
    required(t.quantity, { message: 'Enter a quantity' });
    min(t.quantity, 0.000001, { message: 'The quantity must be above zero' });

    hidden(t.limitPrice, ({ valueOf }) => !NEEDS_LIMIT.includes(valueOf(t.limitType)));
    required(t.limitPrice, {
      message: 'A limit order needs a limit price',
      when: ({ valueOf }) => NEEDS_LIMIT.includes(valueOf(t.limitType)),
    });

    hidden(t.stopPrice, ({ valueOf }) => !NEEDS_STOP.includes(valueOf(t.limitType)));
    required(t.stopPrice, {
      message: 'A stop order needs a stop price',
      when: ({ valueOf }) => NEEDS_STOP.includes(valueOf(t.limitType)),
    });

    hidden(t.validUntil, ({ valueOf }) => valueOf(t.timeInForce) !== 'GTD');
    required(t.validUntil, {
      message: 'Choose the last day the order stays open',
      when: ({ valueOf }) => valueOf(t.timeInForce) === 'GTD',
    });

    hidden(t.negotiatedFee, ({ valueOf }) => !valueOf(t.negotiate));
    required(t.negotiatedFee, {
      message: 'Enter the agreed commission',
      when: ({ valueOf }) => valueOf(t.negotiate),
    });
  });

  protected readonly isDerivative = computed(() => {
    const a = this.instrument()?.assetClass;
    return a === 'OPTION' || a === 'FUTURE';
  });
  protected readonly quantityLabel = computed(() => {
    const a = this.instrument()?.assetClass;
    return a === 'BOND' ? 'Nominal amount' : this.isDerivative() ? 'Contracts' : 'Quantity';
  });

  /** The request to preview, or undefined while the form is incomplete. */
  protected readonly request = computed<OrderRequest | undefined>(() => {
    const i = this.instrument();
    const m = this.model();
    if (!i || this.ticket().invalid()) {
      return undefined;
    }
    return {
      portfolioId: Number(this.id()),
      instrumentId: i.id,
      side: m.side,
      quantity: m.quantity!,
      limitType: m.limitType,
      limitPrice: NEEDS_LIMIT.includes(m.limitType) ? m.limitPrice : null,
      stopPrice: NEEDS_STOP.includes(m.limitType) ? m.stopPrice : null,
      timeInForce: m.timeInForce,
      validUntil: m.timeInForce === 'GTD' ? m.validUntil : null,
      negotiatedFee: m.negotiate ? m.negotiatedFee : null,
    };
  });

  protected readonly preview = httpResource<OrderPreview>(() => {
    const body = this.request();
    return body ? { url: api('/orders/preview'), method: 'POST', body } : undefined;
  });

  protected readonly submitted = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly placed = signal<Order | null>(null);

  constructor() {
    // Pre-fill from a "Buy" / "Sell" link on a holdings row.
    effect(() => {
      const pre = this.preselected.value();
      if (pre && !this.instrument()) {
        this.instrument.set(pre);
      }
    });
    effect(() => {
      const s = this.side();
      if (s === 'BUY' || s === 'SELL') {
        this.model.update((m) => ({ ...m, side: s }));
      }
    });
  }

  protected setSide(side: Side): void {
    this.model.update((m) => ({ ...m, side }));
  }

  protected showError(touched: boolean): boolean {
    return touched || this.submitted();
  }

  protected firstError(errors: readonly { message?: string }[]): string {
    return errors[0]?.message ?? '';
  }

  protected outcomeLabel(o: string): string {
    return o === 'PASS' ? 'Passed' : o === 'WARN' ? 'Warning' : 'Failed';
  }

  protected async placeOrder(p: PortfolioDetail): Promise<void> {
    this.submitted.set(true);
    const body = this.request();
    if (!body) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const order = await firstValueFrom(this.wealth.placeOrder(body));
      this.placed.set(order);
      this.portfolio.reload();
      this.preview.reload();
      if (order.status === 'FILLED') {
        setTimeout(() => this.router.navigate(['/portfolios', p.id]), 1500);
      }
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
