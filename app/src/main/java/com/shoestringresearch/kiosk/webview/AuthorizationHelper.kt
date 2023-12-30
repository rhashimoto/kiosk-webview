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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

const val DEFAULT_ISSUER = "https://accounts.google.com"
val DEFAULT_SCOPES = arrayOf("profile", "email", "openid")

class AuthorizationHelper private constructor(builder: Builder) {
    private val application: Application
    private val coroutineScope: CoroutineScope
    private val preferenceName: String
    private val urlAuthRedirect: String

    private val codeVerifierChallengeMethod: String
    private val messageDigestAlgorithm: String

    private val authorizationService: AuthorizationService
    private var authState: AuthState? = null
    private val prefs: SharedPreferences

    private val mutex = Mutex()
    private lateinit var resultCompletable: CompletableDeferred<ActivityResult>

    init {
        application = builder.application
        coroutineScope = builder.coroutineScope
        preferenceName = builder.preferenceName
        urlAuthRedirect = builder.urlAuthRedirect

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

    fun createAuthIntent(config: Config, action: (Intent) -> Unit) {
        coroutineScope.launch {
            doAuthorizationFlow(config, action)
        }
    }

    fun handleAuthResult(result: ActivityResult) {
        resultCompletable.complete(result)
    }

    suspend fun getAuthState(): AuthState? = mutex.withLock {
        if (authState == null) return null

        val lastTokenResponse = authState!!.lastTokenResponse
        val completable = CompletableDeferred<Unit>()
        authState!!.performActionWithFreshTokens(authorizationService) { _, _, e ->
            try {
                if (e != null) throw e

                if (authState!!.lastTokenResponse != lastTokenResponse) {
                    Log.v("AuthorizationHelper", "tokens refreshed")
                    persistAuthState()
                } else {
                    Log.v("AuthorizationHelper", "token refresh not needed")
                }
                completable.complete(Unit)
            } catch (e: Exception) {
                Log.e("AuthorizationHelper", "refresh failed: ${e.message}")
                completable.completeExceptionally(e)
            }
        }
        completable.await()
        return authState
    }

    private suspend fun doAuthorizationFlow(
        config: Config,
        launchIntent: (Intent) -> Unit) = mutex.withLock {
        try {
            // Extract config file settings.
            val clientId = config.clientId ?: throw Exception("missing clientId")
            val issuer = config.issuer ?: DEFAULT_ISSUER
            val scopes = config.scopes ?: DEFAULT_SCOPES

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
            val serviceConfigDeferred = CompletableDeferred<AuthorizationServiceConfiguration>()
            AuthorizationServiceConfiguration.fetchFromIssuer(
                Uri.parse(issuer)) { serviceConfiguration, ex ->
                if (ex != null) {
                    serviceConfigDeferred.completeExceptionally(ex)
                } else if (serviceConfiguration == null) {
                    serviceConfigDeferred.completeExceptionally(Exception("missing configuration"))
                } else {
                    serviceConfigDeferred.complete(serviceConfiguration)
                }
            }

            // Build the authorization code request.
            val request = AuthorizationRequest.Builder(
                serviceConfigDeferred.await(),
                clientId,
                ResponseTypeValues.CODE,
                Uri.parse(urlAuthRedirect)
            ).apply {
                setCodeVerifier(codeVerifier, codeChallenge, codeVerifierChallengeMethod)
                setScopes(scopes.toList())
            }.build()

            // Call the application callback with the intent that sends the request.
            resultCompletable = CompletableDeferred()
            withContext(Dispatchers.Main) {
                Log.v("AuthorizationHelper", "providing intent")
                launchIntent(authorizationService.getAuthorizationRequestIntent(request)!!)
            }

            // Get the authorization result back from the application.
            val result = resultCompletable.await()
            val intent = requireNotNull(result.data)
            AuthorizationException.fromIntent(intent)?.let { error ->
                throw error
            }

            val response = AuthorizationResponse.fromIntent(intent)
            requireNotNull(response)
            authState = AuthState(response, null)

            // Exchange the authorization code for tokens.
            val tokenRequest = response.createTokenExchangeRequest()
            val tokenResponseDeferred = CompletableDeferred<TokenResponse>()
            authorizationService.performTokenRequest(tokenRequest) { tokenResponse, tokenError ->
                try {
                    tokenError?.let { throw tokenError }
                    tokenResponseDeferred.complete(tokenResponse!!)
                } catch (e: Exception) {
                    tokenResponseDeferred.completeExceptionally(e)
                }
            }

            authState!!.update(tokenResponseDeferred.await(), null)

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

    data class Builder(val application: Application) {
        var coroutineScope = CoroutineScope(Dispatchers.IO)
        var preferenceName = "authorizationState"
        var urlAuthRedirect = "${application.packageName}:/oauth2redirect"
        var codeVerifierChallengeMethod = "S256"
        var messageDigestAlgorithm = "SHA-256"

        fun build() = AuthorizationHelper(this)
    }

    // Configuration argument for createAuthIntent.
    data class Config(
        var clientId: String?,
        var scopes: Array<String>?,
        var issuer: String?) {

        @Suppress("unused") var webClientId: String? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Config

            if (clientId != other.clientId) return false
            if (scopes != null) {
                if (other.scopes == null) return false
                if (!scopes.contentEquals(other.scopes)) return false
            } else if (other.scopes != null) return false
            return issuer == other.issuer
        }

        override fun hashCode(): Int {
            var result = clientId?.hashCode() ?: 0
            result = 31 * result + (scopes?.contentHashCode() ?: 0)
            result = 31 * result + (issuer?.hashCode() ?: 0)
            return result
        }
    }
}
