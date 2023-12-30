// https://developer.android.com/develop/ui/views/layout/webapps/load-local-content#assetloader
const ANDROID_APP = 'https://appassets.androidplatform.net';

export const isAndroidApp = (function() {
  const hasAssetLoaderLocation = window.location.href.startsWith(ANDROID_APP);
  return () => hasAssetLoaderLocation;
})();

export async function getAndroidResource(name) {
  const response = await fetch(`${ANDROID_APP}/x/${name}`);
  return response.text();
}

export async function getAccessToken() {
  return getAndroidResource('accessToken');
}
