package com.tally.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tally.app.data.local.converter.Converters
import com.tally.app.data.local.dao.CircleDao
import com.tally.app.data.local.dao.GameDao
import com.tally.app.data.local.dao.PlayerDao
import com.tally.app.data.local.dao.SessionDao
import com.tally.app.data.local.entity.CircleEntity
import com.tally.app.data.local.entity.GameEntity
import com.tally.app.data.local.entity.PlayerEntity
import com.tally.app.data.local.entity.SessionEntity

/** Central Room database — ties every entity, converter, and DAO together. */
@Database(
    entities = [
        CircleEntity::class,
        PlayerEntity::class,
        GameEntity::class,
        SessionEntity::class,
    ],
    version = 6, // v6: Added lastSessionAt to CircleEntity
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TallyDatabase : RoomDatabase() {
    abstract fun circleDao(): CircleDao
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun sessionDao(): SessionDao
}
