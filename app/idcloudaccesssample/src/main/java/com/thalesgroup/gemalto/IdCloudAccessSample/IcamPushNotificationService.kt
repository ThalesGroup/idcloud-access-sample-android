package com.thalesgroup.gemalto.IdCloudAccessSample

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.utilities.PUSH_TOKEN
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.util.HashMap
import javax.inject.Inject

@AndroidEntryPoint
class IcamPushNotificationService : FirebaseMessagingService() {
    private val TAG = IcamPushNotificationService::class.java.simpleName

    @Inject
    lateinit var dataStoreRepository: DataStoreRepo

    override fun onNewToken(token: String) {
        Log.e(TAG, "FCM Refreshed token: $token")
        runBlocking {
            dataStoreRepository.putString(PUSH_TOKEN, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.e(TAG, "FCM Message From: " + remoteMessage.from)
        if (remoteMessage.notification != null) {
            Log.e(TAG, "FCM Message notification Body: " + remoteMessage.notification?.body)
        }
        if (remoteMessage.data.isNotEmpty()) {
            Log.e(TAG, "FCM Message data payload: " + remoteMessage.data)
            val intent = Intent(PROCESS_PUSH_NOTIFICATION_ACTION)
            val map = HashMap(remoteMessage.data)
            intent.putExtra(MAP_EXTRA_NAME, map)
            intent.action = PROCESS_PUSH_NOTIFICATION_ACTION
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    interface PushNotificationHandler {
        fun handlePushNotification(notification: Map<String?, String?>?)
    }

    companion object {
        const val MAP_EXTRA_NAME = "PROCESS_PUSH_NOTIFICATION_MAP"
        const val PROCESS_PUSH_NOTIFICATION_ACTION = "PROCESS_PUSH_NOTIFICATION_ACTION"
    }
}
