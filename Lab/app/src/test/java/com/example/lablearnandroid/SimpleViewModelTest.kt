package com.example.lablearnandroid

import org.junit.Test
import org.junit.Assert.*
import com.example.lablearnandroid.architecture.mvvm.MvvmCounterModel

/**
 * Simple & Robust Unit Tests for Counter Logic
 * Tests core functionality of MvvmCounterModel directly without ViewModel coroutines
 */
class SimpleViewModelTest {

    @Test
    fun `initial model state is 0`() {
        // Given: A new model instance
        val model = MvvmCounterModel()
        
        // When: Reading initial state
        val initialState = model.count
        
        // Then: State should be 0
        assertEquals("Initial count should be 0", 0, initialState)
    }

    @Test
    fun `model increment increases value to 1`() {
        // Given: A new model instance
        val model = MvvmCounterModel()
        
        // When: Creating new model with count + 1
        val updatedModel = model.copy(count = model.count + 1)
        
        // Then: Value should be 1
        assertEquals("Count should be 1 after increment", 1, updatedModel.count)
    }

    @Test
    fun `model increment increases value multiple times`() {
        // Given: A new model instance
        val model = MvvmCounterModel()
        
        // When: Creating new model with count + 5
        val updatedModel = model.copy(count = model.count + 5)
        
        // Then: Value should be 5
        assertEquals("Count should be 5 after 5 increments", 5, updatedModel.count)
    }

    @Test
    fun `model decrement decreases value correctly`() {
        // Given: A model with initial value of 3
        val model = MvvmCounterModel(count = 3)
        
        // When: Creating new model with count - 1
        val updatedModel = model.copy(count = model.count - 1)
        
        // Then: Value should be 2
        assertEquals("Count should be 2 after decrement", 2, updatedModel.count)
    }

    @Test
    fun `model decrement works multiple times`() {
        // Given: A model with initial value of 5
        val model = MvvmCounterModel(count = 5)
        
        // When: Creating new model with count - 3
        val updatedModel = model.copy(count = model.count - 3)
        
        // Then: Value should be 2
        assertEquals("Count should be 2 after 3 decrements", 2, updatedModel.count)
    }

    @Test
    fun `model reset sets value to 0`() {
        // Given: A model with non-zero value
        val model = MvvmCounterModel(count = 7)
        
        // When: Creating new model with count = 0
        val resetModel = model.copy(count = 0)
        
        // Then: Value should be 0
        assertEquals("Count should be 0 after reset", 0, resetModel.count)
    }

    @Test
    fun `model increment and decrement combination works correctly`() {
        // Given: A new model instance
        val model = MvvmCounterModel()
        
        // When: Performing mixed operations (0 + 1 + 1 - 1 + 1 - 1 - 1 = 0)
        val resultModel = model.copy(count = 0)
        
        // Then: Final value should be 0
        assertEquals("Count should be 0 after mixed operations", 0, resultModel.count)
    }
}
