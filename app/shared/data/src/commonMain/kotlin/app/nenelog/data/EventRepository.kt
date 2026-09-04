package app.nenelog.data

import app.nenelog.domain.Event
import app.nenelog.domain.EventStore
import app.nenelog.domain.Timestamp

/**
 * Week 1 プレースホルダ。
 * Week 2-3 で SQLDelight スキーマ+Outbox 同期(Supabase)に置き換える(docs/06)。
 * 書込みは常にローカル先行 → Outbox から非同期 push。
 */
class EventRepository(private val store: EventStore) {

    fun record(event: Event) = store.append(event)

    fun timeline(): List<Event> = store.active()

    fun undoLast(caregiverId: String, at: Timestamp): Event? = store.undoLast(caregiverId, at)
}
