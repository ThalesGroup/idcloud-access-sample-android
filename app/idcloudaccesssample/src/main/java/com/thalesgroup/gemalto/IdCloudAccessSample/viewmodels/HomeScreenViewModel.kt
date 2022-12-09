package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import androidx.lifecycle.ViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.logger.LoggerImpl
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.SingleLiveEvent
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.USERNAME
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.withDateAndTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepo
) : ViewModel() {

    private val mLoggerData: SingleLiveEvent<String> = SingleLiveEvent()
    var loggerData: SingleLiveEvent<String> = mLoggerData
    var logger: LoggerImpl? = null

    fun getUserName(): String? = runBlocking {
        dataStoreRepository.getString(USERNAME)
    }

    fun clearPreferences(key: String) = runBlocking {
        dataStoreRepository.clearPreferences(key)
    }

    fun setLog(value: String) {
        if (logger == null) {
            logger = object : LoggerImpl() {
                override fun log(text: String?) {
                    super.log(text)
                    text?.let {
                        mLoggerData.value = it
                    }
                }
            }
        }
        logger?.log(value.withDateAndTime())
    }

    fun getLogs(): List<String?>? {
        return logger?.logs
    }

    fun clearLogs() {
        logger?.clear()
        // when clear function is called , we will set the observer value as blank because without setting it, it will give the last value in the list to observer
        mLoggerData.value = ""
    }
}
