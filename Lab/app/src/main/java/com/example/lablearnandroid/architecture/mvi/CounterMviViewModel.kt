package com.example.lablearnandroid.architecture.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVI ViewModel: Processes Intents and updates the State.
 * In MVI, the ViewModel processes Intents and emits new immutable States.
 */
class CounterMviViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(CounterState())
    val uiState: StateFlow<CounterState> = _uiState.asStateFlow()
    
    fun handleIntent(intent: CounterIntent) {
        viewModelScope.launch {
            when (intent) {
                is CounterIntent.Increment -> {
                    _uiState.value = _uiState.value.copy(count = _uiState.value.count + 1)
                }
                is CounterIntent.Decrement -> {
                    _uiState.value = _uiState.value.copy(count = _uiState.value.count - 1)
                }
                is CounterIntent.Reset -> {
                    _uiState.value = CounterState()
                }
            }
        }
    }
}
