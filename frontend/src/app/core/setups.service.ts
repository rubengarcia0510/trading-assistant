import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api-config';

export interface Setup {
  symbol: string;
  assetType: 'STOCK' | 'CRYPTO';
  detectedAt: string;
  entryPrice: number;
  stopLoss: number;
  takeProfit: number;
  riskLevel: 'BAJO' | 'MEDIO' | 'ALTO';
  signalDescription: string;
  recentCloses: number[];
}

export interface ExplainedSetup {
  setup: Setup;
  explanation: string;
}

interface SetupsResponse {
  lastScanAt: string;
  count: number;
  setups: ExplainedSetup[];
}

export interface Decision {
  symbol: string;
  decision: 'approve' | 'discard';
  decidedAt: string;
  source: 'telegram' | 'web';
}

@Injectable({ providedIn: 'root' })
export class SetupsService {
  private http = inject(HttpClient);

  getLastSetups(): Observable<SetupsResponse> {
    return this.http.get<SetupsResponse>(`${API_URL}/setups`);
  }

  scanNow(): Observable<ExplainedSetup[]> {
    return this.http.post<ExplainedSetup[]>(`${API_URL}/setups/scan`, {});
  }

  registerDecision(symbol: string, decision: 'approve' | 'discard'): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${API_URL}/setups/decision`, { symbol, decision });
  }

  getDecisions(): Observable<Decision[]> {
    return this.http.get<Decision[]>(`${API_URL}/telegram/decisions`);
  }

  testExplanation(): Observable<{ explainedSetup: ExplainedSetup }> {
    return this.http.get<{ explainedSetup: ExplainedSetup }>(`${API_URL}/setups/test-explanation`);
  }
}
