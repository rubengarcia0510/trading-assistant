self.addEventListener('push', event => {
  if (!event.data) return;

  let data;
  try {
    data = event.data.json();
  } catch {
    data = { title: 'Trading Assistant', body: event.data.text() };
  }

  event.waitUntil(
    self.registration.showNotification(data.title || 'Trading Assistant', {
      body: data.body || '',
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      tag: data.symbol ? `tai-setup-${data.symbol}` : 'tai-setup',
      renotify: true,
      data: {
        url: data.url || '/setups',
        symbol: data.symbol || null,
      },
    })
  );
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  const targetUrl = event.notification.data?.url || '/setups';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windowClients => {
      const existing = windowClients.find(client => 'focus' in client);
      if (existing) {
        return existing.navigate(targetUrl).then(client => client.focus());
      }
      return clients.openWindow(targetUrl);
    })
  );
});
