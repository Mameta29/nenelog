package app.nenelog.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.nenelog.android.spike.NursingVoiceService
import app.nenelog.domain.VoiceLocaleResolver

/** ホーム画面の授乳タイマーとPixelのマイクFGSを連動させる。 */
class AndroidNursingVoiceSessionController(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun start(): Boolean {
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        // Android may return region/extension variants such as en-JP-u-mu-celsius.
        // SpeechRecognizer only receives Nenelog's explicitly supported locales.
        val locale = VoiceLocaleResolver.fromLanguageTag(
            appContext.resources.configuration.locales[0].toLanguageTag(),
        )
        val intent = Intent(appContext, NursingVoiceService::class.java)
            .putExtra(NursingVoiceService.EXTRA_LOCALE, locale)
            .putExtra(NursingVoiceService.EXTRA_PREFER_OFFLINE, true)
        return runCatching {
            if (Build.VERSION.SDK_INT >= 26) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            true
        }.getOrDefault(false)
    }

    fun stop() {
        appContext.stopService(Intent(appContext, NursingVoiceService::class.java))
    }
}
