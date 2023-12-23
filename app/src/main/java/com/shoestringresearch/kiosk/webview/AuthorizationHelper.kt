package com.shoestringresearch.kiosk.webview

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher
import java.security.MessageDigest
import java.security.SecureRandom

class AuthorizationHelper private constructor(builder: Builder) {
    private val application: Application
    private val coroutineScope: CoroutineScope
    private val preferenceName: String
    private val scopes: Iterable<String>
    private val issuer: String
    private val urlAuthRedirect: String
    private val clientId: String
    private val codeVerifierChallengeMethod: String
    private val messageDigestAlgorithm: String

    private val authorizationService: AuthorizationService
    private var authState: AuthState? = null
    private val prefs: SharedPreferences

    private val mutex = Mutex()
    private val resultChannel = Channel<ActivityResult>(Channel.BUFFERED)

    init {
        application = builder.application
        coroutineScope = builder.coroutineScope
        preferenceName = builder.preferenceName
        scopes = builder.scopes
        issuer = builder.issuer
        urlAuthRedirect = builder.urlAuthRedirect
        clientId = builder.clientId
        codeVerifierChallengeMethod = builder.codeVerifierChallengeMethod
        messageDigestAlgorithm = builder.messageDigestAlgorithm

        // Create AuthorizationService instance.
        val browsers = BrowserAllowList(
            VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
            VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
        )
        val appAuthConfiguration = AppAuthConfiguration.Builder()
            .setBrowserMatcher(browsers)
            .build()
        authorizationService = AuthorizationService(application, appAuthConfiguration)

        // Load AuthState from SharedPreferences.
        prefs = PreferenceManager.getDefaultSharedPreferences(application)
        prefs.getString(preferenceName, null)?.let {json ->
            authState = AuthState.jsonDeserialize(json)
        }
    }

    fun createAuthIntent(action: (Intent) -> Unit) {
        coroutineScope.launch {
            doAuthorizationFlow(action)
        }
    }

    fun handleAuthResult(result: ActivityResult) {
        resultChannel.trySend(result)
    }

    suspend fun getAuthState(): AuthState? = mutex.withLock {
        if (authState == null) return null

        val lastTokenResponse = authState!!.lastTokenResponse
        val completionChannel = Channel<Unit>(Channel.BUFFERED)
        authState!!.performActionWithFreshTokens(authorizationService) { _, _, e ->
            try {
                if (e != null) throw e

                if (authState!!.lastTokenResponse != lastTokenResponse) {
                    Log.v("AuthorizationHelper", "tokens refreshed")
                    persistAuthState()
                } else {
                    Log.v("AuthorizationHelper", "token refresh not needed")
                }
                completionChannel.trySend(Unit)
            } catch (e: Exception) {
                Log.e("AuthorizationHelper", "refresh failed: ${e.message}")
                completionChannel.close(e)
            }
        }
        completionChannel.receive()
        return authState
    }

    private suspend fun doAuthorizationFlow(launchIntent: (Intent) -> Unit) = mutex.withLock {
        try {
            // Create the PKCE verifier and challenge.
            val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            val codeVerifier = run {
                val bytes = ByteArray(64)
                SecureRandom().nextBytes(bytes)
                Base64.encodeToString(bytes, encoding)
            }
            val codeChallenge = run {
                val digest = MessageDigest.getInstance(messageDigestAlgorithm)
                val hash = digest.digest(codeVerifier.toByteArray())
                Base64.encodeToString(hash, encoding)
            }

            // Query the issuer (e.g. Google) for endpoint configuration.
            val configurationChannel = Channel<AuthorizationServiceConfiguration>(Channel.BUFFERED)
            AuthorizationServiceConfiguration.fetchFromIssuer(
                Uri.parse(issuer)) { serviceConfiguration, ex ->
                if (ex != null) {
                    configurationChannel.close(ex)
                } else if (serviceConfiguration == null) {
                    configurationChannel.close(Exception("missing configuration"))
                } else {
                    configurationChannel.trySend(serviceConfiguration)
                }
            }

            // Build the authorization code request.
            val request = AuthorizationRequest.Builder(
                configurationChannel.receive(),
                clientId,
                ResponseTypeValues.CODE,
                Uri.parse(urlAuthRedirect)
            ).apply {
                setCodeVerifier(codeVerifier, codeChallenge, codeVerifierChallengeMethod)
                setScopes(scopes)
            }.build()

            // Call the application callback with the intent that sends the request.
            withContext(Dispatchers.Main) {
                Log.v("AuthorizationHelper", "providing intent")
                launchIntent(authorizationService.getAuthorizationRequestIntent(request)!!)
            }

            // Get the authorization result back from the application.
            val result = resultChannel.receive()
            val intent = requireNotNull(result.data)
            AuthorizationException.fromIntent(intent)?.let { error ->
                throw error
            }

            val response = AuthorizationResponse.fromIntent(intent)
            requireNotNull(response)
            authState = AuthState(response, null)

            // Exchange the authorization code for tokens.
            val tokenRequest = response.createTokenExchangeRequest()
            val tokenResponseChannel = Channel<TokenResponse>(Channel.BUFFERED)
            authorizationService.performTokenRequest(tokenRequest) { tokenResponse, tokenError ->
                try {
                    tokenError?.let { throw tokenError }
                    tokenResponseChannel.trySend(tokenResponse!!)
                } catch (e: Exception) {
                    tokenResponseChannel.close(e)
                }
            }

            val tokenResponse = tokenResponseChannel.receive()
            authState!!.update(tokenResponse, null)

            Log.v("AuthorizationHelper", "tokens received")
            toast("Authorization succeeded", Toast.LENGTH_SHORT)
            persistAuthState()
        } catch (e: Exception) {
            Log.e("AuthorizationHelper", "auth error: ${e.message}")
            toast("Auth failed: ${e.message}", Toast.LENGTH_LONG)
        }
    }

    private fun persistAuthState() {
        val json = if (authState?.needsTokenRefresh == false) {
            authState!!.jsonSerializeString()
        } else {
            null
        }
        prefs.edit {
            if (json != null) {
                putString(preferenceName, json)
            } else {
                remove(preferenceName)
            }
        }
    }

    private suspend fun toast(message: String, duration: Int) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application, message, duration).show()
        }
    }

    data class Builder(val application: Application, val clientId: String) {
        var coroutineScope = CoroutineScope(Dispatchers.IO)
        var preferenceName = "authorizationState"

        var issuer = "https://accounts.google.com"
        var scopes: Iterable<String> = listOf("profile", "email", "openid")
        var urlAuthRedirect = "${application.packageName}:/oauth2redirect"
        var codeVerifierChallengeMethod = "S256"
        var messageDigestAlgorithm = "SHA-256"

        fun build() = AuthorizationHelper(this)
    }
}
