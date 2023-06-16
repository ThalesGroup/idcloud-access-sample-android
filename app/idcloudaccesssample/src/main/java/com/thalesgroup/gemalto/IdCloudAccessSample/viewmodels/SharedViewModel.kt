package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalesgroup.gemalto.IdCloudAccessSample.Configuration
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.OnRiskAnalyzeListener
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.OnRiskServerResponse
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.RiskAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.AUTH_TYPE
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.CLIENT_ID
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.CLIENT_SECRET
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.IDP_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.MS_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.ND_CLIENT_ID
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.ND_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.PUSH_TOKEN
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.REDIRECT_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.RISK_URL
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.TENANT_ID
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.USERNAME
import com.thalesgroup.gemalto.d1.D1Exception
import com.thalesgroup.gemalto.d1.icampoc.OIDCAgent
import com.thalesgroup.gemalto.d1.risk.RiskParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class RiskResponse {
    class Success(var successData: String) : RiskResponse()
    class Exception(var exception: String) : RiskResponse()
}

sealed class UriResponse {
    class Success(var successData: Uri?) : UriResponse()
    class Error(var errorData: String?) : UriResponse()
    class Exception(var exception: String?) : UriResponse()
}

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepo,
    private var oidcAgent: OIDCAgent,
) : ViewModel() {

    private var riskAgent: RiskAgent? = null

    fun init() {
        riskAgent = RiskAgent(getNDUrl(), getNDClientId())
    }

    //region - Risk Agent
    private val mRiskResponse: MutableLiveData<RiskResponse> = MutableLiveData<RiskResponse>()
    var riskResponse: LiveData<RiskResponse> = mRiskResponse

    fun startAnalyzeRisk(params: RiskParams<Fragment>) {
        riskAgent?.startAnalyze(
            params,
            object : OnRiskAnalyzeListener {
                override fun onRiskAnalyzeSuccess() {
                    mRiskResponse.value = RiskResponse.Success("Risk analysis started")
                    // Re-enable the UI
                }

                override fun onRiskAnalyzeError(exception: D1Exception) {
                    mRiskResponse.value = exception.message?.let { RiskResponse.Exception(it) }
                    // Re-enable the UI
                }
            }
        )
    }

    fun pauseAnalyzeRisk() {
        riskAgent?.pauseAnalyze()
    }

    suspend fun stopAnalyzeRisk(): String {
        try {
            val acrValueForRisk = suspendCoroutine { continuation ->
                getUserName()?.let {
                    riskAgent?.submitRiskPayload(
                        getRiskUrl(),
                        it,
                        object : OnRiskServerResponse {
                            override fun onSuccess(riskId: String?) {
                                continuation.resume("sca=fidomob riskid=$riskId")
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resumeWithException(IDCAException("risk", errorMessage!!))
                            }
                        }
                    )
                }
            }
            return acrValueForRisk
        } catch (e: IDCAException) {
            throw e
        }
    }
    //endregion

    //region - OIDCAgent
    private val mUriResponse: MutableLiveData<UriResponse> = MutableLiveData<UriResponse>()
    var uriResponse: LiveData<UriResponse> = mUriResponse

    fun authenticateUser(acrValue: String, userName: String) = viewModelScope.launch {
        runCatching {
            oidcAgent.authorize(getIDPUrl(), acrValue, userName, getClientId(), getRedirectUrl())
        }.onSuccess {
            mUriResponse.value = UriResponse.Success(it)
        }.onFailure {
            if (it is IDCAException) {
                mUriResponse.value = UriResponse.Error(it.getIDCAErrorDescription())
            }
            mUriResponse.value = UriResponse.Exception(it.message)
        }
    }
    //endregion

    //region - Storage
    fun getUserName(): String? = runBlocking {
        dataStoreRepository.getString(USERNAME)
    }

    fun getPushToken(): String? = runBlocking {
        dataStoreRepository.getString(PUSH_TOKEN)
    }

    fun getIDPUrl(): String = runBlocking {
        val idpUrl = dataStoreRepository.getString(IDP_URL)
        return@runBlocking idpUrl ?: Configuration.IDP_URL
    }

    fun getRedirectUrl(): String = runBlocking {
        val redirectUrl = dataStoreRepository.getString(REDIRECT_URL)
        return@runBlocking redirectUrl ?: Configuration.REDIRECT_URL
    }

    fun getClientId(): String = runBlocking {
        val clientId = dataStoreRepository.getString(CLIENT_ID)
        return@runBlocking clientId ?: Configuration.CLIENT_ID
    }

    fun getClientSecret(): String = runBlocking {
        val clientSecret = dataStoreRepository.getString(CLIENT_SECRET)
        return@runBlocking clientSecret ?: Configuration.CLIENT_SECRET
    }

    fun getMSUrl(): String = runBlocking {
        val msUrl = dataStoreRepository.getString(MS_URL)
        return@runBlocking msUrl ?: Configuration.MS_URL
    }

    fun getTenantId(): String = runBlocking {
        val tenantId = dataStoreRepository.getString(TENANT_ID)
        return@runBlocking tenantId ?: Configuration.TENANT_ID
    }

    fun getNDUrl(): String = runBlocking {
        val ndUrl = dataStoreRepository.getString(ND_URL)
        return@runBlocking ndUrl ?: Configuration.ND_URL
    }

    fun getNDClientId(): String = runBlocking {
        val ndClientId = dataStoreRepository.getString(ND_CLIENT_ID)
        return@runBlocking ndClientId ?: Configuration.ND_CLIENT_ID
    }

    fun getRiskUrl(): String = runBlocking {
        val riskUrl = dataStoreRepository.getString(RISK_URL)
        return@runBlocking riskUrl ?: Configuration.RISK_URL
    }

    fun updatePreference(key: String, value: String) {
        runBlocking {
            dataStoreRepository.putString(key, value)
        }
    }

    fun clearStorage() {
        runBlocking {
            dataStoreRepository.clearPreferences(USERNAME)
            dataStoreRepository.clearPreferences(AUTH_TYPE)
        }
    }

    //endregion
}
