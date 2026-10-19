import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnInit, effect, inject, signal, viewChild } from '@angular/core';
import { SessionApi, SessionStartResponse } from '../core/api/session-api';
import { SessionStore } from '../core/auth/session-store';

const STARTED_KEY = 'wealth.sessionStarted';

/**
 * Shown once per sign-in: tells the user when they last signed in, so they
 * can spot use of their account they don't recognise.
 */
@Component({
  selector: 'app-last-login-dialog',
  imports: [DatePipe],
  template: `
    <dialog #dialog class="dialog" aria-labelledby="last-login-title">
      @if (result(); as r) {
        <h2 id="last-login-title" class="dialog-title">Welcome{{ name() ? ', ' + name() : '' }}</h2>
        @if (r.firstLogin) {
          <p>This is your first sign-in. Your account is protected by two-factor authentication.</p>
        } @else {
          <p>
            Your last sign-in was on
            <strong>{{ r.previousLoginAt | date: 'EEEE d MMMM y, HH:mm' }}</strong
            >.
          </p>
          <p class="muted">If that wasn't you, change your password and contact the bank.</p>
        }
        <div class="dialog-actions">
          <button type="button" class="btn btn-primary" (click)="close()">Continue</button>
        </div>
      }
    </dialog>
  `,
})
export class LastLoginDialogComponent implements OnInit {
  private readonly api = inject(SessionApi);
  private readonly session = inject(SessionStore);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly result = signal<SessionStartResponse | null>(null);
  protected readonly name = this.session.displayName;

  constructor() {
    effect(() => {
      if (this.result()) {
        this.dialog().nativeElement.showModal();
      }
    });
  }

  ngOnInit(): void {
    const sessionKey = `${STARTED_KEY}.${this.session.profile()?.sessionId ?? 'unknown'}`;
    if (sessionStorage.getItem(sessionKey)) {
      return;
    }
    this.api.start().subscribe({
      next: (r) => {
        sessionStorage.setItem(sessionKey, '1');
        this.result.set(r);
      },
      error: () => {
        /* Not critical: the app keeps working if this call fails. */
      },
    });
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }
}
