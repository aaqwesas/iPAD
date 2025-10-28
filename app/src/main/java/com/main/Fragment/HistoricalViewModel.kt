package com.main.Fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.main.Data.HistoricalRepository
import com.main.models.HistoricalBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext  // ADD THIS IMPORT

class HistoricalViewModel(private val repo: HistoricalRepository) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val data: List<HistoricalBar> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(symbol: String, limit: Int? = 200, startDate: String? = null, endDate: String? = null) {
        _state.value = UiState(loading = true)  // OK on any thread
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.get(symbol, limit, startDate, endDate) }
                .onSuccess { bars ->
                    withContext(Dispatchers.Main) {
                        _state.value = UiState(data = bars)
                    }
                }
                .onFailure { e ->
                    withContext(Dispatchers.Main) {
                        _state.value = UiState(error = e.message ?: "Unknown error")
                    }
                }
        }
    }
}