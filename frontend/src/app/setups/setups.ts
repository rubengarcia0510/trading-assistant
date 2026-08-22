import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Decision, ExplainedSetup, SetupsService } from '../core/setups.service';
import { Sparkline } from '../shared/sparkline';

@Component({
  selector: 'app-setups',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, Sparkline],
  templateUrl: './setups.html',
  styleUrl: './setups.scss',
})
export class Setups {
  private setupsService = inject(SetupsService);
  private authService = inject(AuthService);
  private router = inject(Router);

  pendingSetups = signal<ExplainedSetup[]>([]);
  decidedItems = signal<Decision[]>([]);
  lastScanAt = signal<string>('');
  loading = signal(false);
  scanning = signal(false);
  /** Símbolo que está en pleno traspaso visual de Pendientes -> Decididos, para animarlo. */
  leavingSymbol = signal<string | null>(null);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.setupsService.getLastSetups().subscribe({
      next: (res) => {
        this.lastScanAt.set(res.lastScanAt);
        this.setupsService.getDecisions().subscribe({
          next: (decisions) => {
            const decidedSymbols = new Set(decisions.map((d) => d.symbol));
            this.pendingSetups.set(res.setups.filter((s) => !decidedSymbols.has(s.setup.symbol)));
            this.decidedItems.set(
              [...decisions].sort((a, b) => (a.decidedAt < b.decidedAt ? 1 : -1))
            );
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

  /** Solo para ver el diseño de la card sin depender de una señal real del mercado. */
  testMock(): void {
    this.setupsService.testExplanation().subscribe((res) => {
      this.pendingSetups.update((list) => [res.explainedSetup, ...list]);
    });
  }

  decide(symbol: string, decision: 'approve' | 'discard'): void {
    this.setupsService.registerDecision(symbol, decision).subscribe(() => {
      // Signature element: la card se marca como "saliendo" (dispara la animación en CSS),
      // y recién después de que termine la transición se saca de Pendientes y se agrega a Decididos.
      this.leavingSymbol.set(symbol);

      setTimeout(() => {
        this.pendingSetups.update((list) => list.filter((s) => s.setup.symbol !== symbol));
        this.decidedItems.update((list) => [
          { symbol, decision, decidedAt: new Date().toISOString(), source: 'web' },
          ...list,
        ]);
        this.leavingSymbol.set(null);
      }, 350);
    });
  }

  isLeaving(symbol: string): boolean {
    return this.leavingSymbol() === symbol;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
