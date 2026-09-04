package app.nenelog.data

import app.cash.sqldelight.db.SqlDriver
import app.nenelog.data.db.NenelogDatabase

/** Platform entrypoints provide the correct SQLite driver while all queries stay shared. */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createNenelogDatabase(factory: DatabaseDriverFactory): NenelogDatabase =
    NenelogDatabase(factory.createDriver())
