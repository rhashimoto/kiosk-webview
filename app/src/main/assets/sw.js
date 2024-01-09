const mediaItemsCacheReady = caches.open('mediaItems');

globalThis.addEventListener('install', event => {
  event.waitUntil(globalThis.skipWaiting());
});

globalThis.addEventListener('activate', event => {
  event.waitUntil(globalThis.clients.claim());
});

globalThis.addEventListener('fetch', event => {
  const url = event.request.url;
  event.respondWith(mediaItemsCacheReady.then(async mediaItemsCache => {
    // Check for a cached mediaItem.
    try {
      const response = await mediaItemsCache.match(url);
      if (response) return response;
    } catch (e) {
      console.error(e.message);
    } finally {
      mediaItemsCache.delete(url);
    }
    return fetch(event.request);
  }));
});