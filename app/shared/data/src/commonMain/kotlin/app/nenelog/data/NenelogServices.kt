package app.nenelog.data

/** One graph keeps every entry path on the same database and event store. */
data class NenelogServices(
    val nursing: NursingSessionService,
    val careLog: CareLogService,
    val settings: AppSettingsService,
)

fun createNenelogServices(
    factory: DatabaseDriverFactory,
    timezoneId: String = "device-local",
): NenelogServices {
    val database = createNenelogDatabase(factory)
    val eventStore = SqlDelightEventStore(database, timezoneId)
    return NenelogServices(
        nursing = NursingSessionService(store = eventStore, timezoneId = timezoneId),
        careLog = CareLogService(store = eventStore),
        settings = AppSettingsService(SqlDelightAppSettingsStore(database)),
    )
}
