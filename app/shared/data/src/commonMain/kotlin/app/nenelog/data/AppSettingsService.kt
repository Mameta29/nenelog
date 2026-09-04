package app.nenelog.data

import app.nenelog.data.db.NenelogDatabase

interface AppSettingsStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

class SqlDelightAppSettingsStore(database: NenelogDatabase) : AppSettingsStore {
    private val queries = database.nenelogQueries

    override fun read(key: String): String? = queries.selectSetting(key).executeAsOneOrNull()

    override fun write(key: String, value: String) {
        queries.upsertSetting(key, value)
    }
}

/** Persisted user choices shared by the iOS and Android Compose UI. */
class AppSettingsService(private val store: AppSettingsStore) {
    fun themePreferenceCode(): String =
        store.read(KEY_THEME)?.takeIf { it in SUPPORTED_THEME_CODES } ?: THEME_AUTO

    fun setThemePreferenceCode(code: String) {
        require(code in SUPPORTED_THEME_CODES) { "unsupported theme preference: $code" }
        store.write(KEY_THEME, code)
    }

    companion object {
        const val THEME_AUTO = "auto"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_NIGHT = "night"

        private const val KEY_THEME = "theme_preference"
        private val SUPPORTED_THEME_CODES = setOf(
            THEME_AUTO,
            THEME_LIGHT,
            THEME_DARK,
            THEME_NIGHT,
        )
    }
}
