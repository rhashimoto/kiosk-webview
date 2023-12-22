package com.shoestringresearch.kiosk.webview

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher
import java.security.MessageDigest
import java.security.SecureRandom

class AuthActivity: AppCompatActivity() {

    companion object {
        val SHARED_PREFERENCES_NAME = "AUTH_STATE_PREFERENCE"
        val AUTH_STATE = "AUTH_STATE"

        val SCOPE_PROFILE = "profile"
        val SCOPE_EMAIL = "email"
        val SCOPE_OPENID = "openid"
//        val SCOPE_DRIVE = "https://www.googleapis.com/auth/drive"

        val CLIENT_ID = "104957196093-r9cv6898ispjkh19ne2g2sq4163p45uc.apps.googleusercontent.com"
        val CODE_VERIFIER_CHALLENGE_METHOD = "S256"
        val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

        val URL_AUTHORIZATION = "https://accounts.google.com/o/oauth2/v2/auth"
        val URL_TOKEN_EXCHANGE = "https://www.googleapis.com/oauth2/v4/token"
        val URL_AUTH_REDIRECT = "com.shoestringresearch.kiosk.webview:/oauth2redirect"
        val URL_LOGOUT = "https://accounts.google.com/o/oauth2/revoke?token="

        private lateinit var authService: AuthorizationService

        fun createAuthIntent(application: Application): Intent {
            // Create the verifier and challenge.
            val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            val codeVerifier = run {
                val bytes = ByteArray(64)
                SecureRandom().nextBytes(bytes)
                Base64.encodeToString(bytes, encoding)
            }
            val codeChallenge = run {
                val digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)
                val hash = digest.digest(codeVerifier.toByteArray())
                Base64.encodeToString(hash, encoding)
            }

            val authServiceConfig = AuthorizationServiceConfiguration(
                Uri.parse(URL_AUTHORIZATION),
                Uri.parse(URL_TOKEN_EXCHANGE),
                null,
                null)

            val request = AuthorizationRequest.Builder(
                authServiceConfig,
                CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(URL_AUTH_REDIRECT)).apply {
                setCodeVerifier(codeVerifier, codeChallenge, CODE_VERIFIER_CHALLENGE_METHOD)
                setScopes(SCOPE_PROFILE, SCOPE_EMAIL, SCOPE_OPENID)
            }.build()
            return getAuthorizationService(application).getAuthorizationRequestIntent(request)!!
        }

        fun handleAuthResult(application: Application, result: ActivityResult) {
            val intent = requireNotNull(result.data)
            val response = AuthorizationResponse.fromIntent(intent)
            val error = AuthorizationException.fromIntent(intent)
            val authState = AuthState(response, error)

            if (response != null) {
                val tokenRequest = response.createTokenExchangeRequest()
                authService.performTokenRequest(tokenRequest) { tokenResponse, tokenError ->
                    authState.update(tokenResponse, tokenError)
                    if (tokenError != null) {
                        Log.e("AuthActivity", "token error $tokenError")
                    } else if (tokenResponse != null) {
                        Log.v("AuthActivity", "token result $tokenResponse")
                        val serialized = authState.jsonSerializeString()
                        Log.v("AuthActivity", serialized)
                    }
                }
            }
        }

        private fun getAuthorizationService(application: Application): AuthorizationService {
            if (!this::authService.isInitialized) {
                val appAuthConfiguration = AppAuthConfiguration.Builder()
                    .setBrowserMatcher(
                        BrowserAllowList(
                            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
                        )
                    ).build()

                authService = AuthorizationService(application, appAuthConfiguration)
            }
            return authService
        }

        fun restoreAuthState(context: Context): AuthState? {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val serialized = prefs.getString(SHARED_PREFERENCES_NAME, null)
            return serialized?.let {
                AuthState.jsonDeserialize(it)
            }
        }

        fun saveAuthState(context: Context, authState: AuthState) {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString(SHARED_PREFERENCES_NAME, authState.jsonSerializeString())
            }
        }
    }
}
