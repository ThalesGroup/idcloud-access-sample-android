package com.thalesgroup.gemalto.d1.icampoc

import android.content.Context
import android.net.Uri
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceConfiguration.RetrieveConfigurationCallback
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OIDCAgent @Inject constructor(private val context: Context) {
    var authState: AuthState? = null
    var authRequest: AuthorizationRequest? = null

    companion object {
        private val TAG = OIDCAgent::class.java.simpleName
    }

    suspend fun authorize(idpUrl: String, acrValue: String, userName: String?, clientId: String, redirectUrl: String): Uri? {
        try {
            // Step 1 : get the service configuration
            val serviceConfiguration = fetchAuthServiceConfig(idpUrl)
            // Step 2 : Create auth state instance
            authState = serviceConfiguration?.let { AuthState(it) }
            // Step 3 : build AuthRequest
            authRequest = serviceConfiguration?.let { buildAuthRequest(it, acrValue, userName, clientId, redirectUrl) }
            // Step 4 : get AuthRequest Uri
            val uri = authRequest?.toUri()
            return uri
        } catch (e: AuthorizationException) {
            throw IDCAException(e)
        }
    }

    // Fetch an AuthorizationServiceConfiguration from an OpenID Connect issuer URI
    private suspend fun fetchAuthServiceConfig(idpUrl: String): AuthorizationServiceConfiguration? =
        suspendCoroutine { continuation ->
            AuthorizationServiceConfiguration.fetchFromIssuer(
                Uri.parse(idpUrl),
                RetrieveConfigurationCallback { serviceConfiguration, ex ->
                    if (ex != null) {
                        continuation.resumeWithException(ex)
                    }
                    serviceConfiguration?.let {
                        continuation.resume(it)
                    }
                }
            )
        }

    private fun buildAuthRequest(serviceConfiguration: AuthorizationServiceConfiguration, acrValue: String, userName: String?, clientId: String, redirectUrl: String): AuthorizationRequest? {

        val authBuilder = AuthorizationRequest.Builder(
            serviceConfiguration, // the authorization service configuration
            clientId, // the client ID, typically pre-registered and static
            ResponseTypeValues.CODE, // the response_type value: we want a code
            Uri.parse(redirectUrl) // / the redirect URI to which the auth response is sent
        )
        val additionalParameters: MutableMap<String, String> = HashMap()
        additionalParameters["acr_values"] = acrValue

        return authBuilder.setScope(AuthorizationRequest.Scope.OPENID).setLoginHint(userName)
            .setPrompt("login").setAdditionalParameters(additionalParameters).build()
    }

    suspend fun performTokenRequest(code: String?, state: String?, clientSecret: String): TokenResponse {
        try {
            val authService = AuthorizationService(context)
            val authorizationResponse = authRequest?.let { AuthorizationResponse.Builder(it).setAuthorizationCode(code).setState(state).build() }

            val clientAuthentication = ClientSecretPost(clientSecret)

            val tokenResponse = suspendCoroutine { continuation ->

                authorizationResponse?.let {
                    authService.performTokenRequest(it.createTokenExchangeRequest(), clientAuthentication) { tokenResponse: TokenResponse?, authException: AuthorizationException? ->
                        if (tokenResponse != null) {
                            continuation.resume(tokenResponse)
                        }
                        if (authException != null) {
                            continuation.resumeWithException(authException)
                        }
                    }
                }
            }
            return tokenResponse
        } catch (e: AuthorizationException) {
            throw IDCAException(e)
        }
    }
}
