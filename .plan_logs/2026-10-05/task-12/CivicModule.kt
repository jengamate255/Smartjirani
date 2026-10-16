package com.smartjirani.feature.civic.di

import com.smartjirani.feature.civic.AiProviderResolver
import com.smartjirani.feature.civic.CivicAiConfig
import com.smartjirani.feature.civic.CivicAiProvider
import com.smartjirani.feature.civic.CivicRepository
import com.smartjirani.feature.civic.CivicRepositoryImpl
import com.smartjirani.feature.civic.KeywordMockCivicAiProvider
import com.smartjirani.feature.civic.SupabaseCivicAiProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CivicModule {
    @Binds
    @Singleton
    abstract fun bindCivicRepository(impl: CivicRepositoryImpl): CivicRepository

    @Binds
    @Singleton
    abstract fun bindCivicAiProvider(impl: SupabaseCivicAiProvider): CivicAiProvider

    companion object {
        @Provides
        @Singleton
        fun provideCivicAiConfig(): CivicAiConfig =
            CivicAiConfig(
                aiProvider = "gemini", // TODO: read from BuildConfig/env
                geminiKey = "", // TODO: read from BuildConfig/env
                geminiModel = "gemini-2.0-flash",
            )

        @Provides
        @Singleton
        fun provideKeywordMock(): KeywordMockCivicAiProvider = KeywordMockCivicAiProvider()

        @Provides
        @Singleton
        fun provideAiProviderResolver(
            supabase: SupabaseCivicAiProvider,
            mock: KeywordMockCivicAiProvider,
            cfg: CivicAiConfig,
        ): AiProviderResolver = AiProviderResolver(supabase, mock, cfg)
    }
}
