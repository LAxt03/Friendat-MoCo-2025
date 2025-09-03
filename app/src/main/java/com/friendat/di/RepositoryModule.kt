package com.friendat.di

import com.friendat.data.repository.AuthRepository
import com.friendat.data.repository.AuthRepositoryImpl
import com.friendat.data.repository.FriendRepository
import com.friendat.data.repository.FriendRepositoryImpl
import com.friendat.data.repository.WifiLocationRepository
import com.friendat.data.repository.WifiLocationRepositoryImpl
import dagger.Binds
import dagger.Module
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
}


