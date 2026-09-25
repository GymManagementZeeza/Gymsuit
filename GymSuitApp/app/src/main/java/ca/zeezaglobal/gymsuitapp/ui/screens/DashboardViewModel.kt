package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.zeezaglobal.gymsuitapp.data.summary.WellnessSummaryGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SummaryUiState {
    data object Idle : SummaryUiState
    data object Loading : SummaryUiState
    data class Success(val summary: String) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

class DashboardViewModel(
    private val summaryGenerator: WellnessSummaryGenerator
) : ViewModel() {

    private val _summaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val summaryState: StateFlow<SummaryUiState> = _summaryState.asStateFlow()

    fun loadSummary(forceRefresh: Boolean = false) {
        if (!forceRefresh && _summaryState.value is SummaryUiState.Loading) {
            return
        }

        _summaryState.value = SummaryUiState.Loading
        viewModelScope.launch {
            try {
                val summary = summaryGenerator.generate()
                if (summary.isNotBlank()) {
                    _summaryState.value = SummaryUiState.Success(summary)
                } else {
                    _summaryState.value = SummaryUiState.Error("No summary available")
                }
            } catch (e: Exception) {
                _summaryState.value = SummaryUiState.Error(
                    e.localizedMessage ?: "Couldn't generate your wellness summary"
                )
            }
        }
    }
}

class DashboardViewModelFactory(
    private val summaryGenerator: WellnessSummaryGenerator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(summaryGenerator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
