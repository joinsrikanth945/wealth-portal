import { Component, computed, inject, signal } from '@angular/core';
import { Role } from '../../core/auth/roles';
import { SessionStore } from '../../core/auth/session-store';

/**
 * Our branded entry page. The user picks Client or Relationship manager,
 * then signs in on Keycloak's secure page (password, then the authenticator
 * code). We never handle passwords in Angular.
 */
@Component({
  selector: 'app-login-page',
  template: `
    <div class="login">
      <section class="login-hero">
        <div class="brand">[Your Bank] Wealth</div>
        <div>
          <h1 class="display">Private wealth, managed with clarity.</h1>
          <p class="hero-text">
            Portfolios, proposals, orders and statements in one secure place for clients and their
            relationship managers.
          </p>
        </div>
        <p class="hero-foot">Regulated investment services. MiFID II compliant.</p>
      </section>

      <section class="login-panel">
        <div class="login-card">
          <h2 class="display-sm">Sign in</h2>

          <div class="segmented" role="group" aria-label="Sign in as">
            <button
              type="button"
              [class.selected]="role() === 'CLIENT'"
              [attr.aria-pressed]="role() === 'CLIENT'"
              (click)="role.set('CLIENT')"
            >
              Client
            </button>
            <button
              type="button"
              [class.selected]="role() === 'RM'"
              [attr.aria-pressed]="role() === 'RM'"
              (click)="role.set('RM')"
            >
              Relationship manager
            </button>
          </div>

          @if (role() === 'RM') {
            <p class="notice notice-warning">
              Staff access is recorded in the audit trail. An authenticator app code is required.
            </p>
          } @else {
            <p class="muted">
              You'll be asked for your password and a code from your authenticator app.
            </p>
          }

          @if (unavailable()) {
            <p class="notice notice-danger" role="alert">
              The sign-in service isn't reachable. If you just started Keycloak, wait a minute and
              reload the page.
            </p>
          }

          <button
            type="button"
            class="btn btn-primary btn-block"
            [disabled]="busy() || unavailable()"
            (click)="signIn()"
          >
            {{ busy() ? 'Opening secure sign-in…' : buttonLabel() }}
          </button>

          <p class="muted center">Administrators sign in with the Relationship manager option.</p>
        </div>
      </section>
    </div>
  `,
})
export class LoginPage {
  private readonly session = inject(SessionStore);

  protected readonly role = signal<Role>('CLIENT');
  protected readonly busy = signal(false);
  protected readonly unavailable = this.session.unavailable;
  protected readonly buttonLabel = computed(() =>
    this.role() === 'RM' ? 'Continue to staff sign-in' : 'Continue to secure sign-in',
  );

  protected async signIn(): Promise<void> {
    this.busy.set(true);
    try {
      await this.session.login(this.role());
    } catch {
      this.busy.set(false);
    }
  }
}
