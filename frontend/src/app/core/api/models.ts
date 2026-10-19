// TypeScript mirrors of the Spring Boot DTOs. Money arrives as JSON numbers,
// dates as ISO strings.

export type ServiceType = 'ADVISORY' | 'DISCRETIONARY' | 'EXECUTION_ONLY';
export type AssetClass = 'EQUITY' | 'BOND' | 'FUND' | 'ETF' | 'OPTION' | 'FUTURE';
export type ClientCategory = 'RETAIL' | 'PROFESSIONAL' | 'ELIGIBLE_COUNTERPARTY';
export type MifidStatus =
  'IN_PROGRESS' | 'PENDING_VALIDATION' | 'VALID' | 'SUPERSEDED' | 'REFUSED' | 'CANCELLED';
export type MifidDisplayStatus = MifidStatus | 'EXPIRED' | 'MISSING';
export type ClientDecision = 'UNDER_REVIEW' | 'ACCEPTED' | 'REFUSED';
export type NextAction = 'PROFILE_NOW' | 'REVIEW_NOW' | 'FINALIZE_REVIEW' | 'NONE';

export interface PortfolioSummary {
  id: number;
  portfolioNumber: string;
  name: string;
  serviceType: ServiceType;
  currency: string;
  totalValue: number;
  cash: number;
  cashRatioPercent: number;
}

export interface PositionLine {
  positionId: number;
  instrumentId: number;
  instrumentCode: string;
  instrumentName: string;
  assetClass: AssetClass;
  riskClass: number;
  complex: boolean;
  quantity: number;
  averageCost: number;
  lastPrice: number;
  priceDate: string;
  marketValue: number;
  costValue: number;
  unrealizedPnl: number;
  unrealizedPnlPercent: number;
  weightPercent: number;
}

export interface AllocationSlice {
  assetClass: AssetClass | 'CASH';
  value: number;
  weightPercent: number;
}

export interface HistoryPoint {
  asOf: string;
  indexValue: number;
}

export interface PortfolioDetail {
  id: number;
  portfolioNumber: string;
  name: string;
  serviceType: ServiceType;
  currency: string;
  openedOn: string;
  clientId: number;
  clientName: string;
  clientNumber: string;
  totalValue: number;
  investedValue: number;
  cash: number;
  cashRatioPercent: number;
  unrealizedPnl: number;
  performancePercent: number;
  allocation: AllocationSlice[];
  positions: PositionLine[];
  history: HistoryPoint[];
}

export interface ProfileSnapshot {
  id: number;
  status: MifidStatus;
  displayStatus: MifidDisplayStatus;
  riskScore: number | null;
  riskLabel: string | null;
  complexProductsOk: boolean;
  completionPercent: number;
  clientDecision: ClientDecision | null;
  validUntil: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  signed: boolean;
}

export interface MifidOverview {
  clientId: number;
  clientName: string;
  current: ProfileSnapshot | null;
  openReview: ProfileSnapshot | null;
  nextAction: NextAction;
  history: ProfileSnapshot[];
}

export interface QuestionOption {
  id: string;
  label: string;
  points: number;
}

export interface Question {
  id: string;
  text: string;
  help: string | null;
  scored: boolean;
  options: QuestionOption[];
}

export interface QuestionnaireSection {
  id: string;
  title: string;
  questions: Question[];
}

export interface Questionnaire {
  version: string;
  sections: QuestionnaireSection[];
}

export interface MifidScore {
  points: number;
  riskScore: number | null;
  riskLabel: string | null;
  complexProductsOk: boolean;
  answered: number;
  totalQuestions: number;
  completionPercent: number;
  missingQuestions: string[];
}

export interface ReviewDetail {
  profile: ProfileSnapshot;
  clientId: number;
  clientName: string;
  questionnaireVersion: string;
  answers: Record<string, string>;
  score: MifidScore;
}

export interface ClientListItem {
  id: number;
  clientNumber: string;
  fullName: string;
  category: ClientCategory;
  country: string;
  portfolioCount: number;
  totalValue: number;
  mifidStatus: MifidDisplayStatus;
  riskScore: number | null;
  riskLabel: string | null;
  mifidValidUntil: string | null;
  mifidNextAction: NextAction;
}

export interface ClientDetail {
  id: number;
  clientNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  birthDate: string | null;
  country: string;
  category: ClientCategory;
  rmUsername: string;
  totalValue: number;
  portfolios: PortfolioSummary[];
  mifid: MifidOverview;
}

export interface RmHome {
  totalAum: number;
  clients: number;
  portfolios: number;
  mifidPendingValidation: number;
  mifidMissingOrExpired: number;
  mifidReviewsInProgress: number;
  highCashPortfolios: number;
  proposalsPendingOver5Days: number;
  ordersAwaitingApproval: number;
  clientsNeedingAttention: ClientListItem[];
}

export interface AdminHome {
  ordersAwaitingApproval: number;
  clients: number;
  portfolios: number;
}

