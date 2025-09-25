package com.illa.cashvan.core.app_preferences.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

interface PreferencesRepo {
    suspend fun <T> setValue(
        key: String,
        value: T,
        serializer: KSerializer<T>,
        encrypt: Boolean
    )
    suspend fun setValue(key: String, value: String)
    suspend fun setValue(key: String, value: Set<String>)
    suspend fun setValue(key: String, value: Boolean)
    suspend fun setValue(key: String, value: Int)
    suspend fun setValue(key: String, value: Long)
    suspend fun setValue(key: String, value: Float)
    suspend fun setValue(key: String, value: Double)

    suspend fun <T> getValue(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
        decrypt: Boolean
    ): Flow<T>

    suspend fun getValue(key: String, defaultValue: String): Flow<String>
    suspend fun getValue(key: String, defaultValue: Set<String>): Flow<Set<String>>
    suspend fun getValue(key: String, defaultValue: Boolean): Flow<Boolean>
    suspend fun getValue(key: String, defaultValue: Int): Flow<Int>
    suspend fun getValue(key: String, defaultValue: Long): Flow<Long>
    suspend fun getValue(key: String, defaultValue: Float): Flow<Float>
    suspend fun getValue(key: String, defaultValue: Double): Flow<Double>

    suspend fun doesPreferenceExist(key: String): Flow<Boolean>
    suspend fun clearPreference(key: String)
    suspend fun clearAppData()
}