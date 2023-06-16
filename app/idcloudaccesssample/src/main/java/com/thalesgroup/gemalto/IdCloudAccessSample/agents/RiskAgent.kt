package com.thalesgroup.gemalto.IdCloudAccessSample.agents

import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
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
import javax.net.ssl.SSLHandshakeException

interface OnRiskAnalyzeListener {
    fun onRiskAnalyzeSuccess()
    fun onRiskAnalyzeError(exception: D1Exception)
}

interface OnRiskServerResponse {
    fun onSuccess(riskId: String?)
    fun onError(errorMessage: String?)
}

class RiskAgent constructor(ndUrl: String, ndClientId: String) {
    private var onRiskAnalyzeListener: OnRiskAnalyzeListener? = null
    private var onRiskServerResponse: OnRiskServerResponse? = null
    private val placementName = "LoginMobile"

    private var riskSDK: D1Task? = null

    init {
        riskSDK = D1Task.Builder().setRiskURLString(ndUrl)
            .setRiskClientID(ndClientId).build()
    }

    /**
     * Start the risk analyze
     */
    fun startAnalyze(
        params: RiskParams<Fragment>,
        onRiskAnalyzeListener: OnRiskAnalyzeListener,
    ) {
        this.onRiskAnalyzeListener = onRiskAnalyzeListener

        riskSDK?.startAnalyze(
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
        riskSDK?.pauseAnalyze()
    }

    /**
     * Stop the risk analyze and submit the result to the server
     */
    fun submitRiskPayload(issuerUrl: String, userName: String, onRiskServerResponse: OnRiskServerResponse) {
        this.onRiskServerResponse = onRiskServerResponse

        riskSDK?.stopAnalyze(object : Callback<ByteArray?> {
            override fun onSuccess(data: ByteArray?) {
                // Stop Analyze :: result -> success
                val payload = data?.let { String(it, StandardCharsets.UTF_8) }
                // The payload is a string in JSON format which contains the data for risk assessment on the SDK backend.

                // region Temporary modification to submit the risk data

                // This is a temporary modification to the output from the IdCloud Risk SDK.
                // At present, the output from the SDK is out-of-sync with what is expected
                // by the IdCloud Risk servers.
                // To remove timestamp and rename the 'nds' -> 'environmentData'
                val jsonPayload: JsonObject = Gson().fromJson(payload, JsonObject::class.java)
                val ndsJson: JsonElement = jsonPayload.get("nds")

                val payloadMap: MutableMap<String, Any> = Gson().fromJson(ndsJson, object : TypeToken<Map<String, Any>>() {}.type)
                payloadMap.remove("timestamp")
                val sessionId: String = payloadMap["sessionId"] as String

                val accountInfo: MutableMap<String, Any> = HashMap()
                accountInfo["internalAccountId"] = userName
                accountInfo["userName"] = userName
                accountInfo["emailAddress"] = userName

                val riskPayload: Any =
                    mapOf("accountInfo" to accountInfo, "environmentData" to payloadMap)

                //endregion

                try {
                    sendPayloadToAppBackend(issuerUrl, Gson().toJson(riskPayload), sessionId, onRiskServerResponse)
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

    private fun sendPayloadToAppBackend(issuerUrl: String, payload: String, sessionID: String, onRiskServerResponse: OnRiskServerResponse) {

        val riskJson: JsonObject = Gson().fromJson(payload, JsonObject::class.java)
        val request: ByteArray = riskJson.toString().toByteArray()

        val url: URL
        var urlConnection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        val builder = StringBuilder()

        try {
            url = URL("$issuerUrl/push")
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
                    val acrRiskId = "$placementName:$sessionID"
                    onRiskServerResponse.onSuccess(acrRiskId)
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
}
