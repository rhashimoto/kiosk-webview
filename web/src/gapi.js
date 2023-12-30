import { isAndroidApp, getAccessToken } from "./android.js";

const {
  GOOGLE_DISCOVERY_DOCS,
  GOOGLE_GAPI_LIBRARIES
} = JSON.parse(document.getElementById('google-config').textContent);

const GAPI_URL = 'https://apis.google.com/js/api.js';
const GIS_URL = 'https://accounts.google.com/gsi/client';

export const withGAPI = (function() {
  const ready = loadScript(GAPI_URL).then(async () => {
    // Initialize GAPI client.
    await new Promise((callback, onerror) => {
      gapi.load(GOOGLE_GAPI_LIBRARIES, { callback, onerror });
    });
    await gapi.client.init({});
    await Promise.all(GOOGLE_DISCOVERY_DOCS.map(discoveryDoc => {
      return gapi.client.load(discoveryDoc);
    }));

    // In the Android app, access tokens are provided by the app
    // asset loader.
    if (isAndroidApp()) return getAccessToken;

    // Outside the app, use Google Identity Services implicit flow.
    // https://developers.google.com/identity/oauth2/web/guides/migration-to-gis#implicit_flow_examples
    const [ config ] = await Promise.all([
      fetch('/test.json').then(response => response.json()),
      loadScript(GIS_URL)
    ]);
    const tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: config.webClientId,
      scope: config.scopes.join(' '),
      prompt: '',
      callback: ''
    });

    return () => new Promise((resolve, reject) => {
      try {
        tokenClient.callback = response => {
          if (response.error) {
            reject(response.error);
          } else {
            resolve(response.access_token);
            console.log('access token received');
          }
        };
        tokenClient.requestAccessToken();
      } catch (e) {
        // Handle errors that are not authorization errors.
        reject(e);
      }
    });
  });

  return async function withGAPI(f) {
    const getToken = await ready;
    for (let i = 0; i < 2; ++i) {
      try {
        return await f(gapi);
      } catch (e) {
        // If the first try fails with an authorization error, get a
        // new token and try again.
        if (!i && needsAuthorization(e)) {
          const token = await getToken();
          gapi.auth.setToken({ access_token: token });
          continue;
        }
        throw e;
      }
    }
  }
})();

async function loadScript(url) {
  const script = document.createElement('script');
  script.src = url;
  document.head.appendChild(script);

  await new Promise(resolve => {
    script.addEventListener('load', resolve);
  });
}

function needsAuthorization(e) {
  return [401, 403].includes(e.result?.error?.code);
}