package com.tally.app.di

import com.tally.app.data.repository.CircleRepositoryImpl
import com.tally.app.data.repository.GameRepositoryImpl
import com.tally.app.data.repository.PlayerRepositoryImpl
import com.tally.app.data.repository.SessionRepositoryImpl
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.domain.repository.GameRepository
import com.tally.app.domain.repository.PlayerRepository
import com.tally.app.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds each repository interface to its Room-backed implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCircleRepository(impl: CircleRepositoryImpl): CircleRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
