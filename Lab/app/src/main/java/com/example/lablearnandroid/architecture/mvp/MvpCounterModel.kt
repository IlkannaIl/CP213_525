package com.example.lablearnandroid.architecture.mvp

/**
 * MVP Model: Data layer.
 * In MVP, the Model represents the data and business logic.
 */
data class MvpCounterModel(
    var count: Int = 0
) {
    fun increment(): Int {
        count++
        return count
    }
    
    fun decrement(): Int {
        count--
        return count
    }
    
    fun reset(): Int {
        count = 0
        return count
    }
    
    fun getCount(): Int = count
}
