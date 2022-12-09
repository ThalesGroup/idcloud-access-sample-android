package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.SCAAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.AUTH_TYPE
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.USERNAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UnenrollmentResponse {
    class Success(var successData: String?) : UnenrollmentResponse()
    class Exception(var exception: IDCAException?) : UnenrollmentResponse()
}

@HiltViewModel
class SettingViewModel @Inject constructor(private val dataStoreRepoImpl: DataStoreRepo) :
    ViewModel() {
    private val mUnenrollResponse: MutableLiveData<UnenrollmentResponse> =
        MutableLiveData<UnenrollmentResponse>()
    var unenrollResponse: LiveData<UnenrollmentResponse> = mUnenrollResponse

    fun unenroll() {
        viewModelScope.launch {
            runCatching {
                SCAAgent.unenroll()
            }.onSuccess {
                clearStorage()
                mUnenrollResponse.value = UnenrollmentResponse.Success("Unenroll Successful")
            }.onFailure {
                if (it is IDCAException) {
                    mUnenrollResponse.value = UnenrollmentResponse.Exception(it)
                }
            }
        }
    }

    private suspend fun clearStorage() {
        dataStoreRepoImpl.clearPreferences(USERNAME)
        dataStoreRepoImpl.clearPreferences(AUTH_TYPE)
    }
}
