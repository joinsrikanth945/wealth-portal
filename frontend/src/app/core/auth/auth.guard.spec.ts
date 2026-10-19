import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { roleGuard } from './auth.guard';
import { SessionStore } from './session-store';

function fakeSession(roles: string[] | null) {
  const profile = signal(roles ? { roles } : null);
  return {
    isAuthenticated: computed(() => profile() !== null),
    hasRole: (r: string) => (profile()?.roles ?? []).includes(r),
  };
}

function runGuard(roles: string[] | null, role: 'RM' | 'CLIENT' | 'ADMIN') {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: SessionStore, useValue: fakeSession(roles) }],
  });
  return TestBed.runInInjectionContext(() =>
    roleGuard(role)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
}

function path(result: unknown): string {
  return TestBed.inject(Router).serializeUrl(result as UrlTree);
}

describe('roleGuard', () => {
  it('lets an RM into the RM area', () => {
    expect(runGuard(['RM'], 'RM')).toBe(true);
  });

  it('sends a client away from the RM area', () => {
    expect(path(runGuard(['CLIENT'], 'RM'))).toBe('/forbidden');
  });

  it('sends a signed-out visitor to the login page', () => {
    expect(path(runGuard(null, 'CLIENT'))).toBe('/login');
  });
});
