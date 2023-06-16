package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.USERNAME
import com.thalesgroup.gemalto.d1.icampoc.OIDCAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

sealed class EnrollmentResponse {
    class Success(var successData: String?) : EnrollmentResponse()
    class Error(var failedData: String?) : EnrollmentResponse()
    class Exception(var exception: String?) : EnrollmentResponse()
}

@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepo,
    private var oidcAgent: OIDCAgent,
) : ViewModel() {

    private val mEnrollmentResponse: MutableLiveData<EnrollmentResponse> =
        MutableLiveData<EnrollmentResponse>()
    var enrollmentResponse: LiveData<EnrollmentResponse> = mEnrollmentResponse

    fun performTokenRequest(username: String, code: String, state: String, clientSecret: String) = viewModelScope.launch {
        runCatching {
            oidcAgent.performTokenRequest(code, state, clientSecret)
        }.onSuccess { response ->
            storeUserName(username)
            mEnrollmentResponse.value =
                EnrollmentResponse.Success("Access Token :: ${response.accessToken}")
            mEnrollmentResponse.value =
                EnrollmentResponse.Success("Id Token :: ${response.idToken}")
            mEnrollmentResponse.value = EnrollmentResponse.Success("Enrollment Successful")
        }.onFailure {
            if (it is IDCAException) {
                mEnrollmentResponse.value = EnrollmentResponse.Error(it.getIDCAErrorDescription())
            }
            mEnrollmentResponse.value = EnrollmentResponse.Exception(it.message)
        }
    }

    private fun storeUserName(userName: String) = runBlocking {
        dataStoreRepository.putString(USERNAME, userName)
    }
}
