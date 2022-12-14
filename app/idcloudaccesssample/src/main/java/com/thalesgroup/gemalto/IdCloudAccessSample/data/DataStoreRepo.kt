package com.thalesgroup.gemalto.IdCloudAccessSample.data

interface DataStoreRepo {
    suspend fun putInteger(key: String, value: Int)
    suspend fun getInteger(key: String): Int?
    suspend fun putString(key: String, value: String)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun getString(key: String): String?
    suspend fun clearPreferences(key: String)
}
