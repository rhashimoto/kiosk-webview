import { LitElement, css, html } from 'lit';
import { openDB} from 'idb';

import { withGAPI } from './gapi.js';

const POPULATE_THROTTLE = 1_000;
const CACHE_BATCH_SIZE = 50;
const DEFAULT_CACHE_NAME = 'mediaItems';
const LAST_SHUFFLE_KEY = 'last-shuffle-key';

const dbReady = openDB('photos', 1, {
  upgrade(db, oldVersion) {
    switch (oldVersion) {
      case 0:
        const store = db.createObjectStore('photos', { keyPath: 'id' });
        store.createIndex('by-date', 'mediaMetadata.creationTime');
        store.createIndex('shuffle', 'shuffleKey');
    }
  }
});

class AppPhotos extends LitElement {
  static get properties() {
    return {
      cacheName: { type: String }
    }
  }

  #isCachingInProgress = false;

  constructor() {
    super();
    this.cacheName = DEFAULT_CACHE_NAME;

    this.addEventListener('populate-photos', () => this.#populatePhotos());
    this.addEventListener('show-photo', () => this.#showPhoto());
  }

  firstUpdated() {
    this.#showPhoto();
  }

  async #populatePhotos() {
    // Get the timestamp of the most recent photo in IndexedDB.
    const db = await dbReady;
    const lastTimestamp = await (async () => {
      const store = db.transaction('photos', 'readonly').store;
      const index = store.index('by-date');
      const cursor = await index.openCursor(null, 'prev');
      return cursor?.value?.mediaMetadata?.creationTime || '1970-01-01T00:00Z';
    })();
    console.log(`lastTimestamp ${lastTimestamp}`);

    // Fetch photos from Google.
    let nAdded = 0;
    const lastDate = new Date(lastTimestamp);
    const searchParams = {
      pageSize: 100,
      filters: {
        dateFilter: {
          ranges: [
            {
              startDate: {
                year: lastDate.getUTCFullYear(),
                month: lastDate.getUTCMonth() + 1,
                day: lastDate.getUTCDate()
              },
              endDate: {
                year: 9999,
                month: 12,
                day: 31
              }
            }
          ]
        }
      },
      orderBy: 'MediaMetadata.creation_time',
    };
    do {
      if (searchParams.pageToken) {
        await new Promise(resolve => setTimeout(resolve, POPULATE_THROTTLE));
      }

      const photos = await withGAPI(async gapi => {
        const response = await gapi.client.photoslibrary.mediaItems.search(searchParams);
        searchParams.pageToken = response.result.nextPageToken;
        return response.result.mediaItems;
      });
      console.log(`received ${photos.length} photos`);

      const store = db.transaction('photos', 'readwrite').store;
      for (const photo of photos) {
        if (photo.mimeType?.startsWith('image/') &&
            photo.mediaMetadata?.creationTime > lastTimestamp) {
          // Remove properties we don't need to keep.
          const p = Object.fromEntries(Object.entries(photo).filter(([k, v]) => {
            return ['id', 'mimeType'].includes(k);
          }));
          p.mediaMetadata = Object.fromEntries(Object.entries(photo.mediaMetadata)
            .filter(([k, v]) => {
              return ['creationTime'].includes(k);
            }));

          // Add a randomly generated statistically unique shuffle key.
          p.shuffleKey = crypto.getRandomValues(new BigUint64Array(2))
            .reduce((result, value) => (result << 64n) + value)
            .toString(36);
    
          store.put(p);
          ++nAdded;
        }
      }
    } while (nAdded === 0 && searchParams.pageToken);

    const count = await db.transaction('photos', 'readonly').store.count();
    console.log(`IndexedDB contains ${count} photos`);
  }

  async #showPhoto() {
    const cache = await caches.open(this.cacheName);
    const cacheKeys = await cache.keys();
    if (cacheKeys[0]) {
      this.shadowRoot.querySelector('img').src = cacheKeys[0].url;
    }
    console.log(`cache contains ${cacheKeys.length} photos`);

    if (cacheKeys.length <= 1 && !this.#isCachingInProgress) {
      try {
        this.#isCachingInProgress = true;
        await this.#cacheImages(cache);
      } finally {
        this.#isCachingInProgress = false;
      }
    }
  }

  async #cacheImages(cache) {
    // Get the next batch of photos from IndexedDB, starting at the
    // last cached shuffle key.
    const db = await dbReady;
    const store = db.transaction('photos', 'readonly').store;
    const index = store.index('shuffle');
    const lastShuffleKey = localStorage.getItem(LAST_SHUFFLE_KEY) || '';
    const photos = await index.getAll(
      IDBKeyRange.lowerBound(lastShuffleKey, true),
      CACHE_BATCH_SIZE);
    if (photos.length < CACHE_BATCH_SIZE) {
      // The batch is not full so wrap around to the beginning of
      // the shuffle keys.
      const morePhotos = await index.getAll(
        IDBKeyRange.bound(Number.NEGATIVE_INFINITY, lastShuffleKey, false, true),
        CACHE_BATCH_SIZE - photos.length);
      photos.push(...morePhotos);
    }

    // Get fresh URLs for the photos in the batch.
    const mediaItemResults = await withGAPI(async gapi => {
      const response = await gapi.client.photoslibrary.mediaItems.batchGet({
        mediaItemIds: photos.map(photo => photo.id)
      });
      return response.result.mediaItemResults;
    });
    console.log(`received ${mediaItemResults.length} mediaItemResults`);

    // Add the photos to the cache.
    for (let i = 0; i < mediaItemResults.length; ++i) {
      const mediaItemResult = mediaItemResults[i];
      if (mediaItemResult.status?.code === 5) {
        // The media item was not found. Delete from IndexedDB.
        console.log(`deleting photo ${mediaItemResult.mediaItem.id}`);
        db.transaction('photos', 'readwrite').store
          .delete(mediaItemResult.mediaItem.id);
        continue;
      }

      // Cache the image file.
      const url = [
        mediaItemResult.mediaItem.baseUrl,
        `=w${this.clientWidth}-h${this.clientHeight}`
      ].join('');
      const response = await fetch(url, { mode: 'no-cors'});
      cache.put(`https://example.com?shuffleKey=${photos[i].shuffleKey}`, response);
      localStorage.setItem(LAST_SHUFFLE_KEY, photos[i].shuffleKey);
    }
  }

  static get styles() {
    return css`
      :host {
        background-color: black;

        display: flex;
        align-items: center;
        justify-content: center;
      }

      * {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
        flex-grow: 1;
      }
    `;
  }

  render() {
    return html`
      <img>
    `;
  }
}
customElements.define('app-photos', AppPhotos);