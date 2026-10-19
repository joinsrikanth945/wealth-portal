import { ProposalStatus } from '../../core/api/models';

export const PROPOSAL_STATUS: Record<ProposalStatus, { label: string; tone: string }> = {
  WORKING: { label: 'Draft', tone: 'neutral' },
  SENT_FOR_VALIDATION: { label: 'Awaiting client', tone: 'warning' },
  VALIDATED: { label: 'Accepted', tone: 'success' },
  REJECTED: { label: 'Declined', tone: 'danger' },
  EXECUTED: { label: 'Executed', tone: 'info' },
};
