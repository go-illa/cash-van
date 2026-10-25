package com.illa.cashvan.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class CashVanMessagingService : FirebaseMessagingService() {

    // Called for data messages, and for notification messages while the app is in the foreground.
    // Notification messages received in the background are shown by the system tray directly.
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (title.isNullOrBlank() && body.isNullOrBlank()) return

        NotificationHelper.show(
            context = this,
            notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
            title = title,
            body = body
        )
    }

    // The backend only receives the token on login (fcm-device-token header),
    // so a rotated token reaches it the next time the agent signs in.
    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed")
    }
}
