package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UnenrollmentResponse {
    class Success(var successData: String?) : UnenrollmentResponse()
    class Exception(var exception: IDCAException?) : UnenrollmentResponse()
}

sealed class UpdatePushTokenResponse {
    class Success(var successData: String?, var token: String) : UpdatePushTokenResponse()
    class Exception(var exception: IDCAException?) : UpdatePushTokenResponse()
}

@HiltViewModel
class SettingViewModel @Inject constructor() : ViewModel() {
    private val mUnenrollResponse: MutableLiveData<UnenrollmentResponse> =
        MutableLiveData<UnenrollmentResponse>()
    var unenrollResponse: LiveData<UnenrollmentResponse> = mUnenrollResponse

    private val mUpdatePushTokenResponse: MutableLiveData<UpdatePushTokenResponse> =
        MutableLiveData<UpdatePushTokenResponse>()
    var updatePushTokenResponse: LiveData<UpdatePushTokenResponse> = mUpdatePushTokenResponse

    fun unenroll() {
        viewModelScope.launch {
            runCatching {
                SCAAgent.unenroll()
            }.onSuccess {
                mUnenrollResponse.value = UnenrollmentResponse.Success("Unenroll Successful")
            }.onFailure {
                if (it is IDCAException) {
                    mUnenrollResponse.value = UnenrollmentResponse.Exception(it)
                }
            }
        }
    }

    fun updatePushToken(token: String) {
        viewModelScope.launch {
            runCatching {
                SCAAgent.updatePushToken(token)
            }.onSuccess {
                mUpdatePushTokenResponse.value = UpdatePushTokenResponse.Success("Update Push Token Successful", token)
            }.onFailure {
                if (it is IDCAException) {
                    mUpdatePushTokenResponse.value = UpdatePushTokenResponse.Exception(it)
                }
            }
        }
    }
}
