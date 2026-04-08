package com.example.lablearnandroid.architecture.mvp

/**
 * MVP Presenter: Handles logic and updates the View via the interface.
 * In MVP, the Presenter acts as the middleman between Model and View.
 */
class MvpCounterPresenter(
    private val model: MvpCounterModel,
    private val view: MvpCounterView
) {
    
    init {
        // Initialize the view with current count
        view.displayCount(model.count)
    }
    
    fun onIncrementClicked() {
        val newCount = model.increment()
        view.showIncrementResult(newCount)
    }
    
    fun onDecrementClicked() {
        val newCount = model.decrement()
        view.showDecrementResult(newCount)
    }
    
    fun onResetClicked() {
        val newCount = model.reset()
        view.showResetResult(newCount)
    }
    
    fun getCurrentCount(): Int {
        return model.count
    }
}
