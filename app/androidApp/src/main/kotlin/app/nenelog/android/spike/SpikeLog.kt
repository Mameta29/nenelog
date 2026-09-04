package app.nenelog.android.spike

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** スパイク用の共有ログ(Activity と FGS の両方から書く)。結果は research/spike-results.md へ転記 */
object SpikeLog {
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun add(line: String) {
        Log.d("NeneSpike", line)
        _lines.value = (_lines.value + line).takeLast(200)
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
