package com.thalesgroup.gemalto.IdCloudAccessSample.agents

import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.thalesgroup.gemalto.IdCloudAccessSample.Configuration
import com.thalesgroup.gemalto.d1.D1Exception
import com.thalesgroup.gemalto.d1.D1Task
import com.thalesgroup.gemalto.d1.D1Task.Callback
import com.thalesgroup.gemalto.d1.risk.RiskParams
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

interface OnRiskAnalyzeListener {
    fun onRiskAnalyzeSuccess()
    fun onRiskAnalyzeError(exception: D1Exception)
}

interface OnRiskServerResponse {
    fun onSuccess(riskId: String?)
    fun onError(errorMessage: String?)
}

class RiskAgent @Inject constructor(private var riskSDK: D1Task) {
    private var onRiskAnalyzeListener: OnRiskAnalyzeListener? = null
    private var onRiskServerResponse: OnRiskServerResponse? = null

    /**
     * Start the risk analyze
     */
    fun startAnalyze(
        params: RiskParams<Fragment>,
        onRiskAnalyzeListener: OnRiskAnalyzeListener,
    ) {
        this.onRiskAnalyzeListener = onRiskAnalyzeListener

        riskSDK.startAnalyze(
            params,
            object : Callback<Void> {
                override fun onSuccess(data: Void?) {
                    onRiskAnalyzeListener.onRiskAnalyzeSuccess()
                }

                override fun onError(exception: D1Exception) {
                    onRiskAnalyzeListener.onRiskAnalyzeError(exception)
                }
            }
        )
    }

    /**
     * Pause the risk analyze
     */
    fun pauseAnalyze() {
        riskSDK.pauseAnalyze()
    }

    /**
     * Stop the risk analyze and submit the result to the server
     */
    fun submitRiskPayload(onRiskServerResponse: OnRiskServerResponse) {
        this.onRiskServerResponse = onRiskServerResponse

        riskSDK.stopAnalyze(object : Callback<ByteArray?> {
            override fun onSuccess(data: ByteArray?) {
                // Stop Analyze :: result -> success
                val payload = data?.let { String(it, StandardCharsets.UTF_8) }
                // The payload is a string in JSON format which contains the data for risk assessment on the SDK backend.
                try {
                    if (payload != null) {
                        // Send a payload to the backend server
                        sendPayloadToAppBackend(payload, onRiskServerResponse)
                    }
                } catch (error: IOException) {
                    // Send a payload to the backend server error
                    onRiskServerResponse.onError(error.message)
                }
            }

            override fun onError(error: D1Exception) {
                // Stop Analyze :: result -> error
                onRiskServerResponse.onError(error.message)
            }
        })
    }

    private fun sendPayloadToAppBackend(payload: String, onRiskServerResponse: OnRiskServerResponse) {
        val issuerUrl: String = Configuration.RISK_URL + "/riskstorage"

        val riskJson: JsonObject = Gson().fromJson(payload, JsonObject::class.java)
        val request: ByteArray = riskJson.toString().toByteArray()

        val url: URL
        var urlConnection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        val builder = StringBuilder()
        var responseResult: String

        try {
            url = URL(issuerUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.setFixedLengthStreamingMode(request.size)
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.setRequestProperty("Accept", "image/png")
            urlConnection.setRequestProperty("Cache-Control", "no-cache")
            urlConnection.outputStream.use { os -> os.write(request) }
            val statusCode = urlConnection.responseCode
            if (statusCode in 200..299) {
                inputStream = BufferedInputStream(urlConnection.inputStream)
                var line: String?
                val reader = BufferedReader(InputStreamReader(inputStream))
                while (reader.readLine().also { line = it } != null) {
                    builder.append(line)
                }
                if (builder.toString().isNotEmpty()) {
                    responseResult = builder.toString()
                    val response = Gson().fromJson(
                        responseResult,
                        ResponsePayload::class.java
                    )
                    val riskId = response.payloadId
                    riskId!!.replace("\"".toRegex(), "")
                    onRiskServerResponse.onSuccess(riskId)
                }
            } else {
                onRiskServerResponse.onError("Invalid http status code")
            }
        } catch (exception: MalformedURLException) {
            onRiskServerResponse.onError(exception.message)
        } catch (exception: SSLHandshakeException) {
            onRiskServerResponse.onError(exception.message)
        } finally {
            urlConnection?.disconnect()
            inputStream?.close()
        }
    }

    internal class ResponsePayload {
        var payloadId: String? = null
    }
}
