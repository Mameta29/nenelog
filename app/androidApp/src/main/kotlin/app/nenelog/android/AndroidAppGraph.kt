package app.nenelog.android

import android.content.Context
import app.nenelog.data.AndroidDatabaseDriverFactory
import app.nenelog.data.NursingSessionService
import app.nenelog.data.createNursingSessionService
import java.util.TimeZone

/** Activity と画面オフFGSが同じDB-backed serviceを共有する。 */
object AndroidAppGraph {
    @Volatile
    private var nursingInstance: NursingSessionService? = null

    fun nursing(context: Context): NursingSessionService = nursingInstance ?: synchronized(this) {
        nursingInstance ?: createNursingSessionService(
            factory = AndroidDatabaseDriverFactory(context.applicationContext),
            timezoneId = TimeZone.getDefault().id,
        ).also { nursingInstance = it }
    }
}
