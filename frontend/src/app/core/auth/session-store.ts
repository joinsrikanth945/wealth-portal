import { Injectable, computed, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { AppConfig } from '../config/app-config';
import { Role, pickPrimaryRole } from './roles';

export interface UserProfile {
  username: string;
  displayName: string;
  email: string | null;
  roles: string[];
  sessionId: string | null;
}

const EXPECTED_ROLE_KEY = 'wealth.expectedRole';

/**
 * Holds who is signed in, as signals. Keycloak does the actual login
 * (password + two-factor) on its own hosted page; this store only wraps
 * the keycloak-js adapter.
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private keycloak: Keycloak | null = null;
  private readonly profileSignal = signal<UserProfile | null>(null);

  readonly profile = this.profileSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.profileSignal() !== null);
  readonly roles = computed(() => this.profileSignal()?.roles ?? []);
  readonly primaryRole = computed<Role | null>(() => pickPrimaryRole(this.roles()));
  readonly displayName = computed(() => this.profileSignal()?.displayName ?? '');

  private readonly unavailableSignal = signal(false);
  /** True when Keycloak could not be reached at start-up. */
  readonly unavailable = this.unavailableSignal.asReadonly();

  markUnavailable(): void {
    this.keycloak = null;
    this.unavailableSignal.set(true);
  }

  async init(config: AppConfig): Promise<void> {
    this.keycloak = new Keycloak({
      url: config.keycloak.url,
      realm: config.keycloak.realm,
      clientId: config.keycloak.clientId,
    });

    const authenticated = await this.keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    });

    this.keycloak.onTokenExpired = () => {
      this.keycloak?.updateToken(30).catch(() => this.clearSession());
    };

    if (authenticated) {
      this.readProfileFromToken();
    }
  }

  hasRole(role: Role): boolean {
    return this.roles().includes(role);
  }

  /** Sends the browser to Keycloak's sign-in page. */
  login(expectedRole: Role): Promise<void> {
    sessionStorage.setItem(EXPECTED_ROLE_KEY, expectedRole);
    return this.requireKeycloak().login({ redirectUri: `${window.location.origin}/` });
  }

  /** The role the user picked on our login page, read once after sign-in. */
  takeExpectedRole(): Role | null {
    const value = sessionStorage.getItem(EXPECTED_ROLE_KEY) as Role | null;
    sessionStorage.removeItem(EXPECTED_ROLE_KEY);
    return value;
  }

  logout(): Promise<void> {
    return this.requireKeycloak().logout({ redirectUri: `${window.location.origin}/login` });
  }

  /** Keycloak's own account page: change password, set up the authenticator app. */
  accountUrl(): string {
    return this.requireKeycloak().createAccountUrl();
  }

  /** A fresh access token, refreshed if it expires within 30 seconds. */
  async token(): Promise<string | null> {
    if (!this.keycloak?.authenticated) {
      return null;
    }
    try {
      await this.keycloak.updateToken(30);
      return this.keycloak.token ?? null;
    } catch {
      this.clearSession();
      return null;
    }
  }

  private readProfileFromToken(): void {
    const t = this.keycloak?.tokenParsed as Record<string, unknown> | undefined;
    if (!t) {
      return;
    }
    const realmAccess = t['realm_access'] as { roles?: string[] } | undefined;
    const given = (t['given_name'] as string | undefined) ?? '';
    const family = (t['family_name'] as string | undefined) ?? '';
    const username = (t['preferred_username'] as string | undefined) ?? '';
    this.profileSignal.set({
      username,
      displayName: `${given} ${family}`.trim() || username,
      email: (t['email'] as string | undefined) ?? null,
      roles: realmAccess?.roles ?? [],
      sessionId: (t['sid'] as string | undefined) ?? null,
    });
  }

  private clearSession(): void {
    this.profileSignal.set(null);
  }

  private requireKeycloak(): Keycloak {
    if (!this.keycloak) {
      throw new Error('Keycloak is not initialised');
    }
    return this.keycloak;
  }
}
