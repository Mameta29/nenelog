package app.nenelog.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.nenelog.data.db.NenelogDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = NenelogDatabase.Schema,
        name = DATABASE_NAME,
    )

    private companion object {
        const val DATABASE_NAME = "nenelog.db"
    }
}
