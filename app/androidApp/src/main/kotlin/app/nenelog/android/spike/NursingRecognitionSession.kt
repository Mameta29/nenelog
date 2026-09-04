package app.nenelog.android.spike

import android.content.Context

/** Foreground Serviceが認識実装の種類に依存せず扱うための最小境界。 */
interface NursingRecognitionSession {
    fun start()
    fun stop()
}

/** debug/releaseのソースセット差を保ったまま、実機評価用認識器を差し込む。 */
interface NursingRecognitionSessionFactory {
    fun create(
        context: Context,
        locale: String,
        commandResponse: (String) -> RecognitionReply?,
        onSessionEnded: () -> Unit,
    ): NursingRecognitionSession
}
