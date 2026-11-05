import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ComplianceScanRequest, ComplianceScanResponse, ComplianceRule } from '../models/api.models';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class ComplianceService {
  constructor(private http: HttpClient) {}

  /** Runs the full compliance pipeline on the provided draft text. */
  scan(request: ComplianceScanRequest): Observable<ComplianceScanResponse> {
    return this.http.post<ComplianceScanResponse>(`${API_BASE}/compliance/scan`, request);
  }

  getRules(): Observable<ComplianceRule[]> {
    return this.http.get<ComplianceRule[]>(`${API_BASE}/compliance/rules`);
  }

  createRule(rule: ComplianceRule): Observable<ComplianceRule> {
    return this.http.post<ComplianceRule>(`${API_BASE}/compliance/rules`, rule);
  }

  deactivateRule(id: number): Observable<string> {
    return this.http.delete(`${API_BASE}/compliance/rules/${id}`, { responseType: 'text' });
  }
}
