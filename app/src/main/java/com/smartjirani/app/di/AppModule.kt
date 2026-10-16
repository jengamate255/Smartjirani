package com.smartjirani.app.di

import com.smartjirani.core.network.PesaPalConfig
import com.smartjirani.core.network.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig =
        SupabaseConfig(
            url = com.smartjirani.app.BuildConfig.SUPABASE_URL,
            anonKey = com.smartjirani.app.BuildConfig.SUPABASE_ANON_KEY,
        )

    @Provides
    @Singleton
    fun providePesaPalConfig(): PesaPalConfig =
        PesaPalConfig(
            consumerKey = com.smartjirani.app.BuildConfig.PESAPAL_CONSUMER_KEY,
            consumerSecret = com.smartjirani.app.BuildConfig.PESAPAL_CONSUMER_SECRET,
            environment = com.smartjirani.app.BuildConfig.PESAPAL_ENV,
            callbackUrl = com.smartjirani.app.BuildConfig.PESAPAL_CALLBACK_URL,
        )
}
