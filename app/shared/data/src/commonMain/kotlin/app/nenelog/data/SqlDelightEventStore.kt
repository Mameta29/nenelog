package app.nenelog.data

import app.nenelog.data.db.NenelogDatabase
import app.nenelog.domain.Event
import app.nenelog.domain.EventId
import app.nenelog.domain.EventPayload
import app.nenelog.domain.EventSource
import app.nenelog.domain.EventStore
import app.nenelog.domain.NursingSessionSnapshot
import app.nenelog.domain.Timestamp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StoredNursingSession(
    val snapshot: NursingSessionSnapshot,
    val source: EventSource,
    val caregiverId: String,
    val timezoneId: String,
)

interface NursingSessionStore : EventStore {
    fun loadActiveSession(): StoredNursingSession?
    fun saveActiveSession(session: StoredNursingSession, updatedAt: Timestamp)
    fun clearActiveSession()

    /** 完了イベントの追記とActiveSession削除を同一transactionで確定する。 */
    fun completeActiveSession(event: Event)
}

/**
 * Local-first production store. Events are immutable rows; undo appends a revocation row.
 * ActiveSession is a single durable snapshot so Siri/FGS/process restarts share one timer.
 */
class SqlDelightEventStore(
    private val database: NenelogDatabase,
    private val timezoneId: String,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : NursingSessionStore {

    private val queries = database.nenelogQueries

    override fun append(event: Event) {
        require(findById(event.id) == null) { "duplicate event id: ${event.id}" }
        queries.insertEvent(
            id = event.id,
            baby_id = event.babyId,
            caregiver_id = event.caregiverId,
            occurred_at = event.occurredAt.epochMillis,
            created_at = event.createdAt.epochMillis,
            source = event.source.toStorageValue(),
            payload_type = event.payload.storageType(),
            payload_json = json.encodeToString(EventPayload.serializer(), event.payload),
            revoked_by = event.revokedBy,
            timezone_id = timezoneId,
        )
    }

    override fun all(): List<Event> = queries.selectAllEvents(::mapEvent).executeAsList()

    override fun active(): List<Event> = queries.selectActiveEvents(::mapEvent).executeAsList()

    override fun revoke(targetEventId: String, caregiverId: String, at: Timestamp): Event? {
        var created: Event? = null
        database.transaction {
            val target = findById(targetEventId) ?: return@transaction
            if (target.revokedBy != null || target.payload is EventPayload.Revocation) return@transaction

            val revocation = Event(
                id = EventId.new(at),
                babyId = target.babyId,
                caregiverId = caregiverId,
                occurredAt = at,
                createdAt = at,
                source = target.source,
                payload = EventPayload.Revocation(targetEventId),
            )
            queries.markEventRevoked(revoked_by = revocation.id, id = target.id)
            append(revocation)
            created = revocation
        }
        return created
    }

    override fun undoLast(caregiverId: String, at: Timestamp): Event? {
        val target = queries.selectLatestActiveEvent(::mapEvent).executeAsOneOrNull() ?: return null
        return if (revoke(target.id, caregiverId, at) != null) target else null
    }

    override fun loadActiveSession(): StoredNursingSession? =
        queries.selectActiveSession { snapshotJson, source, caregiverId, storedTimezoneId ->
            StoredNursingSession(
                snapshot = json.decodeFromString(NursingSessionSnapshot.serializer(), snapshotJson),
                source = source.toEventSource(),
                caregiverId = caregiverId,
                timezoneId = storedTimezoneId,
            )
        }.executeAsOneOrNull()

    override fun saveActiveSession(session: StoredNursingSession, updatedAt: Timestamp) {
        queries.saveActiveSession(
            snapshot_json = json.encodeToString(NursingSessionSnapshot.serializer(), session.snapshot),
            source = session.source.toStorageValue(),
            caregiver_id = session.caregiverId,
            timezone_id = session.timezoneId,
            updated_at = updatedAt.epochMillis,
        )
    }

    override fun clearActiveSession() {
        queries.clearActiveSession()
    }

    override fun completeActiveSession(event: Event) {
        database.transaction {
            append(event)
            clearActiveSession()
        }
    }

    private fun findById(id: String): Event? =
        queries.selectEventById(id, ::mapEvent).executeAsOneOrNull()

    private fun mapEvent(
        id: String,
        babyId: String,
        caregiverId: String,
        occurredAt: Long,
        createdAt: Long,
        source: String,
        @Suppress("UNUSED_PARAMETER") payloadType: String,
        payloadJson: String,
        revokedBy: String?,
    ): Event = Event(
        id = id,
        babyId = babyId,
        caregiverId = caregiverId,
        occurredAt = Timestamp(occurredAt),
        createdAt = Timestamp(createdAt),
        source = source.toEventSource(),
        payload = json.decodeFromString(EventPayload.serializer(), payloadJson),
        revokedBy = revokedBy,
    )

    private fun EventPayload.storageType(): String = when (this) {
        is EventPayload.Nursing -> "nursing"
        is EventPayload.Bottle -> "bottle"
        is EventPayload.Pumping -> "pumping"
        is EventPayload.Diaper -> "diaper"
        is EventPayload.Sleep -> "sleep"
        is EventPayload.Temperature -> "temperature"
        is EventPayload.Medicine -> "medicine"
        EventPayload.Bath -> "bath"
        is EventPayload.Growth -> "growth"
        is EventPayload.Memo -> "memo"
        is EventPayload.Revocation -> "revocation"
    }

    private fun EventSource.toStorageValue(): String = name.lowercase()

    private fun String.toEventSource(): EventSource =
        EventSource.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: error("unknown event source: $this")
}
