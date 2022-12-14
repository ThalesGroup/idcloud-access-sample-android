package com.thalesgroup.gemalto.IdCloudAccessSample.agents

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

interface RedirectUriListener {
    fun onTokenListener(token: String)
    fun onErrorListener(error: String, errorDescription: String)
    fun onStateListener(code: String, state: String)
    fun onRedirectUriListener(uri: Uri)
    fun onScenarioIdListener(uri: String)
    fun onPageFinished()
}

class IDCAUserAgent(private val redirectUriListener: RedirectUriListener) : WebViewClient() {
    var pageStarted = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url
        Log.d("Redirect Urls", uri.toString())
        if (uri?.getQueryParameter("enrollmentToken") != null) {
            Log.e("Enrollment::URL", uri.toString())
            getEnrollmentToken(uri)
            return false
        } else if (uri?.encodedPath?.contains("/oidctest/oidc-callback") == true) {
            Log.e("Callback Redirect::URL", uri.toString())
            if (uri.getQueryParameter("code") != null && uri.getQueryParameter("state") != null) {
                getCodeAndState(uri)
            }
            return false
        } else if (uri?.getQueryParameter("scenarioId") != null) {
            getScenarioId(uri)
            return false
        } else if (uri?.getQueryParameter("error") != null) {
            Log.e("Error: ", uri.toString())
            getError(uri)
            return false
        } else if (uri != null) {
            Log.e("Redirect::URL", uri.toString())
            redirectUriListener.onRedirectUriListener(uri)
            return true
        } else {
            return true
        }
    }

    private fun getError(uri: Uri) {
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        if (error != null && errorDescription != null) {
            redirectUriListener.onErrorListener(error, errorDescription)
        }
    }

    private fun getCodeAndState(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        if (code != null && state != null) {
            redirectUriListener.onStateListener(code, state)
        }
    }

    private fun getEnrollmentToken(uri: Uri) {
        Log.e("Enrollment::URL", uri.toString())
        val enrollmentToken = uri.getQueryParameter("enrollmentToken")
        if (enrollmentToken != null) {
            Log.e("Enrollment Token ", enrollmentToken)
            redirectUriListener.onTokenListener(enrollmentToken)
        }
    }

    private fun getScenarioId(uri: Uri) {
        Log.e("ScenarioId:: ", uri.toString())
        val scenarioId = uri.getQueryParameter("scenarioId")
        if (scenarioId != null) {
            redirectUriListener.onScenarioIdListener(scenarioId)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        pageStarted = true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (pageStarted) {
            redirectUriListener.onPageFinished()
        }
    }
}
