package com.inialpha.executiveai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.executiveai.di.AppContainer
import com.inialpha.executiveai.domain.model.EmailMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class ImportantEmailsUiState(
    val isLoading: Boolean = true,
    val emails: List<EmailMessage> = emptyList(),
)

class ImportantEmailsViewModel(container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ImportantEmailsUiState())
    val state: StateFlow<ImportantEmailsUiState> = _state.asStateFlow()

    init {
        container.emailRepository.observeImportant()
            .onEach { _state.value = ImportantEmailsUiState(isLoading = false, emails = it) }
            .launchIn(viewModelScope)
    }
}
