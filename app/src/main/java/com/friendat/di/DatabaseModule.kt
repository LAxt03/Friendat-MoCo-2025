package com.friendat.di

import android.content.Context
import androidx.room.Room
import com.friendat.data.sources.local.AppDatabase
import com.friendat.data.sources.local.MIGRATION_1_2
import com.friendat.data.sources.local.dao.FriendStatusDao
import com.friendat.data.sources.local.dao.WifiLocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Stellt sicher, dass die DB als Singleton bereitgestellt wird
object DatabaseModule {

    @Provides
    @Singleton // Eine einzige Instanz der Datenbank für die gesamte App-Lebensdauer
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "friendat_database" //Room Datenbank
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton //
    fun provideWifiLocationDao(appDatabase: AppDatabase): WifiLocationDao {
        return appDatabase.wifiLocationDao()
    }

    @Provides
    @Singleton
    fun provideFriendStatusDao(appDatabase: AppDatabase): FriendStatusDao {
        return appDatabase.friendStatusDao()
    }


}