import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ROLE_LABELS, Role, homePathFor } from '../../core/auth/roles';
import { SessionStore } from '../../core/auth/session-store';

@Component({
  selector: 'app-forbidden-page',
  imports: [RouterLink],
  template: `
    <div class="centered-page">
      <div class="card narrow">
        <h1 class="display-sm">No access</h1>
        @if (expectedLabel(); as label) {
          <p>This account isn't registered as a {{ label.toLowerCase() }}.</p>
        } @else {
          <p>You don't have permission to open that page.</p>
        }
        <div class="actions">
          @if (session.isAuthenticated()) {
            <a class="btn btn-primary" [routerLink]="home()">Go to my homepage</a>
            <button type="button" class="btn btn-ghost" (click)="session.logout()">
              Sign in with another account
            </button>
          } @else {
            <a class="btn btn-primary" routerLink="/login">Sign in</a>
          }
        </div>
      </div>
    </div>
  `,
})
export class ForbiddenPage {
  protected readonly session = inject(SessionStore);
  /** Bound from the ?expected= query parameter. */
  readonly expected = input<Role | undefined>();

  protected readonly expectedLabel = computed(() => {
    const e = this.expected();
    return e && e in ROLE_LABELS ? ROLE_LABELS[e] : null;
  });
  protected readonly home = computed(() => homePathFor(this.session.primaryRole()));
}
