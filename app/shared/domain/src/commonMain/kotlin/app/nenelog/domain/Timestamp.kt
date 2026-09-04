package app.nenelog.domain

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * UTCエポックミリ秒のみを持つドメイン時刻。
 *
 * domain層は「時刻の差分」しか扱わない(タイマー=開始時刻の記録、docs/04)ため、
 * カレンダー・タイムゾーンはここに持ち込まない。occurredAt の TZ 併記(docs/06)は
 * data 層の保存時に付与する。
 */
@Serializable
@JvmInline
value class Timestamp(val epochMillis: Long) : Comparable<Timestamp> {
    operator fun minus(other: Timestamp): Long = epochMillis - other.epochMillis
    fun plusMillis(millis: Long): Timestamp = Timestamp(epochMillis + millis)
    override fun compareTo(other: Timestamp): Int = epochMillis.compareTo(other.epochMillis)
}
