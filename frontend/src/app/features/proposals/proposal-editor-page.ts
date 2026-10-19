import { CurrencyPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { api, errorMessage } from '../../core/api/api-url';
import {
  Instrument,
  PortfolioDetail,
  ProposalDetail,
  ProposalRequest,
  Side,
} from '../../core/api/models';
import { WealthApi } from '../../core/api/wealth-api';
import { InstrumentPickerComponent } from '../../shared/instrument-picker';

interface EditLine {
  key: number;
  instrument: Instrument | null;
  side: Side;
  quantity: number | null;
  comment: string;
}

let nextKey = 1;

/** Build or edit a proposal: title, rationale and any number of buy/sell lines. */
@Component({
  selector: 'app-proposal-editor-page',
  imports: [RouterLink, CurrencyPipe, InstrumentPickerComponent],
  template: `
    @if (portfolio.value(); as p) {
      <a
        [routerLink]="proposalId() ? ['/proposals', proposalId()] : ['/portfolios', p.id]"
        class="back-link"
        >Back</a
      >
      <header class="page-header">
        <p class="eyebrow">{{ p.portfolioNumber }} · {{ p.clientName }}</p>
        <h1 class="display">{{ proposalId() ? 'Edit proposal' : 'New proposal' }}</h1>
      </header>

      @if (p.serviceType !== 'ADVISORY') {
        <p class="notice notice-warning">Proposals are only for advisory portfolios.</p>
      }

      <form class="card wizard-main" (submit)="$event.preventDefault(); save(p)">
        <div class="field">
          <label for="title">Title</label>
          <input
            id="title"
            type="text"
            maxlength="200"
            [value]="title()"
            (input)="title.set($any($event.target).value)"
          />
          @if (submitted() && !title().trim()) {
            <p class="field-error">Give the proposal a title.</p>
          }
        </div>
        <div class="field">
          <label for="rationale">Recommendation for the client</label>
          <textarea
            id="rationale"
            rows="4"
            maxlength="4000"
            [value]="rationale()"
            (input)="rationale.set($any($event.target).value)"
          ></textarea>
        </div>

        <h2 class="card-title">Transactions</h2>
        @for (l of lines(); track l.key; let i = $index) {
          <fieldset class="line-editor">
            <legend>Line {{ i + 1 }}</legend>
            <app-instrument-picker
              [inputId]="'pick-' + l.key"
              [selected]="l.instrument"
              (picked)="patch(l.key, { instrument: $event })"
            />
            <div class="form-grid">
              <div class="field">
                <label [for]="'side-' + l.key">Side</label>
                <select
                  [id]="'side-' + l.key"
                  [value]="l.side"
                  (change)="patch(l.key, { side: $any($event.target).value })"
                >
                  <option value="BUY">Buy</option>
                  <option value="SELL">Sell</option>
                </select>
              </div>
              <div class="field">
                <label [for]="'qty-' + l.key">{{
                  l.instrument?.assetClass === 'BOND' ? 'Nominal' : 'Quantity'
                }}</label>
                <input
                  [id]="'qty-' + l.key"
                  type="number"
                  min="0"
                  step="any"
                  [value]="l.quantity ?? ''"
                  (input)="patch(l.key, { quantity: toNumber($any($event.target).value) })"
                />
              </div>
              <div class="field grow">
                <label [for]="'comment-' + l.key">Comment</label>
                <input
                  [id]="'comment-' + l.key"
                  type="text"
                  maxlength="500"
                  [value]="l.comment"
                  (input)="patch(l.key, { comment: $any($event.target).value })"
                />
              </div>
            </div>
            <div class="row-between">
              <span class="muted small">
                @if (l.instrument && l.quantity) {
                  About {{ estimate(l) | currency: 'EUR' : 'symbol' : '1.0-0' }}
                }
              </span>
              <button
                type="button"
                class="btn btn-ghost btn-sm"
                (click)="removeLine(l.key)"
                [disabled]="lines().length === 1"
              >
                Remove line
              </button>
            </div>
            @if (submitted() && (!l.instrument || !l.quantity || l.quantity <= 0)) {
              <p class="field-error">Choose an instrument and a quantity above zero.</p>
            }
          </fieldset>
        }
        <button type="button" class="btn btn-ghost" (click)="addLine()">Add a line</button>

        @if (error()) {
          <p class="notice notice-danger" role="alert">{{ error() }}</p>
        }
        <div class="wizard-actions">
          <span class="muted small"
            >Saving keeps it as a draft. Pre-trade checks run on the next page.</span
          >
          <button
            type="submit"
            class="btn btn-primary"
            [disabled]="busy() || p.serviceType !== 'ADVISORY'"
          >
            {{ busy() ? 'Saving…' : 'Save proposal' }}
          </button>
        </div>
      </form>
    } @else if (portfolio.error() || existing.error()) {
      <p class="notice notice-danger">{{ message(portfolio.error() ?? existing.error()) }}</p>
    } @else {
      <p class="muted">Loading…</p>
    }
  `,
})
export class ProposalEditorPage {
  private readonly wealth = inject(WealthApi);
  private readonly router = inject(Router);

  /** /proposals/new?portfolioId=1  or  /proposals/:id/edit */
  readonly portfolioId = input<string | undefined>();
  readonly id = input<string | undefined>();

  protected readonly proposalId = computed(() => this.id());
  protected readonly existing = httpResource<ProposalDetail>(() =>
    this.id() ? api(`/proposals/${this.id()}`) : undefined,
  );
  private readonly targetPortfolio = computed(
    () => this.existing.value()?.summary.portfolioId?.toString() ?? this.portfolioId(),
  );
  protected readonly portfolio = httpResource<PortfolioDetail>(() =>
    this.targetPortfolio() ? api(`/portfolios/${this.targetPortfolio()}`) : undefined,
  );

  protected readonly title = signal('');
  protected readonly rationale = signal('');
  protected readonly lines = signal<EditLine[]>([this.blank()]);
  protected readonly submitted = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // Editing: copy the saved proposal into the form once it has loaded.
    effect(() => {
      const d = this.existing.value();
      if (!d) {
        return;
      }
      this.title.set(d.summary.title);
      this.rationale.set(d.rationale ?? '');
      this.lines.set(
        d.lines.map((l) => ({
          key: nextKey++,
          side: l.side,
          quantity: l.quantity,
          comment: l.comment ?? '',
          instrument: {
            id: l.instrumentId,
            code: l.instrumentCode,
            name: l.instrumentName,
            assetClass: l.assetClass,
            currency: 'EUR',
            exchange: null,
            riskClass: 0,
            complex: false,
            lastPrice: l.lastPrice,
            priceDate: '',
            underlyingId: null,
            optionType: null,
            strike: null,
            expiry: null,
            contractSize: null,
          },
        })),
      );
    });
  }

  protected addLine(): void {
    this.lines.update((ls) => [...ls, this.blank()]);
  }

  protected removeLine(key: number): void {
    this.lines.update((ls) => ls.filter((l) => l.key !== key));
  }

  protected patch(key: number, change: Partial<EditLine>): void {
    this.lines.update((ls) => ls.map((l) => (l.key === key ? { ...l, ...change } : l)));
  }

  protected toNumber(v: string): number | null {
    return v === '' ? null : Number(v);
  }

  protected estimate(l: EditLine): number {
    const i = l.instrument!;
    const q = l.quantity ?? 0;
    const mult = i.assetClass === 'BOND' ? 0.01 : (i.contractSize ?? 1);
    return q * i.lastPrice * mult;
  }

  protected async save(p: PortfolioDetail): Promise<void> {
    this.submitted.set(true);
    const ls = this.lines();
    if (!this.title().trim() || ls.some((l) => !l.instrument || !l.quantity || l.quantity <= 0)) {
      return;
    }
    const body: ProposalRequest = {
      portfolioId: p.id,
      title: this.title().trim(),
      rationale: this.rationale().trim(),
      lines: ls.map((l) => ({
        instrumentId: l.instrument!.id,
        side: l.side,
        quantity: l.quantity!,
        comment: l.comment.trim() || null,
      })),
    };
    this.busy.set(true);
    this.error.set(null);
    try {
      const id = this.id();
      const saved = await firstValueFrom(
        id ? this.wealth.updateProposal(Number(id), body) : this.wealth.createProposal(body),
      );
      this.router.navigate(['/proposals', saved.summary.id]);
    } catch (e) {
      this.error.set(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected message(e: unknown): string {
    return errorMessage(e);
  }

  private blank(): EditLine {
    return { key: nextKey++, instrument: null, side: 'BUY', quantity: null, comment: '' };
  }
}
