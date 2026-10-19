import { AssetClass, ClientCategory, MifidDisplayStatus, ServiceType } from '../core/api/models';

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  ADVISORY: 'Advisory',
  DISCRETIONARY: 'Discretionary',
  EXECUTION_ONLY: 'Execution only',
};

export const ASSET_CLASS_LABELS: Record<AssetClass | 'CASH', string> = {
  EQUITY: 'Equities',
  BOND: 'Bonds',
  FUND: 'Funds',
  ETF: 'ETFs',
  OPTION: 'Options',
  FUTURE: 'Futures',
  CASH: 'Cash',
};

export const CATEGORY_LABELS: Record<ClientCategory, string> = {
  RETAIL: 'Retail',
  PROFESSIONAL: 'Professional',
  ELIGIBLE_COUNTERPARTY: 'Eligible counterparty',
};

export type Tone = 'success' | 'warning' | 'danger' | 'neutral' | 'info';

export const MIFID_STATUS: Record<MifidDisplayStatus, { label: string; tone: Tone }> = {
  VALID: { label: 'Valid', tone: 'success' },
  EXPIRED: { label: 'Expired', tone: 'danger' },
  MISSING: { label: 'No profile', tone: 'danger' },
  PENDING_VALIDATION: { label: 'Awaiting client', tone: 'warning' },
  IN_PROGRESS: { label: 'Review in progress', tone: 'info' },
  SUPERSEDED: { label: 'Superseded', tone: 'neutral' },
  REFUSED: { label: 'Refused by client', tone: 'neutral' },
  CANCELLED: { label: 'Cancelled', tone: 'neutral' },
};

/** Colours for allocation slices; also differ in lightness so they stay distinguishable. */
export const ASSET_CLASS_COLORS: Record<AssetClass | 'CASH', string> = {
  EQUITY: '#0E5F5B',
  BOND: '#4F8FC0',
  FUND: '#C98A2E',
  ETF: '#7FC4BE',
  OPTION: '#7A5BA6',
  FUTURE: '#3D2E5C',
  CASH: '#9AA8B4',
};
