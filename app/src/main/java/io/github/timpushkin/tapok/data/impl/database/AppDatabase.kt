package io.github.timpushkin.tapok.data.impl.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan

@Database(
    entities = [Rule::class, Node::class, Scan::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun scanDao(): ScanDao
}
