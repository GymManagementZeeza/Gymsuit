package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.zeezaglobal.gymsuitapp.data.repository.AiSummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AiSummaryUiState {
    data object Idle : AiSummaryUiState
    data object Loading : AiSummaryUiState
    data class Success(val summary: String) : AiSummaryUiState
    data class Error(val message: String) : AiSummaryUiState
}

class DashboardViewModel(
    private val aiSummaryRepository: AiSummaryRepository
) : ViewModel() {

    private val _summaryState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val summaryState: StateFlow<AiSummaryUiState> = _summaryState.asStateFlow()

    fun loadAiSummary(forceRefresh: Boolean = false) {
        if (!forceRefresh && _summaryState.value is AiSummaryUiState.Loading) {
            return
        }

        _summaryState.value = AiSummaryUiState.Loading
        viewModelScope.launch {
            val result = aiSummaryRepository.fetchAiSummary()
            result.fold(
                onSuccess = { response ->
                    if (response.summary.isNotBlank()) {
                        _summaryState.value = AiSummaryUiState.Success(response.summary)
                    } else {
                        _summaryState.value = AiSummaryUiState.Error("No summary available")
                    }
                },
                onFailure = { error ->
                    _summaryState.value = AiSummaryUiState.Error(
                        error.localizedMessage ?: "Failed to generate AI health summary"
                    )
                }
            )
        }
    }
}

class DashboardViewModelFactory(
    private val repository: AiSummaryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
