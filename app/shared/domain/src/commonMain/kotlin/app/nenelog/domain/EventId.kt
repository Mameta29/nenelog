package app.nenelog.domain

import kotlin.random.Random

/**
 * UUIDv7(48bit ミリ秒タイムスタンプ + ランダム)を生成する。
 * 時系列で辞書順ソート可能(docs/06 のイベントID要件)。
 */
object EventId {

    fun new(at: Timestamp, random: Random = Random.Default): String {
        val bytes = ByteArray(16)
        val ts = at.epochMillis
        bytes[0] = (ts ushr 40).toByte()
        bytes[1] = (ts ushr 32).toByte()
        bytes[2] = (ts ushr 24).toByte()
        bytes[3] = (ts ushr 16).toByte()
        bytes[4] = (ts ushr 8).toByte()
        bytes[5] = ts.toByte()
        val rand = ByteArray(10).also { random.nextBytes(it) }
        rand.copyInto(bytes, destinationOffset = 6)
        // version 7 / variant 10xx
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        val hex = StringBuilder(36)
        for (i in 0 until 16) {
            if (i == 4 || i == 6 || i == 8 || i == 10) hex.append('-')
            val v = bytes[i].toInt() and 0xFF
            hex.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return hex.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
