package com.thalesgroup.gemalto.IdCloudAccessSample.agents

import SampleUiCallback
import android.content.Context
import androidx.fragment.app.Fragment
import com.thales.dis.mobile.idcloud.auth.IdCloudClient
import com.thales.dis.mobile.idcloud.auth.IdCloudClientConfig
import com.thales.dis.mobile.idcloud.auth.IdCloudClientFactory
import com.thales.dis.mobile.idcloud.auth.exception.IdCloudClientException
import com.thales.dis.mobile.idcloud.auth.operation.EnrollRequestCallback
import com.thales.dis.mobile.idcloud.auth.operation.EnrollResponse
import com.thales.dis.mobile.idcloud.auth.operation.EnrollmentToken
import com.thales.dis.mobile.idcloud.auth.operation.EnrollmentTokenFactory
import com.thales.dis.mobile.idcloud.auth.operation.FetchRequestCallback
import com.thales.dis.mobile.idcloud.auth.operation.FetchResponse
import com.thales.dis.mobile.idcloud.auth.operation.NotificationProfile
import com.thales.dis.mobile.idcloud.auth.operation.ProcessNotificationRequestCallback
import com.thales.dis.mobile.idcloud.auth.operation.ProcessNotificationResponse
import com.thales.dis.mobile.idcloud.auth.operation.RefreshPushTokenRequestCallback
import com.thales.dis.mobile.idcloud.auth.operation.RefreshPushTokenResponse
import com.thales.dis.mobile.idcloud.auth.operation.UnenrollRequestCallback
import com.thales.dis.mobile.idcloud.auth.operation.UnenrollResponse
import com.thales.dis.mobile.idcloud.auth.ui.UiCallbacks
import com.thales.dis.mobile.idcloud.authui.callback.SampleBiometricUiCallback
import com.thales.dis.mobile.idcloud.authui.callback.SampleCommonUiCallback
import com.thales.dis.mobile.idcloud.authui.callback.SampleSecurePinUiCallback
import com.thalesgroup.gemalto.IdCloudAccessSample.Configuration
import com.thalesgroup.gemalto.securelog.SecureLogConfig
import com.thalesgroup.gemalto.securelog.SecureLogLevel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Activity scoped annotation tells dagger that this class is bind to the activity and it's child only, like
// fragments, that means only once instance of the SCAAgent will be created for the activities and fragments
// If we won't use the scopes then multiple instances will be created whenever this class instance is used

// @Inject annotation tells dagger that this class is for dependency injection
// Without @Inject annotation , compilation error will happen with error
// **Class cannot be provided without an @Inject constructor or an @Provides-annotated method**

object SCAAgent {
    private var idCloudClient: IdCloudClient? = null
    private var fragment: Fragment? = null

    fun init(fragment: Fragment?, msUrl: String, tenantId: String) {
        this.fragment = fragment
        fragment?.activity?.applicationContext?.let {
            configureIdcloud(
                it, "icamdemo"
            )
        }
        try {
            idCloudClient = fragment?.activity?.let {
                IdCloudClientFactory.createIdCloudClient(
                    it, msUrl, tenantId
                )
            }
        } catch (e: IdCloudClientException) {
            throw IDCAException(e)
        }
    }

    fun configureIdcloud(context: Context, secureLogFileId: String?) {
        val secureLogConfig = SecureLogConfig.Builder(context)
            .publicKey(Configuration.PUBLIC_KEY_MODULUS, Configuration.PUBLIC_KEY_EXPONENT)
            .fileID(secureLogFileId!!)
            .level(SecureLogLevel.ALL)
            .rollingFileMaxCount(10)
            .rollingFileMaxSizeInKB(100)
            .directory(context.filesDir)
            .build()
        IdCloudClient.configureSecureLog(secureLogConfig)
        IdCloudClientConfig.setAttestationKey(Configuration.ATTESTATION_KEY)
    }

    fun getClientId(): String? {
        return idCloudClient?.clientID
    }

    fun isEnrolled(): Boolean {
        return getClientId() != null
    }

