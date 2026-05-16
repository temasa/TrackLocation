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
        SessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TrackDatabase: RoomDatabase() {

    abstract val trackDao: TrackDao
    abstract val locationDao: LocationDao
    abstract val sessionDao: SessionDao

    companion object {
        @Volatile
        var INSTANCE: TrackDatabase? = null

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
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
                database.execSQL(
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
                database.execSQL("ALTER TABLE `track` ADD COLUMN `startLocationId` INTEGER")
                database.execSQL("ALTER TABLE `track` ADD COLUMN `endLocationId` INTEGER")

                val cursor = database.query("SELECT `idx`, `timestamp`, `duration`, `pathPoints` FROM `track`")
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
                            database.execSQL(
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
                            val insertedId = latestInsertedRowId(database)
                            if (startLocationId == null) startLocationId = insertedId
                            endLocationId = insertedId
                        }

                        val migratedStartLocationId = startLocationId
                        val migratedEndLocationId = endLocationId
                        if (migratedStartLocationId != null && migratedEndLocationId != null) {
                            database.execSQL(
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
