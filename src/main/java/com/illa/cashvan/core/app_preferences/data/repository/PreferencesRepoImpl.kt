package com.illa.cashvan.core.app_preferences.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.illa.cashvan.core.app_preferences.domain.repository.PreferencesRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cash_van_preferences")

class PreferencesRepoImpl(
    private val context: Context
) : PreferencesRepo {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_cash_van_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun <T> setValue(
        key: String,
        value: T,
        serializer: KSerializer<T>,
        encrypt: Boolean
    ) {
        val jsonString = json.encodeToString(serializer, value)
        if (encrypt) {
            encryptedSharedPreferences.edit()
                .putString(key, jsonString)
                .apply()
        } else {
            setValue(key, jsonString)
        }
    }

    override suspend fun setValue(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun setValue(key: String, value: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[stringSetPreferencesKey(key)] = value
        }
    }

    override suspend fun setValue(key: String, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun setValue(key: String, value: Int) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey(key)] = value
        }
    }

    override suspend fun setValue(key: String, value: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey(key)] = value
        }
    }

    override suspend fun setValue(key: String, value: Float) {
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey(key)] = value
        }
    }

    override suspend fun setValue(key: String, value: Double) {
        context.dataStore.edit { preferences ->
            preferences[doublePreferencesKey(key)] = value
        }
    }

    override suspend fun <T> getValue(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
        decrypt: Boolean
    ): Flow<T> {
        return if (decrypt) {
            kotlinx.coroutines.flow.flow {
                val jsonString = encryptedSharedPreferences.getString(key, null)
                val value = if (jsonString != null) {
                    try {
                        json.decodeFromString(serializer, jsonString)
                    } catch (e: Exception) {
                        defaultValue
                    }
                } else {
                    defaultValue
                }
                emit(value)
            }
        } else {
            getValue(key, json.encodeToString(serializer, defaultValue)).map { jsonString ->
                try {
                    json.decodeFromString(serializer, jsonString)
                } catch (e: Exception) {
                    defaultValue
                }
            }
        }
    }

    override suspend fun getValue(key: String, defaultValue: String): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun getValue(key: String, defaultValue: Set<String>): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[stringSetPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun getValue(key: String, defaultValue: Boolean): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun getValue(key: String, defaultValue: Int): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[intPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun getValue(key: String, defaultValue: Long): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[longPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun getValue(key: String, defaultValue: Float): Flow<Float> {
        return context.dataStore.data.map { preferences ->
            preferences[floatPreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun getValue(key: String, defaultValue: Double): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[doublePreferencesKey(key)] ?: defaultValue
        }
    }

    override suspend fun doesPreferenceExist(key: String): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences.contains(stringPreferencesKey(key)) ||
            encryptedSharedPreferences.contains(key)
        }
    }

    override suspend fun clearPreference(key: String) {
        context.dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
            preferences.remove(stringSetPreferencesKey(key))
            preferences.remove(booleanPreferencesKey(key))
            preferences.remove(intPreferencesKey(key))
            preferences.remove(longPreferencesKey(key))
            preferences.remove(floatPreferencesKey(key))
            preferences.remove(doublePreferencesKey(key))
        }

        encryptedSharedPreferences.edit().remove(key).apply()
    }

    override suspend fun clearAppData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        encryptedSharedPreferences.edit().clear().apply()
    }
}