package com.example.lablearnandroid.architecture.mvp

/**
 * MVP View: Interface defining UI methods.
 * In MVP, the View is an interface that the Presenter uses to update the UI.
 */
interface MvpCounterView {
    fun displayCount(count: Int)
    fun showIncrementResult(count: Int)
    fun showDecrementResult(count: Int)
    fun showResetResult(count: Int)
}
