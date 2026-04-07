package com.example.lablearnandroid.architecture.mvi

/**
 * MVI Intent: Sealed class for user actions.
 * In MVI, Intents represent user actions or system events that can modify the state.
 */
sealed class CounterIntent {
    object Increment : CounterIntent()
    object Decrement : CounterIntent()
    object Reset : CounterIntent()
}
