import { Role } from '../core/auth/roles';

export interface NavItem {
  label: string;
  path: string;
  queryParams?: Record<string, string>;
  /** Show the unread-messages count next to this item. */
  badge?: 'messages';
  /** Shown greyed-out until the phase that builds it. */
  comingSoon?: boolean;
}

const RM_NAV: NavItem[] = [
  { label: 'Home', path: '/rm/home' },
  { label: 'Clients', path: '/rm/clients' },
  { label: 'Proposals', path: '/proposals' },
  { label: 'Orders', path: '/orders' },
  { label: 'Messages', path: '/messages', badge: 'messages' },
];

const CLIENT_NAV: NavItem[] = [
  { label: 'Home', path: '/client/home' },
  { label: 'Proposals', path: '/proposals' },
  { label: 'Orders', path: '/orders' },
  { label: 'Messages', path: '/messages', badge: 'messages' },
];

const ADMIN_NAV: NavItem[] = [
  { label: 'Home', path: '/admin/home' },
  { label: 'Approvals', path: '/orders', queryParams: { status: 'PENDING_APPROVAL' } },
  { label: 'All orders', path: '/orders' },
  { label: 'Users', path: '/admin/users', comingSoon: true },
  { label: 'Questionnaires', path: '/admin/questionnaires', comingSoon: true },
  { label: 'Instruments', path: '/admin/instruments', comingSoon: true },
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
