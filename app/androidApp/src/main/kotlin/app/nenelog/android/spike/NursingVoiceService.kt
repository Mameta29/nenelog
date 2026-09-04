package app.nenelog.android.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import app.nenelog.android.R
import app.nenelog.android.AndroidAppGraph
import app.nenelog.android.AndroidNursingVoiceCommandHandler

/**
 * Pixel L2本番導線: 授乳セッション中だけ、画面オフでも固定文法を連続認識する。
 * 経過時間の真実はDBの開始時刻であり、WakeLockは音声認識維持のために限定する。
 */
class NursingVoiceService : Service() {

    private var loop: NursingRecognitionSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoCloseCheck = object : Runnable {
        override fun run() {
            val status = AndroidAppGraph.nursing(this@NursingVoiceService)
                .status(System.currentTimeMillis())
            if (status.stateCode == "idle") {
                stopSelf()
            } else {
                handler.postDelayed(this, AUTO_CLOSE_POLL_MILLIS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LISTENING -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP_AND_RECORD -> {
                AndroidAppGraph.nursing(this).stop(System.currentTimeMillis())
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundWithMicType()
        if (loop != null) return START_STICKY
        publishState("waiting")

        // 画面オフでもCPUを維持(docs/04: PARTIAL_WAKE_LOCK)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nenelog:voice-session").apply {
            acquire(WAKE_LOCK_TIMEOUT_MILLIS)
        }

        val locale = intent?.getStringExtra(EXTRA_LOCALE) ?: "ja-JP"
        val offline = intent?.getBooleanExtra(EXTRA_PREFER_OFFLINE, true) ?: true
        val commandHandler = AndroidNursingVoiceCommandHandler(AndroidAppGraph.nursing(this))
        val commandResponse: (String) -> RecognitionReply? = { transcript ->
            commandHandler.handle(transcript, locale, System.currentTimeMillis())
        }
        val onSessionEnded = { stopSelf() }
        val onStateChanged: (String, String?) -> Unit = { state, transcript ->
            publishState(state, transcript)
        }
        loop = createEvaluationVoskSession(
            locale = locale,
            commandResponse = commandResponse,
            onSessionEnded = onSessionEnded,
            onStateChanged = onStateChanged,
        ) ?: RecognitionLoop(
            context = this,
            locale = locale,
            preferOffline = offline,
            speakBack = true,
            tag = "FGS",
            commandResponse = commandResponse,
            onSessionEnded = onSessionEnded,
            onStateChanged = onStateChanged,
        )
        loop?.start()
        handler.removeCallbacks(autoCloseCheck)
        handler.postDelayed(autoCloseCheck, AUTO_CLOSE_POLL_MILLIS)
        SpikeLog.add("[FGS] Nenelog voice session started")
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoCloseCheck)
        loop?.stop()
        loop = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        publishState("waiting")
        SpikeLog.add("[FGS] service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Voskは実機評価用debugソースセットにだけ置く。クラスまたはモデルが無ければ、
     * releaseを含めて既存SpeechRecognizerへ自動的に戻る。
     */
    private fun createEvaluationVoskSession(
        locale: String,
        commandResponse: (String) -> RecognitionReply?,
        onSessionEnded: () -> Unit,
        onStateChanged: (String, String?) -> Unit,
    ): NursingRecognitionSession? {
        if (!locale.startsWith("ja", ignoreCase = true)) return null
        return runCatching {
            val factory = Class.forName(VOSK_FACTORY_CLASS_NAME)
                .getDeclaredConstructor()
                .newInstance() as NursingRecognitionSessionFactory
            factory.create(
                context = this,
                locale = locale,
                commandResponse = commandResponse,
                onSessionEnded = onSessionEnded,
                onStateChanged = onStateChanged,
            )
        }.onSuccess {
            SpikeLog.add("[FGS] using Vosk fixed grammar evaluation engine")
        }.onFailure { error ->
            SpikeLog.add("[FGS] Vosk unavailable; using SpeechRecognizer: ${error.message}")
        }.getOrNull()
    }

    private fun startForegroundWithMicType() {
        val channelId = "nursing_voice"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.voice_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val stopIntent = Intent(this, NursingVoiceService::class.java)
            .setAction(ACTION_STOP_AND_RECORD)
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
                .setContentTitle(getString(R.string.voice_notification_title))
                .setContentText(getString(R.string.voice_notification_text))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                        getString(R.string.stop_and_save),
                        stopPendingIntent,
                    ).build(),
                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.voice_notification_title))
                .setContentText(getString(R.string.voice_notification_text))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                        getString(R.string.stop_and_save),
                        stopPendingIntent,
                    ).build(),
                )
                .build()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun publishState(stateCode: String, transcript: String? = null) {
        currentStateCode = stateCode
        currentTranscript = transcript
        sendBroadcast(
            Intent(ACTION_VOICE_STATE_CHANGED)
                .setPackage(packageName)
                .putExtra(EXTRA_STATE_CODE, stateCode)
                .putExtra(EXTRA_TRANSCRIPT, transcript),
        )
    }

    companion object {
        const val ACTION_STOP_LISTENING = "app.nenelog.android.voice.STOP_LISTENING"
        const val ACTION_STOP_AND_RECORD = "app.nenelog.android.voice.STOP_AND_RECORD"
        const val EXTRA_LOCALE = "locale"
        const val EXTRA_PREFER_OFFLINE = "prefer_offline"
        const val ACTION_VOICE_STATE_CHANGED = "app.nenelog.android.voice.STATE_CHANGED"
        const val EXTRA_STATE_CODE = "state_code"
        const val EXTRA_TRANSCRIPT = "transcript"

        @Volatile
        private var currentStateCode: String = "waiting"

        @Volatile
        private var currentTranscript: String? = null

        fun currentUiStateCode(): String = currentStateCode
        fun currentUiTranscript(): String? = currentTranscript
        private const val VOSK_FACTORY_CLASS_NAME =
            "app.nenelog.android.spike.VoskNursingRecognitionSessionFactory"
        private const val NOTIFICATION_ID = 1001
        private const val AUTO_CLOSE_POLL_MILLIS = 60_000L
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 95L * 60 * 1000
    }
}
