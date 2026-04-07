package com.example.lablearnandroid.architecture.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MvvmCounterViewModel : ViewModel() {
    
    // Model instance
    private var model = MvvmCounterModel()
    
    private val _count = MutableStateFlow(model.count)
    val count: StateFlow<Int> = _count.asStateFlow()
    
    fun increment() {
        viewModelScope.launch {
            model = model.copy(count = model.count + 1)
            _count.value = model.count
        }
    }
    
    fun decrement() {
        viewModelScope.launch {
            model = model.copy(count = model.count - 1)
            _count.value = model.count
        }
    }
    
    fun reset() {
        viewModelScope.launch {
            model = model.copy(count = 0)
            _count.value = model.count
        }
    }
}
