const CACHE_NAME = 'sabtehazineh-v17';
const APP_SHELL = [
  '/sabtehazineha/',
  '/sabtehazineha/index.html',
  '/sabtehazineha/app.css',
  '/sabtehazineha/app.js',
  '/sabtehazineha/manifest.json',
  '/sabtehazineha/icon-192.png',
  '/sabtehazineha/icon-512.png',
  '/sabtehazineha/icon-192-mask.png',
  '/sabtehazineha/icon-512-mask.png',
  '/sabtehazineha/apple-touch-icon.png',
  '/sabtehazineha/favicon-32.png',
  '/sabtehazineha/favicon-48.png',
  '/sabtehazineha/favicon.ico'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;

  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) return cachedResponse;

      return fetch(event.request).then((networkResponse) => {
        if (!networkResponse || networkResponse.status !== 200) return networkResponse;
        const copy = networkResponse.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        return networkResponse;
      });
    }).catch(() => {
      if (event.request.mode === 'navigate') {
        return caches.match('/sabtehazineha/index.html');
      }
      return Response.error();
    })
  );
});
