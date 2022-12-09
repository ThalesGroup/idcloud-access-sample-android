package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.OnRiskAnalyzeListener
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.OnRiskServerResponse
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.RiskAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
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
    private val riskAgent: RiskAgent,
    private var oidcAgent: OIDCAgent,
) : ViewModel() {

    //region - Risk Agent
    private val mRiskResponse: MutableLiveData<RiskResponse> = MutableLiveData<RiskResponse>()
    var riskResponse: LiveData<RiskResponse> = mRiskResponse

    fun startAnalyzeRisk(params: RiskParams<Fragment>) {
        riskAgent.startAnalyze(
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
        riskAgent.pauseAnalyze()
    }

    suspend fun stopAnalyzeRisk(): String {
        try {
            val acrValueForRisk = suspendCoroutine { continuation ->
                riskAgent.submitRiskPayload(object : OnRiskServerResponse {
                    override fun onSuccess(riskId: String?) {
                        continuation.resume("sca=fidomob riskid=$riskId")
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resumeWithException(IDCAException("risk", errorMessage!!))
                    }
                })
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
            oidcAgent.authorize(acrValue, userName)
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
    //endregion
}
