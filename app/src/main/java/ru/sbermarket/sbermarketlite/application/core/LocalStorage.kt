package ru.sbermarket.sbermarketlite.application.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import ru.sbermarket.platform.LocalStorage


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sbermarket.Preferences")


class DataLocalStorage(
    val context: Context
): LocalStorage {
    override suspend fun setItem(key: String, value: String) {
        context.dataStore.edit {
            val stringKey = stringPreferencesKey(name = key)
            it[stringKey] = value
        }
    }

    override suspend fun getItem(key: String): String? {
        return context.dataStore.data.map {
            it[stringPreferencesKey(name = key)]
        }.firstOrNull()
    }
}