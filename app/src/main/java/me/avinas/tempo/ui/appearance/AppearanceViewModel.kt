package me.avinas.tempo.ui.appearance

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val preferences: AppearancePreferencesRepository,
    private val transferService: AppearanceTransferService,
) : ViewModel() {
    val settings = preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        null,
    )
    val savedLooks = preferences.savedLooks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        emptyMap(),
    )

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    private val _transferState = MutableStateFlow(AppearanceTransferState())
    val transferState: StateFlow<AppearanceTransferState> = _transferState.asStateFlow()

    private var pendingExport: AppearanceSettings? = null

    fun saveLook(name: String, settings: AppearanceSettings) =
        write { preferences.saveLook(name, settings) }

    fun deleteLook(name: String) =
        write { preferences.deleteLook(name) }

    fun apply(settings: AppearanceSettings) =
        write { preferences.apply(settings) }

    fun prepareExport(settings: AppearanceSettings) {
        pendingExport = settings
        clearTransferFeedback()
    }

    fun cancelPreparedExport() {
        pendingExport = null
    }

    fun exportPrepared(uri: Uri) {
        val snapshot = pendingExport ?: return
        pendingExport = null
        transfer {
            transferService.write(uri, snapshot)
            null
        }
    }

    fun importAppearance(uri: Uri) {
        transfer { transferService.read(uri) }
    }

    fun consumeImportedAppearance() {
        _transferState.update { it.copy(imported = null) }
    }

    fun clearTransferFeedback() {
        _transferState.update { it.copy(failed = false, completed = false) }
    }

    private fun write(action: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        _saveFailed.value = false
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _saveFailed.value = true
            } finally {
                _saving.value = false
            }
        }
    }

    private fun transfer(action: suspend () -> AppearanceSettings?) {
        if (_transferState.value.busy) return
        _transferState.value = AppearanceTransferState(busy = true)
        viewModelScope.launch {
            try {
                val imported = action()
                _transferState.value = AppearanceTransferState(
                    imported = imported,
                    completed = true,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _transferState.value = AppearanceTransferState(failed = true)
            }
        }
    }

}

@androidx.compose.runtime.Immutable
data class AppearanceTransferState(
    val busy: Boolean = false,
    val imported: AppearanceSettings? = null,
    val completed: Boolean = false,
    val failed: Boolean = false,
)
