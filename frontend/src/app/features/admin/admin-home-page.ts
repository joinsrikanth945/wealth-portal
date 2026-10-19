import { Component, inject } from '@angular/core';
import { SessionStore } from '../../core/auth/session-store';
import { HomeTile, HomeTilesComponent } from '../home/home-tiles';

@Component({
  selector: 'app-admin-home-page',
  imports: [HomeTilesComponent],
  template: `
    <header class="page-header">
      <p class="eyebrow">Administration</p>
      <h1 class="display">Hello, {{ session.displayName() }}</h1>
    </header>

    <app-home-tiles [tiles]="tiles" />

    <p class="notice">User, questionnaire and constraint administration arrive in phase 8.</p>
  `,
})
export class AdminHomePage {
  protected readonly session = inject(SessionStore);

  protected readonly tiles: HomeTile[] = [
    { label: 'Users awaiting approval', value: '—' },
    { label: 'Active questionnaires', value: '—' },
    { label: 'Mandatory alerts', value: '—' },
  ];
}
