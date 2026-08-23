import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, from, map, of, switchMap, tap, throwError } from 'rxjs';
import { API_URL } from './api-config';

interface PublicKeyResponse {
  publicKey: string;
}

interface PushSubscriptionPayload {
  endpoint: string;
  keys: {
    p256dh: string;
    auth: string;
  };
}

@Injectable({ providedIn: 'root' })
export class WebPushService {
  private readonly http = inject(HttpClient);
  private readonly serviceWorkerPath = '/sw.js';

  isSupported(): boolean {
    return typeof window !== 'undefined'
      && 'Notification' in window
      && 'serviceWorker' in navigator
      && 'PushManager' in window;
  }

  permission(): NotificationPermission | 'unsupported' {
    return this.isSupported() ? Notification.permission : 'unsupported';
  }

  getSubscription(): Promise<PushSubscription | null> {
    if (!this.isSupported()) return Promise.resolve(null);
    return navigator.serviceWorker.ready.then(registration =>
      registration.pushManager.getSubscription()
    );
  }

  subscribe(): Observable<PushSubscription> {
    if (!this.isSupported()) {
      return throwError(() => new Error('Web Push no está soportado por este navegador.'));
    }

    if (Notification.permission === 'denied') {
      return throwError(() => new Error('Las notificaciones están bloqueadas por el navegador.'));
    }

    return from(Notification.requestPermission()).pipe(
      switchMap(permission => {
        if (permission !== 'granted') {
          return throwError(() => new Error('Permiso de notificaciones no concedido.'));
        }
        return from(navigator.serviceWorker.register(this.serviceWorkerPath));
      }),
      switchMap(() => from(navigator.serviceWorker.ready)),
      switchMap(registration => from(registration.pushManager.getSubscription())),
      switchMap(existing => {
        if (existing) return of(existing);
        return this.http.get<PublicKeyResponse>(`${API_URL}/notification/push/public-key`).pipe(
          switchMap(response => from(navigator.serviceWorker.ready).pipe(
            switchMap(registration => registration.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey: urlBase64ToUint8Array(response.publicKey),
            }))
          ))
        );
      }),
      switchMap(subscription => this.registerSubscription(subscription).pipe(map(() => subscription)))
    );
  }

  unsubscribe(): Observable<void> {
    if (!this.isSupported()) return of(void 0);

    return from(this.getSubscription()).pipe(
      switchMap(subscription => {
        if (!subscription) return of(void 0);
        return this.http.delete(`${API_URL}/notification/push/subscribe`, {
          params: { endpoint: subscription.endpoint },
        }).pipe(
          switchMap(() => from(subscription.unsubscribe())),
          map(() => void 0)
        );
      })
    );
  }

  private registerSubscription(subscription: PushSubscription): Observable<void> {
    const json = subscription.toJSON();
    const payload: PushSubscriptionPayload = {
      endpoint: subscription.endpoint,
      keys: {
        p256dh: json.keys?.['p256dh'] || '',
        auth: json.keys?.['auth'] || '',
      },
    };

    if (!payload.keys.p256dh || !payload.keys.auth) {
      return throwError(() => new Error('PushSubscription no contiene las claves requeridas.'));
    }

    return this.http.post<void>(`${API_URL}/notification/push/subscribe`, payload);
  }
}

function urlBase64ToUint8Array(
  base64String: string
): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat(
    (4 - (base64String.length % 4)) % 4
  );

  const base64 = (base64String + padding)
    .replace(/-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const bytes = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; i++) {
    bytes[i] = rawData.charCodeAt(i);
  }

  return bytes;
}
