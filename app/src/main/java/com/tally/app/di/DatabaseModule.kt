package com.tally.app.di

import android.content.Context
import androidx.room.Room
import com.tally.app.data.local.TallyDatabase
import com.tally.app.data.local.dao.CircleDao
import com.tally.app.data.local.dao.GameDao
import com.tally.app.data.local.dao.PlayerDao
import com.tally.app.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the singleton [TallyDatabase] and each DAO to the Hilt graph. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TallyDatabase =
        Room.databaseBuilder(context, TallyDatabase::class.java, "tally_database")
            .build()

    @Provides
    @Singleton
    fun provideCircleDao(database: TallyDatabase): CircleDao = database.circleDao()

    @Provides
    @Singleton
    fun providePlayerDao(database: TallyDatabase): PlayerDao = database.playerDao()

    @Provides
    @Singleton
    fun provideGameDao(database: TallyDatabase): GameDao = database.gameDao()

    @Provides
    @Singleton
    fun provideSessionDao(database: TallyDatabase): SessionDao = database.sessionDao()
}
