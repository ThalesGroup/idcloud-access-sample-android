package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import androidx.lifecycle.ViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.AUTH_TYPE
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.USERNAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepo,
) : ViewModel() {

    fun storeAuthenticationType(authType: Int) = runBlocking {
        dataStoreRepository.putInteger(AUTH_TYPE, authType)
    }

    fun getAuthenticationType(): Int? = runBlocking {
        dataStoreRepository.getInteger(AUTH_TYPE)
    }

    fun storeUserName(userName: String) = runBlocking {
        dataStoreRepository.putString(USERNAME, userName)
    }
}
