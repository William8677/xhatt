package com.williamfq.data.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.williamfq.domain.models.PanicAlert
import com.williamfq.domain.service.MessagingException
import com.williamfq.domain.service.MessagingService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación de [MessagingService] que usa FirebaseMessaging.
 */
class MessagingServiceImpl @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging
) : MessagingService {

    override suspend fun sendEmergencyMessage(recipientId: String, alert: PanicAlert) {
        try {
            val message = createEmergencyMessage(alert, recipientId)
            // Podemos hacer:
            // firebaseMessaging.send(message)
            // o firebaseMessaging.send(message).await()
            // si queremos usar coroutines. Ajusta según tu caso.
            firebaseMessaging.send(message)
        } catch (e: Exception) {
            throw MessagingException("Failed to send emergency message", e)
        }
    }

    /**
     * Crea un [RemoteMessage] con la info de [alert] para enviarlo a [recipientId].
     */
    private fun createEmergencyMessage(alert: PanicAlert, recipientId: String): RemoteMessage {
        return RemoteMessage.Builder("$MESSAGING_TOPIC/$recipientId")
            .addData("type", "EMERGENCY_ALERT")
            .addData("alertId", alert.id.toString())
            .addData("message", alert.message)
            .addData("userId", alert.userId)
            .addData("latitude", alert.location?.latitude?.toString() ?: "")
            .addData("longitude", alert.location?.longitude?.toString() ?: "")
            .addData("timestamp", alert.timestamp.toString())
            .addData("notificationTitle", "Emergency Alert")
            .addData("notificationBody", alert.message)
            .addData("priority", "high")
            .setTtl(EMERGENCY_MESSAGE_TTL) // 4 horas
            .build()
    }

    companion object {
        private const val MESSAGING_TOPIC = "/topics/emergency"
        private const val EMERGENCY_MESSAGE_TTL = 14400 // 4 horas (60 * 60 * 4)
    }
}
