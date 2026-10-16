package com.smartjirani.core.network.di

import com.smartjirani.core.network.PesaPalClient
import com.smartjirani.core.network.PesaPalConfig
import com.smartjirani.core.network.SupabaseClientProvider
import com.smartjirani.core.network.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideSupabaseClientProvider(config: SupabaseConfig): SupabaseClientProvider = SupabaseClientProvider(config)

    @Provides
    @Singleton
    fun providePesaPalClient(config: PesaPalConfig): PesaPalClient = PesaPalClient(config)
}
