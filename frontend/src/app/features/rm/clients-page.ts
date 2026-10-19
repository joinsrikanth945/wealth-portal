import { CurrencyPipe, DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { api, errorMessage } from '../../core/api/api-url';
import { ClientListItem } from '../../core/api/models';
import { CATEGORY_LABELS } from '../../shared/labels';
import { MifidBadgeComponent } from '../../shared/status-badge';

type Filter = 'all' | 'attention';

@Component({
  selector: 'app-clients-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, MifidBadgeComponent],
  template: `
    <header class="page-header row-between">
      <div>
        <p class="eyebrow">My book</p>
        <h1 class="display">Clients</h1>
      </div>
    </header>

    <div class="toolbar">
      <div class="field grow">
        <label for="search">Search</label>
        <input
          id="search"
          type="search"
          placeholder="Name or client number"
          [value]="query()"
          (input)="query.set($any($event.target).value)"
        />
      </div>
      <div class="segmented compact" role="group" aria-label="Show">
        <button
          type="button"
          [class.selected]="filter() === 'all'"
          [attr.aria-pressed]="filter() === 'all'"
          (click)="filter.set('all')"
        >
          All clients
        </button>
        <button
          type="button"
          [class.selected]="filter() === 'attention'"
          [attr.aria-pressed]="filter() === 'attention'"
          (click)="filter.set('attention')"
        >
          MiFID action needed
        </button>
      </div>
    </div>

    @if (clients.isLoading()) {
      <p class="muted">Loading clients…</p>
    } @else if (clients.error()) {
      <p class="notice notice-danger">{{ message(clients.error()) }}</p>
    } @else {
      <div class="card table-card">
        <div class="table-scroll">
          <table class="table">
            <thead>
              <tr>
                <th>Client</th>
                <th>Category</th>
                <th class="num">Portfolios</th>
                <th class="num">Total value</th>
                <th>MiFID profile</th>
                <th>Risk profile</th>
                <th>Valid until</th>
              </tr>
            </thead>
            <tbody>
              @for (c of visible(); track c.id) {
                <tr>
                  <td>
                    <a [routerLink]="['/rm/clients', c.id]">{{ c.fullName }}</a>
                    <div class="muted small">{{ c.clientNumber }} · {{ c.country }}</div>
                  </td>
                  <td>{{ categoryLabel(c) }}</td>
                  <td class="num">{{ c.portfolioCount }}</td>
                  <td class="num mono">
                    {{ c.totalValue | currency: 'EUR' : 'symbol' : '1.0-0' }}
                  </td>
                  <td><app-mifid-badge [status]="c.mifidStatus" /></td>
                  <td>{{ c.riskScore ? c.riskScore + ' — ' + c.riskLabel : '—' }}</td>
                  <td>{{ c.mifidValidUntil ? (c.mifidValidUntil | date: 'd MMM y') : '—' }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" class="muted">No clients match.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `,
})
export class ClientsPage {
  protected readonly clients = httpResource<ClientListItem[]>(() => api('/rm/clients'));
  protected readonly query = signal('');
  protected readonly filter = signal<Filter>('all');

  protected readonly visible = computed(() => {
    const q = this.query().trim().toLowerCase();
    return (this.clients.value() ?? []).filter(
      (c) =>
        (this.filter() === 'all' || c.mifidNextAction !== 'NONE') &&
        (!q || c.fullName.toLowerCase().includes(q) || c.clientNumber.toLowerCase().includes(q)),
    );
  });

  protected categoryLabel(c: ClientListItem): string {
    return CATEGORY_LABELS[c.category];
  }

  protected message(error: unknown): string {
    return errorMessage(error);
  }
}
