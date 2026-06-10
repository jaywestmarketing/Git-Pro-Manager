package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class TokenManager(private val context: Context) {
    companion object {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val IGNORED_REPOS = stringSetPreferencesKey("ignored_repos")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GITHUB_TOKEN]
    }

    val ignoredReposFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[IGNORED_REPOS] ?: emptySet()
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[GITHUB_TOKEN] = token
        }
    }

    suspend fun addIgnoredRepo(repoId: String) {
        context.dataStore.edit { preferences ->
            val set = preferences[IGNORED_REPOS]?.toMutableSet() ?: mutableSetOf()
            set.add(repoId)
            preferences[IGNORED_REPOS] = set
        }
    }
    
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(GITHUB_TOKEN)
        }
    }
}
