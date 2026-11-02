package com.managesoundmode.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.data.repository.SoundModeRepository
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState

  data class Ready(
    val currentMode: SoundMode,
    val streams: VolumeStreams,
    val hasDndPermission: Boolean,
    val profiles: List<SoundProfile>,
    val activeProfileId: String? = null,
    val activeTimer: TimedModeSession? = null,
    val statusMessage: String? = null,
  ) : MainScreenUiState

  data class Error(val message: String) : MainScreenUiState
}

class MainScreenViewModel(
  private val repository: SoundModeRepository,
) : ViewModel() {

  private val feedbackMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<MainScreenUiState> = combine(
    repository.snapshotStream,
    repository.profilesStream,
    feedbackMessage,
  ) { snapshot: DeviceSoundSnapshot, profiles: List<SoundProfile>, message: String? ->
    MainScreenUiState.Ready(
      currentMode = snapshot.currentMode,
      streams = snapshot.streams,
      hasDndPermission = snapshot.hasDndPermission,
      profiles = profiles,
      activeProfileId = snapshot.activeProfileId,
      activeTimer = snapshot.activeTimer,
      statusMessage = message,
    ) as MainScreenUiState
  }
    .catch { throwable ->
      emit(MainScreenUiState.Error(throwable.message ?: "Erro inesperado ao carregar estado de som"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = MainScreenUiState.Loading,
    )

  fun onRefreshSystemState() {
    repository.refreshSystemState()
  }

  fun onSelectMode(mode: SoundMode) {
    viewModelScope.launch {
      repository.setSoundMode(mode)
        .onSuccess {
          feedbackMessage.value = "Modo alterado para ${mode.title}"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Não foi possível alterar o modo de som"
        }
    }
  }

  fun onStreamVolumeChange(type: VolumeStreamType, percent: Int) {
    viewModelScope.launch {
      repository.setStreamVolume(type, percent)
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao ajustar volume de ${type.label}"
        }
    }
  }

  fun onActivateProfile(profile: SoundProfile) {
    viewModelScope.launch {
      repository.activateProfile(profile)
        .onSuccess {
          feedbackMessage.value = "Perfil '${profile.name}' ativado"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Não foi possível ativar o perfil '${profile.name}'"
        }
    }
  }

  fun onStartTimer(targetMode: SoundMode, durationMinutes: Int) {
    viewModelScope.launch {
      repository.startTimedSession(targetMode = targetMode, durationMinutes = durationMinutes)
        .onSuccess {
          feedbackMessage.value = "Modo ${targetMode.title} ativado por $durationMinutes min"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao iniciar temporizador"
        }
    }
  }

  fun onCancelTimer() {
    viewModelScope.launch {
      repository.cancelOrRestoreTimedSession()
        .onSuccess {
          feedbackMessage.value = "Temporizador encerrado e modo restaurado"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao restaurar modo anterior"
        }
    }
  }

  fun onSaveCurrentAsProfile(name: String, description: String) {
    viewModelScope.launch {
      repository.saveCurrentAsProfile(name, description)
        .onSuccess { profile ->
          feedbackMessage.value = "Perfil '${profile.name}' salvo com sucesso"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao salvar perfil"
        }
    }
  }

  fun onDismissMessage() {
    feedbackMessage.value = null
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val application =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManageSoundApplication
        MainScreenViewModel(repository = application.container.soundModeRepository)
      }
    }
  }
}
