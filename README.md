# TAI-13 frontend delta

Aplicar sobre `feature/TAI-13-web-push`.

Archivos nuevos:
- `frontend/public/sw.js`
- `frontend/src/app/core/web-push.service.ts`

El servicio reutiliza `API_URL` y el interceptor JWT existente. El backend Web Push no se modifica.

Para completar la integración UI, inyectar `WebPushService` en `Setups` y agregar controles explícitos de activar/desactivar notificaciones. No solicitar permiso automáticamente al cargar la aplicación.
