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
    const response = await mediaItemsCache.match(url);
    if (response) {
      // Remove the cache entry and return the response.
      mediaItemsCache.delete(url);
      return response;
    }
    return fetch(event.request);
  }));
});