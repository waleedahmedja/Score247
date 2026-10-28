package com.waleedahmedja.score247.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.waleedahmedja.score247.data.model.MatchRecord
import com.waleedahmedja.score247.data.model.MatchState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

/** One DataStore file per process — must be declared at top level, exactly once. */
private val Context.matchStore: DataStore<Preferences> by preferencesDataStore("score247_match")

/**
 * Persists the whole [MatchState] as a single JSON string so a match survives
 * app restarts and process death. Persistence is best-effort: a failed read or
 * write must never stop the scorer from carrying on.
 */
class MatchDataStore(context: Context) {

    private val dataStore = context.applicationContext.matchStore
    private val key = stringPreferencesKey("state")              // the match being played
    private val historyKey = stringPreferencesKey("history")     // finished matches
    private val historySerializer = ListSerializer(MatchRecord.serializer())

    private val json = Json {
        ignoreUnknownKeys = true   // a newer/older app version may have written extra fields
        encodeDefaults    = true   // always write defaults so old readers see complete objects
    }

    /** Emits the saved match, or null if nothing is stored or the data can't be read. */
    val flow: Flow<MatchState?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[key]?.let { raw ->
                // Corrupt/incompatible JSON → treat as "no saved match" instead of crashing.
                try { json.decodeFromString(MatchState.serializer(), raw) } catch (e: Exception) { null }
            }
        }

    suspend fun save(state: MatchState) {
        try {
            dataStore.edit { it[key] = json.encodeToString(MatchState.serializer(), state) }
        } catch (e: CancellationException) {
            throw e                    // never swallow cancellation
        } catch (e: Exception) {
            // Best-effort: the in-memory state is still correct, we'll retry on the next change.
        }
    }

    // ── Match history ─────────────────────────────────────────────────────────

    /** All finished matches, newest first. Empty if none are saved or the data is unreadable. */
    val historyFlow: Flow<List<MatchRecord>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[historyKey]?.let { raw ->
                try { json.decodeFromString(historySerializer, raw) } catch (e: Exception) { null }
            } ?: emptyList()
        }

    suspend fun saveHistory(records: List<MatchRecord>) {
        try {
            dataStore.edit { it[historyKey] = json.encodeToString(historySerializer, records) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best-effort, see save().
        }
    }
}
