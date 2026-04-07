package com.example.lablearnandroid.architecture.mvi

/**
 * MVI State: Data class representing the immutable UI state.
 * In MVI, the State represents the current UI state and is immutable.
 */
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
