import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { MeResponse } from '../../core/api/session-api';
import { SessionStore } from '../../core/auth/session-store';
import { appConfig } from '../../core/config/app-config';

const PREFS_KEY = 'wealth.preferences';

interface Preferences {
  language: string;
  timeZone: string;
}

function readPrefs(): Preferences {
  try {
    const saved = localStorage.getItem(PREFS_KEY);
    if (saved) {
      return JSON.parse(saved) as Preferences;
    }
  } catch {
    /* fall through to defaults */
  }
  return { language: 'en', timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone };
}

@Component({
  selector: 'app-preferences-page',
  imports: [DatePipe],
  template: `
    <header class="page-header">
      <p class="eyebrow">Settings</p>
      <h1 class="display">Your preferences</h1>
    </header>

    <div class="grid-2">
      <section class="card">
        <h2 class="card-title">Profile</h2>
        @if (me.isLoading()) {
          <p class="muted">Loading…</p>
        } @else if (me.error()) {
          <p class="notice notice-danger">Couldn't load your profile. Is the backend running?</p>
          <button type="button" class="btn btn-ghost" (click)="me.reload()">Try again</button>
        } @else if (me.value(); as profile) {
          <dl class="details">
            <dt>Name</dt>
            <dd>{{ profile.displayName }}</dd>
            <dt>Username</dt>
            <dd>{{ profile.username }}</dd>
            <dt>Email</dt>
            <dd>{{ profile.email ?? '—' }}</dd>
            <dt>Roles</dt>
            <dd>{{ profile.roles.join(', ') }}</dd>
            <dt>Last sign-in</dt>
            <dd>
              {{ profile.lastLoginAt ? (profile.lastLoginAt | date: 'd MMM y, HH:mm') : '—' }}
            </dd>
          </dl>
        }
      </section>

      <section class="card">
        <h2 class="card-title">Display</h2>
        <div class="field">
          <label for="language">Language</label>
          <select id="language" [value]="prefs().language" (change)="setLanguage($event)">
            <option value="en">English</option>
            <option value="fr">Français</option>
            <option value="de">Deutsch</option>
          </select>
        </div>
        <div class="field">
          <label for="tz">Time zone</label>
          <select id="tz" [value]="prefs().timeZone" (change)="setTimeZone($event)">
            @for (zone of timeZones; track zone) {
              <option [value]="zone">{{ zone }}</option>
            }
          </select>
        </div>
        <p class="muted">Saved on this device. Translations arrive in a later phase.</p>
      </section>

      <section class="card">
        <h2 class="card-title">Security</h2>
        <p>Change your password or set up your authenticator app on the secure account page.</p>
        <a class="btn btn-primary" [href]="accountUrl" target="_blank" rel="noopener">
          Open account security
        </a>
      </section>
    </div>
  `,
})
export class PreferencesPage {
  private readonly session = inject(SessionStore);

  protected readonly me = httpResource<MeResponse>(() => `${appConfig().apiBaseUrl}/me`);
  protected readonly prefs = signal<Preferences>(readPrefs());
  protected readonly accountUrl = this.session.accountUrl();
  protected readonly timeZones = Array.from(
    new Set([
      readPrefs().timeZone,
      'Europe/London',
      'Europe/Paris',
      'Europe/Zurich',
      'Europe/Luxembourg',
      'Asia/Dubai',
      'Asia/Singapore',
      'Asia/Kolkata',
      'America/New_York',
    ]),
  );

  constructor() {
    effect(() => {
      try {
        localStorage.setItem(PREFS_KEY, JSON.stringify(this.prefs()));
      } catch {
        /* storage can be unavailable in private windows */
      }
    });
  }

  protected setLanguage(event: Event): void {
    const language = (event.target as HTMLSelectElement).value;
    this.prefs.update((p) => ({ ...p, language }));
  }

  protected setTimeZone(event: Event): void {
    const timeZone = (event.target as HTMLSelectElement).value;
    this.prefs.update((p) => ({ ...p, timeZone }));
  }
}
