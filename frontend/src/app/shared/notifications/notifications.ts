import { Component, inject, signal } from '@angular/core';
import { WebPushService } from '../../core/web-push.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications {
  private webPushService = inject(WebPushService);

  enabled = signal(false);
  loading = signal(false);
  error = signal<string | null>(null);

  enable(): void {
    this.loading.set(true);
    this.error.set(null);

    this.webPushService.subscribe().subscribe({
      next: () => {
        this.enabled.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron activar las notificaciones.');
        this.loading.set(false);
      },
    });
  }

  disable(): void {
    this.loading.set(true);
    this.error.set(null);

    this.webPushService.unsubscribe().subscribe({
      next: () => {
        this.enabled.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron desactivar las notificaciones.');
        this.loading.set(false);
      },
    });
  }
}
