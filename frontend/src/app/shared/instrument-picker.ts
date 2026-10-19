import { DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, input, output, signal } from '@angular/core';
import { api } from '../core/api/api-url';
import { AssetClass, Instrument } from '../core/api/models';
import { ASSET_CLASS_LABELS } from './labels';

/** Search box for the security master: type 2+ letters, pick from the list. */
@Component({
  selector: 'app-instrument-picker',
  imports: [DecimalPipe],
  template: `
    <div class="picker">
      @if (selected(); as s) {
        <div class="picker-selected">
          <div>
            <strong>{{ s.name }}</strong>
            <div class="muted small">
              {{ s.code }} · {{ labels[s.assetClass] }} · risk {{ s.riskClass }}/7
              @if (s.complex) {
                · complex
              }
              · last {{ s.lastPrice | number: '1.2-2' }} {{ s.currency }}
            </div>
            @if (s.assetClass === 'OPTION' || s.assetClass === 'FUTURE') {
              <div class="muted small">
                {{ s.optionType ?? 'Future' }}{{ s.strike ? ' · strike ' + s.strike : '' }} · expiry
                {{ s.expiry }} · contract size {{ s.contractSize }}
              </div>
            }
          </div>
          <button type="button" class="btn btn-ghost" (click)="clear()">Change</button>
        </div>
      } @else {
        <label [for]="inputId()" class="sr-only">Search instruments</label>
        <input
          [id]="inputId()"
          type="search"
          class="input"
          placeholder="Search by name or ISIN, e.g. ASML"
          [value]="query()"
          (input)="query.set($any($event.target).value)"
          autocomplete="off"
        />
        @if (query().trim().length >= 2) {
          <ul class="picker-results" role="listbox">
            @for (i of results(); track i.id) {
              <li>
                <button type="button" class="picker-option" (click)="choose(i)">
                  <span>{{ i.name }}</span>
                  <span class="muted small"
                    >{{ i.code }} · {{ labels[i.assetClass] }} ·
                    {{ i.lastPrice | number: '1.2-2' }}</span
                  >
                </button>
              </li>
            } @empty {
              <li class="muted small picker-empty">
                {{ search.isLoading() ? 'Searching…' : 'No instruments found' }}
              </li>
            }
          </ul>
        }
      }
    </div>
  `,
})
export class InstrumentPickerComponent {
  /** Restrict to some asset classes (e.g. derivatives only). */
  readonly only = input<AssetClass[] | null>(null);
  readonly inputId = input('instrument-search');
  readonly selected = input<Instrument | null>(null);
  readonly picked = output<Instrument | null>();

  protected readonly labels = ASSET_CLASS_LABELS;
  protected readonly query = signal('');

  protected readonly search = httpResource<Instrument[]>(() => {
    const q = this.query().trim();
    return q.length >= 2 ? { url: api('/instruments'), params: { q } } : undefined;
  });

  protected readonly results = computed(() => {
    const only = this.only();
    return (this.search.value() ?? [])
      .filter((i) => !only || only.includes(i.assetClass))
      .slice(0, 8);
  });

  protected choose(i: Instrument): void {
    this.query.set('');
    this.picked.emit(i);
  }

  protected clear(): void {
    this.picked.emit(null);
  }
}
