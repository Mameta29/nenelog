package app.nenelog.domain

data class BilingualText(val ja: String, val en: String)

/** docs/05 の授乳TTS台本を一か所に閉じ込める。 */
object NursingResponseComposer {

    fun started(side: Side): BilingualText = when (side) {
        Side.RIGHT -> BilingualText(ja = "右、スタート", en = "Right, started.")
        Side.LEFT -> BilingualText(ja = "左、スタート", en = "Left, started.")
    }

    fun noTimer(): BilingualText = BilingualText(
        ja = "タイマーは動いていません",
        en = "No timer is running.",
    )

    fun stopped(payload: EventPayload.Nursing): BilingualText {
        val durations = durationParts(payload)
        if (durations.isEmpty()) {
            return BilingualText(
                ja = "おしまい。授乳を記録しました",
                en = "Done. Nursing recorded.",
            )
        }
        val jaParts = durations.map { (side, minutes) -> "${side.ttsLabelJa()}、${minutes}分" }
        val enParts = durations.map { (side, minutes) ->
            "${side.labelEn()} $minutes ${if (minutes == 1L) "minute" else "minutes"}"
        }
        return BilingualText(
            ja = "おしまい。${jaParts.joinToString("。")}を記録しました",
            en = "Done. ${enParts.joinToString(" and ")} recorded.",
        )
    }

    private fun durationParts(payload: EventPayload.Nursing): List<Pair<Side, Long>> = buildList {
        val right = payload.segments.filter { it.side == Side.RIGHT }.sumOf { it.durationMillis }
        val left = payload.segments.filter { it.side == Side.LEFT }.sumOf { it.durationMillis }
        if (right > 0) add(Side.RIGHT to right.roundedMinutes())
        if (left > 0) add(Side.LEFT to left.roundedMinutes())
    }

    private fun Side.ttsLabelJa(): String = if (this == Side.RIGHT) "みぎ" else "ひだり"
    private fun Side.labelEn(): String = if (this == Side.RIGHT) "Right" else "Left"
    private fun Long.roundedMinutes(): Long = when {
        this <= 0 -> 0
        this < 60_000 -> 1
        else -> (this + 30_000) / 60_000
    }
}
