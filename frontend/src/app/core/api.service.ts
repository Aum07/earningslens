import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ChatRequest,
  ChatResponse,
  Company,
  CorpusDocument,
  KeyFigures,
  LatestEvalReport,
  LiveConfig,
} from './models';

/**
 * The only place that knows the backend URLs (spec section 10).
 * @Injectable({ providedIn: 'root' }): Angular creates one shared instance and hands it to whoever asks for it
 * with inject(ApiService) (dependency injection, like Spring beans).
 * Each method returns an Observable: nothing is sent until someone subscribes. Errors arrive as ApiError.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  /** POST /api/v1/chat. Paid: one LLM call per question unless retrieval abstains. */
  chat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.base}/chat`, request);
  }

  /** GET /api/v1/documents: every document with its chunk count. */
  documents(): Observable<CorpusDocument[]> {
    return this.http.get<CorpusDocument[]>(`${this.base}/documents`);
  }

  /** GET /api/v1/companies: companies and their quarters, for the filters. */
  companies(): Observable<Company[]> {
    return this.http.get<Company[]>(`${this.base}/companies`);
  }

  /** GET /api/v1/eval/latest: latest.json unchanged. ApiError kind 'not-found' if no report exists yet. */
  latestEval(): Observable<LatestEvalReport> {
    return this.http.get<LatestEvalReport>(`${this.base}/eval/latest`);
  }

  /** GET /api/v1/eval/live-config: the ablation config (A to D) and models the chat API uses right now. */
  liveConfig(): Observable<LiveConfig> {
    return this.http.get<LiveConfig>(`${this.base}/eval/live-config`);
  }

  /** GET /api/v1/key-figures: official revenue, net income and EPS per corpus quarter, from SEC EDGAR. */
  keyFigures(): Observable<KeyFigures[]> {
    return this.http.get<KeyFigures[]>(`${this.base}/key-figures`);
  }
}
