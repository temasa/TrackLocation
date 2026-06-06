package com.kolee.tracklocation.data.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackEntity::class,
        LocationEntity::class,
        SessionEntity::class,
        ObservedEventEntity::class,
        AllowlistRuleEntity::class,
        ObdSampleEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TrackDatabase: RoomDatabase() {

    abstract val trackDao: TrackDao
    abstract val locationDao: LocationDao
    abstract val sessionDao: SessionDao
    abstract val observerEventDao: ObserverEventDao
    abstract val allowlistRuleDao: AllowlistRuleDao
    abstract val obdSampleDao: ObdSampleDao

    companion object {
        @Volatile
        var INSTANCE: TrackDatabase? = null

        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `observer_event` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `activityName` TEXT,
                        `eventType` TEXT NOT NULL,
                        `firstSeenAt` INTEGER NOT NULL,
                        `lastSeenAt` INTEGER NOT NULL,
                        `textSummary` TEXT,
                        `repeatCount` INTEGER NOT NULL DEFAULT 1,
                        `treeSnapshot` TEXT,
                        `truncationMetadata` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `allowlist_rule` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `matchType` TEXT NOT NULL,
                        `pattern` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_observer_event_packageName` ON `observer_event` (`packageName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_observer_event_lastSeenAt` ON `observer_event` (`lastSeenAt`)")
            }
        }

        private val MIGRATION_3_4 = object: Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `obd_sample` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        `rpm` INTEGER,
                        `obdSpeedKmh` INTEGER,
                        `fuelRateLph` REAL,
                        `mafGramsPerSecond` REAL,
                        `fuelRateSource` TEXT NOT NULL,
                        `adapterElapsedMs` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_obd_sample_timestampMs` ON `obd_sample` (`timestampMs`)")
            }
        }

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `location_log` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `accuracyMeters` REAL,
                        `speedMetersPerSecond` REAL,
                        `bearingDegrees` REAL,
                        `altitudeMeters` REAL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recording_session` (
                        `id` TEXT NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `startLocationId` INTEGER,
                        `endLocationId` INTEGER,
                        `distanceMeters` REAL NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        `pointCount` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // Migration for 'track' table to handle 'distance' type mismatch (REAL -> INTEGER)
                // and add new columns 'startLocationId' and 'endLocationId'.
                db.execSQL(
                    """
                    CREATE TABLE `track_new` (
                        `idx` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `distance` INTEGER NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `pathPoints` TEXT NOT NULL,
                        `startLocationId` INTEGER,
                        `endLocationId` INTEGER
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `track_new` (`idx`, `timestamp`, `distance`, `duration`, `pathPoints`)
                    SELECT `idx`, `timestamp`, CAST(`distance` AS INTEGER), `duration`, `pathPoints` FROM `track`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `track`")
                db.execSQL("ALTER TABLE `track_new` RENAME TO `track`")

                val cursor = db.query("SELECT `idx`, `timestamp`, `duration`, `pathPoints` FROM `track`")
                try {
                    while (cursor.moveToNext()) {
                        val trackId = cursor.getInt(0)
                        val timestamp = cursor.getLong(1)
                        val duration = cursor.getLong(2)
                        val pathPoints = cursor.getString(3).orEmpty()
                        val points = parseLegacyPathPoints(pathPoints)
                        if (points.isEmpty()) continue

                        var startLocationId: Long? = null
                        var endLocationId: Long? = null
                        val interval = if (points.size > 1 && duration > 0L) {
                            duration / (points.size - 1)
                        } else {
                            0L
                        }

                        points.forEachIndexed { index, point ->
                            db.execSQL(
                                """
                                INSERT INTO `location_log` (
                                    `timestamp`,
                                    `latitude`,
                                    `longitude`,
                                    `accuracyMeters`,
                                    `speedMetersPerSecond`,
                                    `bearingDegrees`,
                                    `altitudeMeters`
                                ) VALUES (?, ?, ?, NULL, NULL, NULL, NULL)
                                """.trimIndent(),
                                arrayOf(timestamp + (interval * index), point.first, point.second)
                            )
                            val insertedId = latestInsertedRowId(db)
                            if (startLocationId == null) startLocationId = insertedId
                            endLocationId = insertedId
                        }

                        val migratedStartLocationId = startLocationId
                        val migratedEndLocationId = endLocationId
                        if (migratedStartLocationId != null && migratedEndLocationId != null) {
                            db.execSQL(
                                "UPDATE `track` SET `startLocationId` = ?, `endLocationId` = ? WHERE `idx` = ?",
                                arrayOf(migratedStartLocationId, migratedEndLocationId, trackId)
                            )
                        }
                    }
                } finally {
                    cursor.close()
                }
            }

            private fun parseLegacyPathPoints(pathPoints: String): List<Pair<Double, Double>> {
                return pathPoints
                    .split("/")
                    .mapNotNull { rawPoint ->
                        val parts = rawPoint.split(",")
                        if (parts.size != 2) return@mapNotNull null
                        val latitude = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                        val longitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                        latitude to longitude
                    }
            }

            private fun latestInsertedRowId(database: SupportSQLiteDatabase): Long {
                val idCursor = database.query("SELECT last_insert_rowid()")
                return try {
                    if (idCursor.moveToFirst()) idCursor.getLong(0) else 0L
                } finally {
                    idCursor.close()
                }
            }
        }

        fun getDatabase(context: Context): TrackDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context,
                    TrackDatabase::class.java,
                    "track_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
