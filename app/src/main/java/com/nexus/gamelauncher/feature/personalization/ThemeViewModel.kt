package com.nexus.gamelauncher.feature.personalization

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NexusTheme(val label: String, val accent: Long, val glow: Long) {
    Dynamic("Dinámico", 0xFF62D6FF, 0xFFA680FF),
    Blue("Nexus Azul", 0xFF55BFFF, 0xFF3F65FF),
    Purple("Púrpura Neón", 0xFFC68CFF, 0xFF9455FF),
    Red("Rojo Gamer", 0xFFFF8E98, 0xFFFF483E),
    Minimal("Minimalista", 0xFFD5E1F1, 0xFF8292AD),
    Retro("Retro", 0xFFFFCA75, 0xFFF17346)
}

private val Context.preferences by preferencesDataStore("nexus_preferences")

class ThemeRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.preferences
    private val key = stringPreferencesKey("theme")
    val theme = store.data.map { preferences ->
        NexusTheme.entries.firstOrNull { it.name == preferences[key] } ?: NexusTheme.Dynamic
    }

    suspend fun set(theme: NexusTheme) {
        store.edit { it[key] = theme.name }
    }
}

internal fun startupTheme(source: Flow<NexusTheme>, scope: CoroutineScope): StateFlow<NexusTheme?> =
    source.stateIn(scope, SharingStarted.Eagerly, null)

@HiltViewModel
class ThemeViewModel @Inject constructor(private val repository: ThemeRepository) : ViewModel() {
    private val messages = Channel<String>(Channel.BUFFERED)
    val feedback = messages.receiveAsFlow()
    val theme = startupTheme(repository.theme.catch { error ->
        if (error !is IOException) throw error
        messages.send("No se pudo leer el tema guardado.")
        emit(NexusTheme.Dynamic)
    }, viewModelScope)

    fun choose(theme: NexusTheme) {
        viewModelScope.launch {
            try {
                repository.set(theme)
            } catch (_: IOException) {
                messages.send("No se pudo guardar el tema.")
            }
        }
    }
}
