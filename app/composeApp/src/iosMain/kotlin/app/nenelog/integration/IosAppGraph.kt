package app.nenelog.integration

import app.nenelog.data.IosDatabaseDriverFactory
import app.nenelog.data.NenelogServices
import app.nenelog.data.NursingSessionService
import app.nenelog.data.createNenelogServices
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

/** App UI と App Intents が同じローカルDB・use caseを共有する。 */
internal object IosAppGraph {
    val services: NenelogServices by lazy {
        createNenelogServices(
            factory = IosDatabaseDriverFactory(),
            timezoneId = currentTimeZoneId(),
        )
    }

    val nursing: NursingSessionService get() = services.nursing
}

internal fun iosServices(): NenelogServices = IosAppGraph.services

internal fun iosNursingSessionService(): NursingSessionService = IosAppGraph.services.nursing

private fun currentTimeZoneId(): String = NSDateFormatter().run {
    dateFormat = "VV"
    stringFromDate(NSDate())
}
