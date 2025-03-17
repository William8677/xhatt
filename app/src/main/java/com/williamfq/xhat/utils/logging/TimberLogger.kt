package com.williamfq.xhat.utils.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

@Singleton
class TimberLogger @Inject constructor() : LoggerInterface {

    companion object {
        private const val USER_ID = "William8677"
        private const val TIMESTAMP = "2025-02-21 20:15:02"
        private const val TAG = "TimberLogger"
        private const val MAX_MESSAGE_LENGTH = 4000
    }

    private var crashlytics: FirebaseCrashlytics? = null
    private var isInitialized = false

    init {
        try {
            crashlytics = FirebaseCrashlytics.getInstance()
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Crashlytics in TimberLogger", e)
        }
    }

    override suspend fun logEvent(
        tag: String,
        message: String,
        level: LogLevel,
        throwable: Throwable?
    ) = withContext(Dispatchers.Default) {
        try {
            val formattedMessage = formatLogMessage(message)

            // Dividir mensajes largos para evitar el límite de Android
            if (formattedMessage.length > MAX_MESSAGE_LENGTH) {
                logLongMessage(tag, formattedMessage, level, throwable)
            } else {
                logMessage(tag, formattedMessage, level, throwable)
            }

            // Registrar en Crashlytics si es un error
            if (level == LogLevel.ERROR && isInitialized) {
                logToCrashlytics(tag, formattedMessage, throwable)
            }
        } catch (e: Exception) {
            handleLoggingError(tag, e)
        }
    }

    private fun formatLogMessage(message: String): String {
        return buildString {
            append("[$TIMESTAMP]")
            append("[User: $USER_ID]")
            append(" $message")
        }
    }

    private fun logMessage(tag: String, message: String, level: LogLevel, throwable: Throwable?) {
        when (level) {
            LogLevel.VERBOSE -> Timber.tag(tag).v(throwable, message)
            LogLevel.DEBUG -> Timber.tag(tag).d(throwable, message)
            LogLevel.INFO -> Timber.tag(tag).i(throwable, message)
            LogLevel.WARNING -> Timber.tag(tag).w(throwable, message)
            LogLevel.ERROR -> Timber.tag(tag).e(throwable, message)
        }
    }

    private fun logLongMessage(tag: String, message: String, level: LogLevel, throwable: Throwable?) {
        message.chunked(MAX_MESSAGE_LENGTH).forEachIndexed { index, chunk ->
            val chunkMessage = "Part ${index + 1}: $chunk"
            logMessage(tag, chunkMessage, level, if (index == 0) throwable else null)
        }
    }

    private fun logToCrashlytics(tag: String, message: String, throwable: Throwable?) {
        try {
            crashlytics?.apply {
                setCustomKey("log_timestamp", TIMESTAMP)
                setCustomKey("log_tag", tag)
                setCustomKey("log_message", message)
                log("$tag: $message")
                throwable?.let { recordException(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging to Crashlytics", e)
        }
    }

    private fun handleLoggingError(tag: String, error: Exception) {
        try {
            // Log básico como último recurso
            Log.e(TAG, "Error logging with TimberLogger", error)

            // Intentar registrar en Crashlytics si está disponible
            if (isInitialized) {
                crashlytics?.apply {
                    setCustomKey("logger_error_time", TIMESTAMP)
                    setCustomKey("logger_error_tag", tag)
                    recordException(error)
                }
            }
        } catch (e: Exception) {
            // Si todo lo demás falla, usar el log más básico posible
            Log.e(TAG, "Critical error in logging: ${e.message}")
        }
    }

    fun setUserId(userId: String) {
        try {
            if (isInitialized) {
                crashlytics?.setUserId(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user ID in Crashlytics", e)
        }
    }

    fun setCustomKey(key: String, value: String) {
        try {
            if (isInitialized) {
                crashlytics?.setCustomKey(key, value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting custom key in Crashlytics", e)
        }
    }
}