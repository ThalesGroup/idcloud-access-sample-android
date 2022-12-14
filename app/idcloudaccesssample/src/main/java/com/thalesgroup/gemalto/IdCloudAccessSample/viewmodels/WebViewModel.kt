package com.thalesgroup.gemalto.IdCloudAccessSample.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAException
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.IDCAUserAgent
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.RedirectUriListener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class UriState(var code: String?, var state: String?)
data class UriError(val ex: IDCAException)

@HiltViewModel
class WebViewModel @Inject constructor() : ViewModel() {
    private val mEnrollmentToken: MutableLiveData<String> = MutableLiveData<String>()
    var enrollmentToken: LiveData<String> = mEnrollmentToken

    private val mUriState: MutableLiveData<UriState> = MutableLiveData()
    var uriState: LiveData<UriState> = mUriState

    private val mUriError: MutableLiveData<UriError> = MutableLiveData()
    var uriError: LiveData<UriError> = mUriError

    private val mRedirectUrl: MutableLiveData<String> = MutableLiveData<String>()
    var redirectUrl: LiveData<String> = mRedirectUrl

    private val mFetch: MutableLiveData<String> = MutableLiveData<String>()
    var fetch: LiveData<String> = mFetch

    private val mWebPageFinished: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    var webPageFinished: LiveData<Boolean> = mWebPageFinished

    private var idcaUserAgent: IDCAUserAgent? = null

    fun getIDCAUserAgent(): IDCAUserAgent {
        idcaUserAgent = IDCAUserAgent(object : RedirectUriListener {
            override fun onTokenListener(token: String) {
                mEnrollmentToken.postValue(token)
            }
            override fun onErrorListener(error: String, errorDescription: String) {
                if (error == "access_denied") {
                    mUriError.postValue(UriError(IDCAException(error, errorDescription)))
                } else {
                    mUriError.postValue(UriError(IDCAException(error, errorDescription)))
                }
            }
            override fun onStateListener(code: String, state: String) {
                mUriState.postValue(UriState(code, state))
            }

            override fun onRedirectUriListener(uri: Uri) {
                mRedirectUrl.postValue(uri.toString())
            }

            override fun onScenarioIdListener(uri: String) {
                mFetch.postValue(uri)
            }

            override fun onPageFinished() {
                mWebPageFinished.value = true
            }
        })

        return idcaUserAgent as IDCAUserAgent
    }
}
