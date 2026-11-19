package com.nexus.gamelauncher.feature.library

import android.net.Uri
import com.nexus.gamelauncher.audio.Cue
import com.nexus.gamelauncher.audio.ToruvaAudio
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repository: LibraryRepository,
    private val savedState: SavedStateHandle,
    private val audio: ToruvaAudio
) : ViewModel() {
    private val discovery = MutableStateFlow(Discovery())
    private var refreshJob: Job? = null
    private val messages = Channel<String>(Channel.BUFFERED)
    val feedback = messages.receiveAsFlow()
    val state = combine(discovery, repository.entries.catch {
        messages.send("No se pudo leer la biblioteca guardada.")
        emit(emptyList())
    }) { scan, entries -> LibraryState(scan, entries.associateBy(LibraryEntry::packageName)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    val homeSelection = savedState.getStateFlow<String?>("homeSelection", null)

    init {
        save("No se pudieron revisar las imágenes guardadas.") { repository.cleanArtwork() }
    }

    fun selectForHome(packageName: String) {
        savedState["homeSelection"] = packageName
    }

    fun artworkFile(name: String?) = repository.artworkFile(name)

    fun setDisplayName(app: InstalledApp, name: String) = save("No se pudo guardar el nombre.") {
        repository.setDisplayName(app.packageName, name)
    }

    fun setArtworkFrame(app: InstalledApp, kind: ArtworkKind, frame: ArtworkFrame) =
        save("No se pudo guardar el encuadre.") { repository.setArtworkFrame(app.packageName, kind, frame) }

    fun changeArtwork(app: InstalledApp, kind: ArtworkKind?, uri: Uri? = null) =
        save("No se pudo actualizar la imagen. Elige una imagen compatible de hasta 32 MB.") {
            repository.changeArtwork(app.packageName, kind, uri)
        }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            discovery.value = discovery.value.copy(loading = true, failed = false)
            try {
                discovery.value = Discovery(repository.discover(), loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                discovery.value = discovery.value.copy(loading = false, failed = true)
            }
        }
    }

    suspend fun icon(packageName: String) = repository.icon(packageName)

    fun setIncluded(app: InstalledApp, included: Boolean) = save("No se pudo actualizar la biblioteca.") {
        repository.setIncluded(app.packageName, included)
    }

    fun toggleFavorite(app: InstalledApp) = save("No se pudo actualizar el favorito.") {
        repository.toggleFavorite(app.packageName)
    }

    fun launch(app: InstalledApp) {
        if (repository.launch(app)) {
            audio.play(Cue.Launch)
            save("Se abrió la aplicación, pero no se pudo guardar en Recientes.") {
                repository.recordLaunch(app)
            }
        } else {
            messages.trySend("No se pudo abrir ${app.name}.")
            refresh()
        }
    }

    private fun save(error: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                messages.send(error)
            }
        }
    }
}
