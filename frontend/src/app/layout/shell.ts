import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { IsActiveMatchOptions } from '@angular/router';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ROLE_LABELS } from '../core/auth/roles';
import { SessionStore } from '../core/auth/session-store';
import { LastLoginDialogComponent } from './last-login-dialog';
import { navItemsFor } from './nav-items';
import { UnreadStore } from './unread-store';

/** The signed-in layout: side menu, top bar and the routed page. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LastLoginDialogComponent],
  template: `
    <a class="skip-link" href="#main">Skip to content</a>
    <div class="shell">
      <nav class="side-nav" [class.open]="menuOpen()" aria-label="Main">
        <div class="brand">[Your Bank] Wealth</div>
        @for (item of navItems(); track item.path) {
          @if (item.comingSoon) {
            <span class="nav-link disabled" aria-disabled="true">
              {{ item.label }} <small>soon</small>
            </span>
          } @else {
            <a
              class="nav-link"
              [routerLink]="item.path"
              [queryParams]="item.queryParams ?? null"
              routerLinkActive="active"
              [routerLinkActiveOptions]="exactMatch"
              (click)="menuOpen.set(false)"
            >
              {{ item.label }}
              @if (item.badge === 'messages' && unread.count() > 0) {
                <span class="count-badge" [attr.aria-label]="unread.count() + ' unread'">{{
                  unread.count()
                }}</span>
              }
            </a>
          }
        }
      </nav>

      <div class="shell-body">
        <header class="top-bar">
          <button
            type="button"
            class="btn btn-ghost menu-toggle"
            [attr.aria-expanded]="menuOpen()"
            aria-label="Toggle menu"
            (click)="menuOpen.update((v) => !v)"
          >
            Menu
          </button>
          <div class="top-bar-user">
            <span class="user-name">{{ session.displayName() }}</span>
            <span class="badge">{{ roleLabel() }}</span>
            <a class="btn btn-ghost" routerLink="/settings">Settings</a>
            <button type="button" class="btn btn-ghost" (click)="session.logout()">Sign out</button>
          </div>
        </header>
        <main id="main" class="page" tabindex="-1">
          <router-outlet />
        </main>
      </div>
    </div>
    <app-last-login-dialog />
  `,
})
export class ShellComponent {
  protected readonly session = inject(SessionStore);
  protected readonly unread = inject(UnreadStore);
  protected readonly menuOpen = signal(false);
  protected readonly exactMatch: IsActiveMatchOptions = {
    paths: 'subset',
    queryParams: 'exact',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };

  constructor() {
    this.unread.start();
    inject(DestroyRef).onDestroy(() => this.unread.stop());
  }
  protected readonly navItems = computed(() => navItemsFor(this.session.primaryRole()));
  protected readonly roleLabel = computed(() => {
    const role = this.session.primaryRole();
    return role ? ROLE_LABELS[role] : '';
  });
}
