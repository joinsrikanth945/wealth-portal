import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role, homePathFor } from './roles';
import { SessionStore } from './session-store';

/** Any signed-in user. Others go to the login page. */
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionStore);
  return session.isAuthenticated() || inject(Router).createUrlTree(['/login']);
};

/** Only users holding the given role. Signed-in users without it see "forbidden". */
export const roleGuard =
  (role: Role): CanActivateFn =>
  () => {
    const session = inject(SessionStore);
    const router = inject(Router);
    if (!session.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }
    return session.hasRole(role) || router.createUrlTree(['/forbidden']);
  };

/** The login page is only for signed-out users. */
export const signedOutGuard: CanActivateFn = () => {
  const session = inject(SessionStore);
  return session.isAuthenticated()
    ? inject(Router).createUrlTree([homePathFor(session.primaryRole())])
    : true;
};

/**
 * The "/" route: sends a signed-in user to their homepage. If they chose
 * "Relationship manager" on the login page but the account has no RM role,
 * they are told rather than silently sent to the client area.
 */
export const homeRedirectGuard: CanActivateFn = () => {
  const session = inject(SessionStore);
  const router = inject(Router);
  if (!session.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  const expected = session.takeExpectedRole();
  if (expected && !session.hasRole(expected)) {
    return router.createUrlTree(['/forbidden'], { queryParams: { expected } });
  }
  return router.createUrlTree([homePathFor(session.primaryRole())]);
};
