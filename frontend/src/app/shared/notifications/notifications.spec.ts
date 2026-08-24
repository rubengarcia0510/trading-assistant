import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Notifications } from './notifications';
import { WebPushService } from '../../core/web-push.service';

describe('Notifications', () => {
  let fixture: ComponentFixture<Notifications>;
  let component: Notifications;
  let webPushService: {
    subscribe: ReturnType<typeof vi.fn>;
    unsubscribe: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    webPushService = {
      subscribe: vi.fn(),
      unsubscribe: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Notifications],
      providers: [
        {
          provide: WebPushService,
          useValue: webPushService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Notifications);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should enable notifications when subscription succeeds', () => {
    webPushService.subscribe.mockReturnValue(of({} as PushSubscription));

    component.enable();

    expect(webPushService.subscribe).toHaveBeenCalled();
    expect(component.enabled()).toBe(true);
    expect(component.loading()).toBe(false);
    expect(component.error()).toBeNull();
  });

  it('should report an error when enabling fails', () => {
    webPushService.subscribe.mockReturnValue(
      throwError(() => new Error('subscribe failed')),
    );

    component.enable();

    expect(component.enabled()).toBe(false);
    expect(component.loading()).toBe(false);
    expect(component.error()).toBe(
      'No se pudieron activar las notificaciones.',
    );
  });

  it('should disable notifications when unsubscribe succeeds', () => {
    component.enabled.set(true);
    webPushService.unsubscribe.mockReturnValue(of(void 0));

    component.disable();

    expect(webPushService.unsubscribe).toHaveBeenCalled();
    expect(component.enabled()).toBe(false);
    expect(component.loading()).toBe(false);
    expect(component.error()).toBeNull();
  });

  it('should report an error when disabling fails', () => {
    component.enabled.set(true);
    webPushService.unsubscribe.mockReturnValue(
      throwError(() => new Error('unsubscribe failed')),
    );

    component.disable();

    expect(component.enabled()).toBe(true);
    expect(component.loading()).toBe(false);
    expect(component.error()).toBe(
      'No se pudieron desactivar las notificaciones.',
    );
  });
});
