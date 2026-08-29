package com.aktarjabed.jagallery.di

import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.domain.MediaOperationsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindMediaOperations(
        mediaOperationsImpl: MediaOperationsImpl
    ): MediaOperations
}