    suspend fun enroll(registrationCode: String, pushToken: String?): String {
        try {
            val token: EnrollmentToken = try {
                EnrollmentTokenFactory.createEnrollmentTokenWithBlob(registrationCode.toByteArray())
            } catch (e: IdCloudClientException) {
                throw IDCAException(e)
            }

            pushToken?.let { token.setDevicePushToken(pushToken) }

            val uiCallbacks = UiCallbacks()
            uiCallbacks.biometricUiCallback = SampleBiometricUiCallback()
            uiCallbacks.commonUiCallback = SampleUiCallback(fragment?.childFragmentManager)
            uiCallbacks.securePinPadUiCallback =
                SampleSecurePinUiCallback(fragment?.childFragmentManager, "Add Authenticator")
            val enrollResponse = suspendCoroutine { continuation ->
                val enrollRequestCallback: EnrollRequestCallback = object :
                    EnrollRequestCallback() {
                    override fun onSuccess(enrollResponse: EnrollResponse) {
                        continuation.resume("Enrollment completed")
                    }

                    override fun onError(e: IdCloudClientException) {
                        continuation.resumeWithException(e)
                    }
                }
                fragment?.activity?.runOnUiThread {
                    idCloudClient?.createEnrollRequest(token, uiCallbacks, enrollRequestCallback)
                        ?.execute()
                }
            }
            return enrollResponse
        } catch (e: IdCloudClientException) {
            throw IDCAException(e)
        }
    }

    suspend fun fetch(): String {
        try {
            val uiCallbacks = UiCallbacks()
            // BIOMETRIC
            uiCallbacks.biometricUiCallback = SampleBiometricUiCallback()
            // PIN
            uiCallbacks.securePinPadUiCallback =
                SampleSecurePinUiCallback(fragment?.childFragmentManager, "Fetch")
            //  COMMON
            uiCallbacks.commonUiCallback = SampleCommonUiCallback(fragment?.childFragmentManager)

            val fetchResponse = suspendCoroutine { continuation ->
                val fetchRequestCallback = object : FetchRequestCallback() {
                    override fun onSuccess(fetchResponse: FetchResponse) {
                        continuation.resume("Authentication completed")
                    }

                    override fun onError(ex: IdCloudClientException) {
                        continuation.resumeWithException(ex)
                    }
                }

                fragment?.activity?.runOnUiThread {
                    idCloudClient?.createFetchRequest(uiCallbacks, fetchRequestCallback)?.execute()
                }
            }

            return fetchResponse
        } catch (e: IdCloudClientException) {
            throw IDCAException(e)
        }
    }

    suspend fun unenroll(): UnenrollResponse {
        try {
            val response = suspendCoroutine { continuation ->
                val unenrollRequestCallback: UnenrollRequestCallback =
                    object : UnenrollRequestCallback() {
                        override fun onSuccess(unenrollResponse: UnenrollResponse) {
                            continuation.resume(unenrollResponse)
                        }

                        override fun onError(e: IdCloudClientException) {
                            continuation.resumeWithException(e)
                        }
                    }

                fragment?.activity?.runOnUiThread {
                    idCloudClient?.createUnenrollRequest(unenrollRequestCallback)?.execute()
                }
            }
            return response
        } catch (e: IdCloudClientException) {
            throw IDCAException(e)
        }
    }

    suspend fun updatePushToken(token: String): RefreshPushTokenResponse {
        val notificationProfile = NotificationProfile(token)
        val response = suspendCoroutine { continuation ->
            val refreshPushTokenRequestCallback: RefreshPushTokenRequestCallback =
                object : RefreshPushTokenRequestCallback() {
                    override fun onSuccess(updatePushTokenresponse: RefreshPushTokenResponse) {
                        continuation.resume(updatePushTokenresponse)
                    }

                    override fun onError(e: IdCloudClientException) {
                        continuation.resumeWithException(e)
                    }
                }
            fragment?.activity?.runOnUiThread {
                idCloudClient?.createRefreshPushTokenRequest(
                    notificationProfile,
                    refreshPushTokenRequestCallback
                )?.execute()
            }
        }

        return response
    }

    suspend fun processNotification(data: Map<String?, String?>): ProcessNotificationResponse {
        val uiCallbacks = UiCallbacks()
        // BIOMETRIC
        uiCallbacks.biometricUiCallback = SampleBiometricUiCallback()
        // PIN
        uiCallbacks.securePinPadUiCallback =
            SampleSecurePinUiCallback(fragment?.childFragmentManager, "Fetch")
        //  COMMON
        uiCallbacks.commonUiCallback = SampleCommonUiCallback(fragment?.childFragmentManager)

        val response = suspendCoroutine { continuation ->
            val processNotificationRequestCallback: ProcessNotificationRequestCallback =
                object : ProcessNotificationRequestCallback() {
                    override fun onSuccess(processNotificationResponse: ProcessNotificationResponse) {
                        continuation.resume(processNotificationResponse)
                    }

                    override fun onError(e: IdCloudClientException) {
                        continuation.resumeWithException(e)
                    }
                }

            fragment?.activity?.runOnUiThread {
                idCloudClient?.createProcessNotificationRequest(
                    data,
                    uiCallbacks,
                    processNotificationRequestCallback
                )?.execute()
            }
        }

        return response
    }
}
