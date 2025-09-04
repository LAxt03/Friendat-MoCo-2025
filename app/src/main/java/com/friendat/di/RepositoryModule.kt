package com.friendat.di

import com.friendat.data.repository.AuthRepository
import com.friendat.data.repository.AuthRepositoryImpl
import com.friendat.data.repository.FriendRepository
import com.friendat.data.repository.FriendRepositoryImpl
import com.friendat.data.repository.LastStatusRepository // Importiere dein Objekt
import com.friendat.data.repository.WifiLocationRepository
import com.friendat.data.repository.WifiLocationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides // Import für @Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWifiLocationRepository(
        wifiLocationRepositoryImpl: WifiLocationRepositoryImpl
    ): WifiLocationRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(
        friendRepositoryImpl: FriendRepositoryImpl
    ): FriendRepository

    // Companion object, um @Provides-Methoden in einem abstrakten Modul zu ermöglichen
    companion object { // Notwendig, wenn RepositoryModule eine abstract class ist
        @Provides
        @Singleton
        fun provideLastStatusRepository(): LastStatusRepository {
            return LastStatusRepository // Da es ein Kotlin 'object' ist
        }
    }
}
