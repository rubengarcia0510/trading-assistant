import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HistoryEntry, SetupsService } from '../core/setups.service';

type LoadState = 'idle' | 'loading' | 'loaded' | 'empty' | 'error';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './history.html',
  styleUrl: './history.scss',
})
export class History {
  private setupsService = inject(SetupsService);
  private router = inject(Router);

  private readonly pageSize = 20;

  // Filtros (combinables entre sí)
  symbolFilter = '';
  fromFilter = ''; // <input type="date"> devuelve "yyyy-MM-dd"
  toFilter = '';

  entries = signal<HistoryEntry[]>([]);
  currentPage = signal(0);
  totalElements = signal(0);
  state = signal<LoadState>('idle');

  hasMore(): boolean {
    return this.entries().length < this.totalElements();
  }

  ngOnInit(): void {
    this.search();
  }

  /** Nueva búsqueda desde cero (se llama al aplicar filtros). */
  search(): void {
    this.currentPage.set(0);
    this.fetchPage(true);
  }

  loadMore(): void {
    this.currentPage.update((p) => p + 1);
    this.fetchPage(false);
  }

  clearFilters(): void {
    this.symbolFilter = '';
    this.fromFilter = '';
    this.toFilter = '';
    this.search();
  }

  private fetchPage(replace: boolean): void {
    this.state.set('loading');

    this.setupsService
      .searchHistory({
        symbol: this.symbolFilter || undefined,
        from: this.fromFilter ? `${this.fromFilter}T00:00:00Z` : undefined,
        to: this.toFilter ? `${this.toFilter}T23:59:59Z` : undefined,
        page: this.currentPage(),
        size: this.pageSize,
      })
      .subscribe({
        next: (res) => {
          this.entries.set(replace ? res.content : [...this.entries(), ...res.content]);
          this.totalElements.set(res.totalElements);
          this.state.set(this.entries().length === 0 ? 'empty' : 'loaded');
        },
        error: () => this.state.set('error'),
      });
  }

  goBack(): void {
    this.router.navigate(['/setups']);
  }
}
