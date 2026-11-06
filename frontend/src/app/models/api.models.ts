/** VibeCheck AI — API data models matching the Spring Boot DTOs exactly */

export interface AuthRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  roles?: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  roles: string;
  expiresInMs: number;
}

export interface DocumentSummary {
  documentName: string;
  documentType: string;
  totalChunks: number;
}

export interface DocumentChunk {
  id: number;
  documentName: string;
  documentType: string;
  pageNumber: number;
  chunkIndex: number;
  content: string;
  contentLength: number;
  ingestedAt: string;
}

export interface ComplianceScanRequest {
  draftText: string;
  draftTitle?: string;
  contentType?: string;
  submissionId?: number;
}

export type VerificationState = 'SUPPORTED' | 'CONTRADICTED' | 'UNVERIFIED';
export type ReviewStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'NEEDS_REVISION';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface EvidenceReference {
  chunkId: number;
  documentName: string;
  pageNumber: number;
  excerpt: string;
}

export interface VerificationItem {
  claim: string;
  state: VerificationState;
  severity?: 'CRITICAL' | 'WARNING' | 'SUGGESTION';
  guidelineReference?: string;
  findingType?: string;
  confidenceScore: number;
  rationale: string;
  suggestedCorrection?: string;
  evidenceIds: EvidenceReference[];
  correctionValidationState:
  | 'SAFE'
  | 'NEEDS_REVIEW'
  | 'NOT_APPLICABLE';
  correctionValidationRationale?: string;
}

export interface LocalViolation {
  ruleId: number;
  ruleName: string;
  matchedText: string;
  severity: Severity;
  description: string;
}

export interface ComplianceScanResponse {
  draftReviewLogId: number;
  overallState: VerificationState;
  verificationItems: VerificationItem[];
  localViolations: LocalViolation[];
  supportedCount: number;
  contradictedCount: number;
  unverifiedCount: number;
  localViolationCount: number;
  piiDetected: boolean;
  voiceScore?: number;
  voiceFeedback?: string;
  aiDisclosure?: string;
  scannedAt: string;
}

export interface DraftReviewLog {
  id: number;
  submittedByUserId: number;
  submittedByUsername: string;
  originalDraftText: string;
  maskedDraftText: string;
  overallState: VerificationState;
  supportedCount: number;
  contradictedCount: number;
  unverifiedCount: number;
  localViolationCount: number;
  reviewStatus: ReviewStatus;
  reviewedByManagerId?: number;
  managerComments?: string;
  submittedAt: string;
  reviewedAt?: string;
}

export type ContentType = 'GENERAL' | 'SOCIAL_POST' | 'AD_COPY' | 'EMAIL' | 'BLOG_ARTICLE' | 'PRESS_RELEASE';
export type SubmissionStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'NEEDS_REVISION';

export interface SubmissionDto {
  id: number;
  title: string;
  contentType: ContentType;
  creatorUserId: number;
  creatorUsername: string;
  content: string;
  status: SubmissionStatus;
  latestDraftReviewLogId?: number;
  overallState?: VerificationState;
  voiceScore?: number;
  voiceFeedback?: string;
  creatorNote?: string;
  reviewedByUserId?: number;
  reviewedByUsername?: string;
  managerComments?: string;
  createdAt: string;
  updatedAt: string;
  submittedAt?: string;
  reviewedAt?: string;
  commentCount: number;
}

export interface SubmissionComment {
  id: number;
  submissionId: number;
  authorUserId: number;
  authorUsername: string;
  authorRole: string;
  message: string;
  createdAt: string;
}

export interface BrandGuideline {
  id?: number;
  brandName: string;
  version: string;
  missionStatement?: string;
  targetAudience?: string;
  toneOfVoice?: string;
  primaryColors?: string;
  competitorWatchlist?: string;
  guidelineContent?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface BrandTerm {
  id?: number;
  term: string;
  prohibited: boolean;
  approvedAlternative?: string;
  category?: string;
  rationale?: string;
  active: boolean;
  createdAt?: string;
}

export interface BrandFactClaim {
  id?: number;
  topic: string;
  verifiedClaim: string;
  sourceReference?: string;
  category?: string;
  active: boolean;
  createdAt?: string;
}

export interface GovernanceInsights {
  totalScans: number;
  totalSubmissions: number;
  approvedSubmissions: number;
  rejectedSubmissions: number;
  pendingSubmissions: number;
  overallComplianceRate: number;
  topBrokenRules: { ruleName: string; severity: string; count: number }[];
  severityBreakdown: { severity: string; count: number }[];
  creatorLeaderboard: { username: string; totalSubmissions: number; approvedCount: number; complianceRate: number }[];
  contentTypeDistribution: Record<string, number>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ReviewStats {
  pending: number;
  approved: number;
  rejected: number;
  needsRevision: number;
}

export interface ComplianceRule {
  id?: number;
  ruleName: string;
  ruleType: 'BANNED_KEYWORD' | 'REGEX_PATTERN';
  pattern: string;
  severity: Severity;
  description: string;
  active: boolean;
}

