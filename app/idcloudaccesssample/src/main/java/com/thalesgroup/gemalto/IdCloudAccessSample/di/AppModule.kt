package com.thalesgroup.gemalto.IdCloudAccessSample.di

import android.content.Context
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepo
import com.thalesgroup.gemalto.IdCloudAccessSample.data.DataStoreRepoImpl
import com.thalesgroup.gemalto.d1.icampoc.OIDCAgent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun providesOIDCAgent(@ApplicationContext context: Context): OIDCAgent = OIDCAgent(context)

    // a singleton object of DatastoreRepoImpl will be created in the dependency graph
    @Singleton
    @Provides
    fun providesDataStoreRepo(@ApplicationContext context: Context): DataStoreRepo = DataStoreRepoImpl(context)
}
