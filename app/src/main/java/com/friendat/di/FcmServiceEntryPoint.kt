package com.friendat.di
import com.friendat.data.sources.local.dao.FriendStatusDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FcmServiceEntryPoint {
    fun friendStatusDao(): FriendStatusDao
}