package app.nenelog.domain

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class EventStoreTest {

    private fun t(minutes: Int) = Timestamp(minutes * 60_000L)

    private fun event(
        at: Timestamp,
        payload: EventPayload = EventPayload.Diaper(pee = true, poop = false),
        caregiver: String = "mama",
    ) = Event(
        id = EventId.new(at),
        babyId = "baby-1",
        caregiverId = caregiver,
        occurredAt = at,
        createdAt = at,
        source = EventSource.VOICE_L2,
        payload = payload,
    )

    @Test
    fun append_and_read_back_in_time_order() {
        val store = InMemoryEventStore()
        val later = event(t(10))
        val earlier = event(t(5))
        store.append(later)
        store.append(earlier)
        store.active().map { it.id } shouldBe listOf(earlier.id, later.id)
    }

    @Test
    fun duplicate_id_is_rejected() {
        val store = InMemoryEventStore()
        val e = event(t(1))
        store.append(e)
        io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> { store.append(e) }
    }

    @Test
    fun revoke_marks_target_and_appends_revocation_event() {
        val store = InMemoryEventStore()
        val e = event(t(1))
        store.append(e)

        val revocation = store.revoke(e.id, "papa", t(2)).shouldNotBeNull()
        revocation.payload.shouldBeInstanceOf<EventPayload.Revocation>().targetEventId shouldBe e.id

        store.active() shouldBe emptyList() // 現在ビューから消える
        store.all().size shouldBe 2 // ただしイベントは追記で残る(docs/06)
        store.all().first { it.id == e.id }.revokedBy shouldBe revocation.id
    }

    @Test
    fun revoke_unknown_or_already_revoked_returns_null() {
        val store = InMemoryEventStore()
        val e = event(t(1))
        store.append(e)
        store.revoke("no-such-id", "mama", t(2)).shouldBeNull()
        store.revoke(e.id, "mama", t(2)).shouldNotBeNull()
        store.revoke(e.id, "mama", t(3)).shouldBeNull() // 二重打ち消し不可
    }

    @Test
    fun revocation_event_itself_cannot_be_revoked() {
        val store = InMemoryEventStore()
        val e = event(t(1))
        store.append(e)
        val revocation = store.revoke(e.id, "mama", t(2)).shouldNotBeNull()
        store.revoke(revocation.id, "mama", t(3)).shouldBeNull()
    }

    // --- 「取り消し」1語 undo ---

    @Test
    fun undo_revokes_most_recently_created_active_event() {
        val store = InMemoryEventStore()
        val first = event(t(1))
        val second = event(t(2), payload = EventPayload.Bottle(80, BottleKind.FORMULA))
        store.append(first)
        store.append(second)

        val undone = store.undoLast("mama", t(3)).shouldNotBeNull()
        undone.id shouldBe second.id
        store.active().map { it.id } shouldBe listOf(first.id)
    }

    @Test
    fun undo_twice_walks_back_in_creation_order() {
        val store = InMemoryEventStore()
        val first = event(t(1))
        val second = event(t(2))
        store.append(first)
        store.append(second)

        store.undoLast("mama", t(3))!!.id shouldBe second.id
        store.undoLast("mama", t(4))!!.id shouldBe first.id
        store.undoLast("mama", t(5)).shouldBeNull() // もう取り消すものがない
    }

    @Test
    fun undo_on_empty_store_returns_null() {
        InMemoryEventStore().undoLast("mama", t(1)).shouldBeNull()
    }

    @Test
    fun undo_never_targets_revocation_events() {
        val store = InMemoryEventStore()
        val e = event(t(1))
        store.append(e)
        store.undoLast("mama", t(2)).shouldNotBeNull() // e を打ち消す(Revocationが追記される)
        // 直近の追記イベントは Revocation だが、undo の対象にはならない
        store.undoLast("mama", t(3)).shouldBeNull()
    }

    @Test
    fun payload_survives_json_round_trip() {
        // SQLDelight の payload JSON列(docs/06)を見据えた直列化の確認
        val json = kotlinx.serialization.json.Json
        val original = event(
            t(1),
            payload = EventPayload.Nursing(
                segments = listOf(NursingSegment(Side.RIGHT, t(0), t(1))),
                note = "うとうとしてた",
            ),
        )
        val encoded = json.encodeToString(Event.serializer(), original)
        json.decodeFromString(Event.serializer(), encoded) shouldBe original
    }
}
