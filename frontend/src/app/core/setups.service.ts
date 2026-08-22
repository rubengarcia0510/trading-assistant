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

export interface HistoryEntry {
  id: string;
  symbol: string;
  assetType: 'STOCK' | 'CRYPTO';
  entryPrice: number;
  stopLoss: number;
  takeProfit: number;
  riskLevel: 'BAJO' | 'MEDIO' | 'ALTO';
  explanation: string;
  detectedAt: string | null;
  decision: 'approve' | 'discard';
  decidedAt: string;
  source: 'web' | 'telegram';
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

export interface HistoryFilters {
  symbol?: string;
  from?: string; // ISO datetime
  to?: string;   // ISO datetime
  page?: number;
  size?: number;
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

  testExplanation(): Observable<{ explainedSetup: ExplainedSetup }> {
    return this.http.get<{ explainedSetup: ExplainedSetup }>(`${API_URL}/setups/test-explanation`);
  }

  /** Últimas decisiones, sin filtros — para el resumen del dashboard (TAI-14). */
  getRecentHistory(size = 20): Observable<PageResult<HistoryEntry>> {
    return this.http.get<PageResult<HistoryEntry>>(`${API_URL}/history`, { params: { size } });
  }

  /** Búsqueda con filtros combinables — la vista de historial completa (TAI-15). */
  searchHistory(filters: HistoryFilters): Observable<PageResult<HistoryEntry>> {
    let params: Record<string, string | number> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };
    if (filters.symbol) params['symbol'] = filters.symbol;
    if (filters.from) params['from'] = filters.from;
    if (filters.to) params['to'] = filters.to;

    return this.http.get<PageResult<HistoryEntry>>(`${API_URL}/history`, { params });
  }
}
