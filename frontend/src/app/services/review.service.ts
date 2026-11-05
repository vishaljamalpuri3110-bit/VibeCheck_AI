import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DraftReviewLog, Page, ReviewStats } from '../models/api.models';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  constructor(private http: HttpClient) {}

  getPendingReviews(page = 0, size = 10): Observable<Page<DraftReviewLog>> {
    return this.http.get<Page<DraftReviewLog>>(
      `${API_BASE}/review/pending`, { params: { page, size } });
  }

  getAllReviews(page = 0, size = 10): Observable<Page<DraftReviewLog>> {
    return this.http.get<Page<DraftReviewLog>>(
      `${API_BASE}/review/all`, { params: { page, size } });
  }

  getReview(id: number): Observable<DraftReviewLog> {
    return this.http.get<DraftReviewLog>(`${API_BASE}/review/${id}`);
  }

  submitDecision(id: number, decision: string, comments: string): Observable<DraftReviewLog> {
    return this.http.post<DraftReviewLog>(`${API_BASE}/review/${id}/decision`, {
      decision, comments
    });
  }

  getStats(): Observable<ReviewStats> {
    return this.http.get<ReviewStats>(`${API_BASE}/review/stats`);
  }
}
