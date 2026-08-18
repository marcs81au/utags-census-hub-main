package com.utags.androidpc.CensusHub.di

import com.utags.androidpc.CensusHub.data.repository.ReaderRepositoryImpl
import com.utags.androidpc.CensusHub.domain.repository.ReaderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindReaderRepository(impl: ReaderRepositoryImpl): ReaderRepository
}