// ------------------------------------------------------------------ orders
export type Side = 'BUY' | 'SELL';
export type LimitType = 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
export type TimeInForce = 'DAY' | 'GTC' | 'GTD';
export type OrderStatus = 'PENDING_APPROVAL' | 'PLACED' | 'FILLED' | 'CANCELLED' | 'REJECTED';
export type CheckOutcome = 'PASS' | 'WARN' | 'FAIL';

export interface Instrument {
  id: number;
  code: string;
  name: string;
  assetClass: AssetClass;
  currency: string;
  exchange: string | null;
  riskClass: number;
  complex: boolean;
  lastPrice: number;
  priceDate: string;
  underlyingId: number | null;
  optionType: 'CALL' | 'PUT' | null;
  strike: number | null;
  expiry: string | null;
  contractSize: number | null;
}

export interface OrderRequest {
  portfolioId: number;
  instrumentId: number;
  side: Side;
  quantity: number;
  limitType: LimitType;
  limitPrice: number | null;
  stopPrice: number | null;
  timeInForce: TimeInForce;
  validUntil: string | null;
  negotiatedFee: number | null;
}

export interface PreTradeCheck {
  code: string;
  label: string;
  outcome: CheckOutcome;
  message: string | null;
}

export interface OrderPreview {
  instrumentId: number;
  instrumentName: string;
  assetClass: AssetClass;
  natureLabel: string;
  lastPrice: number;
  estimatedPrice: number;
  grossAmount: number;
  fees: number;
  standardFees: number;
  netAmount: number;
  cashAvailable: number;
  quantityHeld: number;
  checks: PreTradeCheck[];
  canSubmit: boolean;
  needsApproval: boolean;
}

export interface Order {
  id: number;
  portfolioId: number;
  portfolioNumber: string;
  portfolioName: string;
  clientId: number;
  clientName: string;
  instrumentId: number;
  instrumentCode: string;
  instrumentName: string;
  assetClass: AssetClass;
  side: Side;
  natureLabel: string;
  quantity: number;
  limitType: LimitType;
  limitPrice: number | null;
  stopPrice: number | null;
  timeInForce: TimeInForce;
  validUntil: string | null;
  status: OrderStatus;
  estimatedPrice: number;
  grossAmount: number;
  fees: number;
  netAmount: number;
  executedPrice: number | null;
  executedAt: string | null;
  checkSummary: string | null;
  proposalId: number | null;
  createdBy: string;
  createdAt: string;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionNote: string | null;
}

// --------------------------------------------------------------- proposals
export type ProposalStatus =
  'WORKING' | 'SENT_FOR_VALIDATION' | 'VALIDATED' | 'REJECTED' | 'EXECUTED';

export interface ProposalSummary {
  id: number;
  title: string;
  status: ProposalStatus;
  portfolioId: number;
  portfolioNumber: string;
  portfolioName: string;
  clientId: number;
  clientName: string;
  lineCount: number;
  estimatedBuys: number;
  estimatedSells: number;
  createdBy: string;
  createdAt: string;
  sentAt: string | null;
  pendingOver5Days: boolean;
}

export interface ProposalLine {
  id: number;
  lineNo: number;
  instrumentId: number;
  instrumentCode: string;
  instrumentName: string;
  assetClass: AssetClass;
  side: Side;
  quantity: number;
  lastPrice: number;
  estimatedAmount: number;
  estimatedFees: number;
  comment: string | null;
  checks: PreTradeCheck[];
}

export interface ProposalDetail {
  summary: ProposalSummary;
  rationale: string | null;
  decidedAt: string | null;
  decidedBy: string | null;
  rejectReason: string | null;
  signed: boolean;
  lines: ProposalLine[];
  hasBlockingChecks: boolean;
  hasWarnings: boolean;
  orderIds: number[];
}

export interface ProposalRequest {
  portfolioId: number;
  title: string;
  rationale: string;
  lines: { instrumentId: number; side: Side; quantity: number; comment: string | null }[];
}

// ----------------------------------------------------------------- reports
export type ReportType = 'STATEMENT' | 'VALUATION' | 'PERFORMANCE' | 'TRANSACTIONS' | 'COSTS';

export interface Report {
  id: number;
  portfolioId: number;
  type: ReportType;
  title: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  requestedBy: string;
  requestedAt: string;
  completedAt: string | null;
  fileName: string | null;
  error: string | null;
}

// ---------------------------------------------------------------- messages
export interface ThreadSummary {
  clientId: number;
  clientName: string;
  clientNumber: string;
  lastMessage: string | null;
  lastAt: string | null;
  unread: number;
}

export interface Message {
  id: number;
  fromClient: boolean;
  senderName: string;
  body: string;
  createdAt: string;
  readAt: string | null;
  mine: boolean;
}

export interface Thread {
  clientId: number;
  clientName: string;
  messages: Message[];
}
