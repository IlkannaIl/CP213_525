package com.example.lablearnandroid.architecture.mvc

/**
 * MVC Model: Simple data class to hold the counter value.
 * In MVC, the Model represents the data and business logic.
 */
data class MvcCounterModel(
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
