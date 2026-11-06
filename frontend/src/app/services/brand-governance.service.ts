import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SubmissionDto,
  SubmissionComment,
  BrandGuideline,
  BrandTerm,
  BrandFactClaim,
  GovernanceInsights,
  Page
} from '../models/api.models';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class BrandGovernanceService {
  constructor(private http: HttpClient) {}

  // Submissions & Drafts
  saveDraft(data: Partial<SubmissionDto>): Observable<SubmissionDto> {
    return this.http.post<SubmissionDto>(`${API_BASE}/submissions/draft`, data);
  }

  submitForReview(id: number, creatorNote?: string): Observable<SubmissionDto> {
    return this.http.post<SubmissionDto>(`${API_BASE}/submissions/${id}/submit`, { creatorNote });
  }

  getMyDrafts(): Observable<SubmissionDto[]> {
    return this.http.get<SubmissionDto[]>(`${API_BASE}/submissions/my-drafts`);
  }

  getSubmission(id: number): Observable<SubmissionDto> {
    return this.http.get<SubmissionDto>(`${API_BASE}/submissions/${id}`);
  }

  getPendingSubmissions(page = 0, size = 10): Observable<Page<SubmissionDto>> {
    return this.http.get<Page<SubmissionDto>>(`${API_BASE}/submissions/review/pending`, { params: { page, size } });
  }

  getAllSubmissions(page = 0, size = 10): Observable<Page<SubmissionDto>> {
    return this.http.get<Page<SubmissionDto>>(`${API_BASE}/submissions/review/all`, { params: { page, size } });
  }

  submitDecision(id: number, decision: string, comments: string): Observable<SubmissionDto> {
    return this.http.post<SubmissionDto>(`${API_BASE}/submissions/${id}/decision`, { decision, comments });
  }

  // Comments / Threaded discussions
  getComments(submissionId: number): Observable<SubmissionComment[]> {
    return this.http.get<SubmissionComment[]>(`${API_BASE}/submissions/${submissionId}/comments`);
  }

  addComment(submissionId: number, message: string): Observable<SubmissionComment> {
    return this.http.post<SubmissionComment>(`${API_BASE}/submissions/${submissionId}/comments`, { message });
  }

  // Guidelines Builder & Management
  getActiveGuideline(): Observable<BrandGuideline> {
    return this.http.get<BrandGuideline>(`${API_BASE}/guidelines/active`);
  }

  getGuidelineHistory(): Observable<BrandGuideline[]> {
    return this.http.get<BrandGuideline[]>(`${API_BASE}/guidelines/history`);
  }

  saveGuideline(guideline: BrandGuideline): Observable<BrandGuideline> {
    return this.http.post<BrandGuideline>(`${API_BASE}/guidelines`, guideline);
  }

  getTerms(): Observable<BrandTerm[]> {
    return this.http.get<BrandTerm[]>(`${API_BASE}/guidelines/terms`);
  }

  createTerm(term: BrandTerm): Observable<BrandTerm> {
    return this.http.post<BrandTerm>(`${API_BASE}/guidelines/terms`, term);
  }

  deactivateTerm(id: number): Observable<any> {
    return this.http.delete(`${API_BASE}/guidelines/terms/${id}`, { responseType: 'text' });
  }

  getFacts(): Observable<BrandFactClaim[]> {
    return this.http.get<BrandFactClaim[]>(`${API_BASE}/guidelines/facts`);
  }

  createFact(fact: BrandFactClaim): Observable<BrandFactClaim> {
    return this.http.post<BrandFactClaim>(`${API_BASE}/guidelines/facts`, fact);
  }

  deactivateFact(id: number): Observable<any> {
    return this.http.delete(`${API_BASE}/guidelines/facts/${id}`, { responseType: 'text' });
  }

  // Governance Insights
  getInsights(): Observable<GovernanceInsights> {
    return this.http.get<GovernanceInsights>(`${API_BASE}/governance/insights`);
  }
}
