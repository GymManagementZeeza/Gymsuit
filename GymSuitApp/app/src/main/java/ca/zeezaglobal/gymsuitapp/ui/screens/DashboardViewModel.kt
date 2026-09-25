package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.zeezaglobal.gymsuitapp.data.repository.AiSummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

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

    private var activeJob: Job? = null
    private var currentDate: LocalDate = LocalDate.now()

    /**
     * Called whenever a date is selected or navigated to.
     * 1. If cached, immediately displays cached summary without triggering API.
     * 2. If not cached (or expired for today), immediately starts loading animation
     *    and waits for 3 seconds of hold before calling the API.
     * 3. If user rapidly changes dates, previous pending timer/request is cancelled immediately.
     */
    fun onDateSelected(date: LocalDate, forceRefresh: Boolean = false) {
        currentDate = date
        activeJob?.cancel()

        // Check local cache first
        if (!forceRefresh) {
            val cached = aiSummaryRepository.getCachedSummary(date)
            if (cached != null) {
                _summaryState.value = AiSummaryUiState.Success(cached)
                return
            }
        }

        // Start loading animation immediately as requested
        _summaryState.value = AiSummaryUiState.Loading

        activeJob = viewModelScope.launch {
            // Wait 3 seconds hold on the page before firing API call
            delay(3000L)

            val result = aiSummaryRepository.getSummaryForDate(date, forceRefresh = forceRefresh)
            result.fold(
                onSuccess = { response ->
                    if (response.summary.isNotBlank()) {
                        _summaryState.value = AiSummaryUiState.Success(response.summary)
                    } else {
                        _summaryState.value = AiSummaryUiState.Error("No summary available for this date")
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

    fun retryCurrentDate() {
        onDateSelected(currentDate, forceRefresh = true)
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
