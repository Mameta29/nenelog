package app.nenelog.domain

/**
 * 追記型イベントストア(docs/06)。
 * 記録は不変イベントとして追加し、訂正は Revocation イベントの追記で表現する。
 */
interface EventStore {

    fun append(event: Event)

    /** 全イベント(打ち消し済み・Revocation 含む)。occurredAt → id 順 */
    fun all(): List<Event>

    /** 現在ビュー: 打ち消されていない記録イベントのみ。occurredAt → id 順 */
    fun active(): List<Event>

    /**
     * 指定イベントを打ち消す。成功時は追記された Revocation イベントを返す。
     * 対象が存在しない・打ち消し済み・Revocation 自身の場合は null。
     */
    fun revoke(targetEventId: String, caregiverId: String, at: Timestamp): Event?

    /**
     * 「取り消し」1語 undo: 最後に追加された有効な記録イベントを打ち消し、
     * 打ち消された対象イベントを返す。対象がなければ null。
     */
    fun undoLast(caregiverId: String, at: Timestamp): Event?
}

/** Week1 のテスト・開発用。SQLDelight 実装(shared/data)は Week2 以降 */
class InMemoryEventStore : EventStore {

    private val events = mutableListOf<Event>()

    override fun append(event: Event) {
        require(events.none { it.id == event.id }) { "duplicate event id: ${event.id}" }
        events += event
    }

    override fun all(): List<Event> =
        events.sortedWith(compareBy({ it.occurredAt }, { it.id }))

    override fun active(): List<Event> =
        all().filter { it.revokedBy == null && it.payload !is EventPayload.Revocation }

    override fun revoke(targetEventId: String, caregiverId: String, at: Timestamp): Event? {
        val index = events.indexOfFirst { it.id == targetEventId }
        if (index < 0) return null
        val target = events[index]
        if (target.revokedBy != null || target.payload is EventPayload.Revocation) return null

        val revocation = Event(
            id = EventId.new(at),
            babyId = target.babyId,
            caregiverId = caregiverId,
            occurredAt = at,
            createdAt = at,
            source = target.source,
            payload = EventPayload.Revocation(targetEventId),
        )
        events[index] = target.copy(revokedBy = revocation.id)
        events += revocation
        return revocation
    }

    override fun undoLast(caregiverId: String, at: Timestamp): Event? {
        val target = events
            .filter { it.revokedBy == null && it.payload !is EventPayload.Revocation }
            .maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
            ?: return null
        revoke(target.id, caregiverId, at) ?: return null
        return events.first { it.id == target.id }
    }
}
