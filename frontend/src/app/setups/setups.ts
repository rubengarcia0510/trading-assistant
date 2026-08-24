import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ExplainedSetup, HistoryEntry, SetupsService } from '../core/setups.service';
import { Sparkline } from '../shared/sparkline';
import { Notifications } from '../shared/notifications/notifications';

@Component({
  selector: 'app-setups',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, Sparkline, Notifications],
  templateUrl: './setups.html',
  styleUrl: './setups.scss',
})
export class Setups {
  private setupsService = inject(SetupsService);
  private authService = inject(AuthService);
  private router = inject(Router);

  pendingSetups = signal<ExplainedSetup[]>([]);
  decidedItems = signal<HistoryEntry[]>([]);
  lastScanAt = signal<string>('');
  loading = signal(false);
  scanning = signal(false);
  leavingSymbol = signal<string | null>(null);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.setupsService.getLastSetups().subscribe({
      next: (res) => {
        this.lastScanAt.set(res.lastScanAt);
        this.setupsService.getRecentHistory(20).subscribe({
          next: (page) => {
            const decidedSymbols = new Set(page.content.map((d) => d.symbol));
            this.pendingSetups.set(res.setups.filter((s) => !decidedSymbols.has(s.setup.symbol)));
            this.decidedItems.set(page.content);
            this.loading.set(false);
          },
          error: () => {
            this.pendingSetups.set(res.setups);
            this.loading.set(false);
          },
        });
      },
      error: () => this.loading.set(false),
    });
  }

  scanNow(): void {
    this.scanning.set(true);
    this.setupsService.scanNow().subscribe({
      next: () => {
        this.scanning.set(false);
        this.loadAll();
      },
      error: () => this.scanning.set(false),
    });
  }

  testMock(): void {
    this.setupsService.testExplanation().subscribe((res) => {
      this.pendingSetups.update((list) => [res.explainedSetup, ...list]);
    });
  }

  decide(symbol: string, decision: 'approve' | 'discard'): void {
    this.setupsService.registerDecision(symbol, decision).subscribe(() => {
      this.leavingSymbol.set(symbol);

      setTimeout(() => {
        this.pendingSetups.update((list) => list.filter((s) => s.setup.symbol !== symbol));
        // Recargamos el historial real en vez de simularlo a mano — así queda
        // consistente con lo que realmente se guardó en Mongo.
        this.setupsService.getRecentHistory(20).subscribe((page) => this.decidedItems.set(page.content));
        this.leavingSymbol.set(null);
      }, 350);
    });
  }

  isLeaving(symbol: string): boolean {
    return this.leavingSymbol() === symbol;
  }

  goToHistory(): void {
    this.router.navigate(['/history']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
