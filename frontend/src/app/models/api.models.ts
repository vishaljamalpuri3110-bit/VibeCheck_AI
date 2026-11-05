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
  confidenceScore: number;
  rationale: string;
  suggestedCorrection?: string;
  evidenceIds: EvidenceReference[];
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
