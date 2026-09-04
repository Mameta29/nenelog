package app.nenelog.android

import android.content.Context
import app.nenelog.data.AndroidDatabaseDriverFactory
import app.nenelog.data.NenelogServices
import app.nenelog.data.NursingSessionService
import app.nenelog.data.createNenelogServices
import java.util.TimeZone

/** Activity と画面オフFGSが同じDB-backed serviceを共有する。 */
object AndroidAppGraph {
    @Volatile
    private var instance: NenelogServices? = null

    fun services(context: Context): NenelogServices = instance ?: synchronized(this) {
        instance ?: createNenelogServices(
            factory = AndroidDatabaseDriverFactory(context.applicationContext),
            timezoneId = TimeZone.getDefault().id,
        ).also { instance = it }
    }

    fun nursing(context: Context): NursingSessionService = services(context).nursing
}
