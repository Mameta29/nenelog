package app.nenelog.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.nenelog.data.db.NenelogDatabase

class AndroidDatabaseDriverFactory(
    private val context: Context,
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = NenelogDatabase.Schema,
        context = context.applicationContext,
        name = DATABASE_NAME,
    )

    private companion object {
        const val DATABASE_NAME = "nenelog.db"
    }
}
