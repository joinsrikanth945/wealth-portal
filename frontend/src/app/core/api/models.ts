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
  clientsNeedingAttention: ClientListItem[];
}
