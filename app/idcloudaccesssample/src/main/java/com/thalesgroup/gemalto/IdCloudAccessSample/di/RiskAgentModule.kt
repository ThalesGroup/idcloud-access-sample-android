package com.thalesgroup.gemalto.IdCloudAccessSample.di

import com.thalesgroup.gemalto.IdCloudAccessSample.Configuration
import com.thalesgroup.gemalto.IdCloudAccessSample.agents.RiskAgent
import com.thalesgroup.gemalto.d1.D1Task
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class RiskAgentModule {

    @Provides
    @ViewModelScoped
    fun provideD1Task(): D1Task = D1Task.Builder()
        .setRiskURLString(Configuration.ND_URL)
        .setRiskClientID(Configuration.ND_CLIENT_ID)
        .build()

    @Provides
    @ViewModelScoped
    fun providesRiskAgent(d1Task: D1Task): RiskAgent = RiskAgent(d1Task)
}
