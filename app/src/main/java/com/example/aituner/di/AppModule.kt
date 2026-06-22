package com.example.aituner.di

import android.content.Context
import com.example.aituner.ai.AiProviderFactory
import com.example.aituner.ai.AiSettingsRepository
import com.example.aituner.audio.TunerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTunerEngine(): TunerEngine = TunerEngine()

    @Provides
    @Singleton
    fun provideAiProviderFactory(): AiProviderFactory = AiProviderFactory()

    @Provides
    @Singleton
    fun provideAiSettingsRepository(
        @ApplicationContext context: Context
    ): AiSettingsRepository = AiSettingsRepository(context)
}
