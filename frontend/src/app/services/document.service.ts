import { Injectable } from '@angular/core';
import { HttpClient, HttpRequest, HttpEventType, HttpResponse } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { DocumentChunk, DocumentSummary } from '../models/api.models';

const API_BASE = 'http://localhost:8080/api';

export interface UploadResult {
  filename: string;
  documentType: string;
  chunksCreated: number;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  constructor(private http: HttpClient) {}

  /**
   * Uploads a PDF file with real-time progress tracking.
   * Returns two observables: upload % progress and final result.
   */
  uploadPdf(file: File, documentType: string): {
    progress$: Observable<number>;
    result$: Observable<UploadResult>;
  } {
    const progress$ = new Subject<number>();
    const result$   = new Subject<UploadResult>();

    const form = new FormData();
    form.append('file', file);
    form.append('documentType', documentType);

    const req = new HttpRequest('POST', `${API_BASE}/documents/upload`, form, {
      reportProgress: true
    });

    this.http.request(req).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          progress$.next(Math.round(100 * event.loaded / event.total));
        } else if (event instanceof HttpResponse) {
          result$.next(event.body as UploadResult);
          result$.complete();
          progress$.complete();
        }
      },
      error: err => { result$.error(err); progress$.error(err); }
    });

    return { progress$: progress$.asObservable(), result$: result$.asObservable() };
  }

  listDocuments(): Observable<DocumentSummary[]> {
    return this.http.get<DocumentSummary[]>(`${API_BASE}/documents`);
  }

  getChunks(documentName: string): Observable<DocumentChunk[]> {
    return this.http.get<DocumentChunk[]>(`${API_BASE}/documents/${encodeURIComponent(documentName)}/chunks`);
  }

  searchChunks(keyword: string): Observable<DocumentChunk[]> {
    return this.http.get<DocumentChunk[]>(`${API_BASE}/documents/search`, {
      params: { keyword }
    });
  }

  deleteDocument(documentName: string): Observable<string> {
    return this.http.delete(`${API_BASE}/documents/${encodeURIComponent(documentName)}`, {
      responseType: 'text'
    });
  }
}
