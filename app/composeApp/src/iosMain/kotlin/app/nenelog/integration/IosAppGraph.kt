package app.nenelog.integration

import app.nenelog.data.IosDatabaseDriverFactory
import app.nenelog.data.NursingSessionService
import app.nenelog.data.createNursingSessionService
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

/** App UI と App Intents が同じローカルDB・use caseを共有する。 */
internal object IosAppGraph {
    val nursing: NursingSessionService by lazy {
        createNursingSessionService(
            factory = IosDatabaseDriverFactory(),
            timezoneId = currentTimeZoneId(),
        )
    }
}

internal fun iosNursingSessionService(): NursingSessionService = IosAppGraph.nursing

private fun currentTimeZoneId(): String = NSDateFormatter().run {
    dateFormat = "VV"
    stringFromDate(NSDate())
}
