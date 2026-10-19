export type Role = 'CLIENT' | 'RM' | 'ADMIN';

export const ALL_ROLES: readonly Role[] = ['ADMIN', 'RM', 'CLIENT'];

/** Highest-privilege role wins when a user holds several (ADMIN > RM > CLIENT). */
export function pickPrimaryRole(roles: readonly string[]): Role | null {
  return ALL_ROLES.find((r) => roles.includes(r)) ?? null;
}

/** Where each role lands after signing in. */
export function homePathFor(role: Role | null): string {
  switch (role) {
    case 'ADMIN':
      return '/admin/home';
    case 'RM':
      return '/rm/home';
    case 'CLIENT':
      return '/client/home';
    default:
      return '/forbidden';
  }
}

export const ROLE_LABELS: Record<Role, string> = {
  CLIENT: 'Client',
  RM: 'Relationship manager',
  ADMIN: 'Administrator',
};
