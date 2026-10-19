import { Role } from '../core/auth/roles';

export interface NavItem {
  label: string;
  path: string;
  /** Shown greyed-out until the phase that builds it. */
  comingSoon?: boolean;
}

const RM_NAV: NavItem[] = [
  { label: 'Home', path: '/rm/home' },
  { label: 'Clients', path: '/rm/clients' },
  { label: 'Proposals', path: '/rm/proposals', comingSoon: true },
  { label: 'Orders', path: '/rm/orders', comingSoon: true },
  { label: 'Reports', path: '/rm/reports', comingSoon: true },
  { label: 'Messages', path: '/messages', comingSoon: true },
];

const CLIENT_NAV: NavItem[] = [
  { label: 'Home', path: '/client/home' },
  { label: 'Proposals', path: '/client/proposals', comingSoon: true },
  { label: 'Documents', path: '/client/documents', comingSoon: true },
  { label: 'Messages', path: '/messages', comingSoon: true },
];

const ADMIN_NAV: NavItem[] = [
  { label: 'Home', path: '/admin/home' },
  { label: 'Users', path: '/admin/users', comingSoon: true },
  { label: 'Questionnaires', path: '/admin/questionnaires', comingSoon: true },
  { label: 'Instruments', path: '/admin/instruments', comingSoon: true },
  { label: 'Constraints', path: '/admin/constraints', comingSoon: true },
];

export function navItemsFor(role: Role | null): NavItem[] {
  switch (role) {
    case 'ADMIN':
      return ADMIN_NAV;
    case 'RM':
      return RM_NAV;
    case 'CLIENT':
      return CLIENT_NAV;
    default:
      return [];
  }
}
