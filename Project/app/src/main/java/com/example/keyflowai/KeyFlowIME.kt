package com.example.keyflowai

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.text.TextUtils
import kotlinx.coroutines.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import android.os.Handler
import android.os.Looper

class KeyFlowIME : InputMethodService() {

    private lateinit var keyboardView: CustomKeyboardView
    private lateinit var refineButton: Button
    private lateinit var culturalAlert: View
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRefining = false
    
    // UI Handler for main thread operations
    private val mainHandler = Handler(Looper.getMainLooper())

    //  8 5 9 Gemini AI
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyDlq8pc9fC79_I08kwLy6hGJxkcjwJM_eM"
    )

    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.input_method, null)
        
        // Find the actual keyboard view within the inflated layout
        keyboardView = rootView.findViewById(R.id.keyboard_view)

        refineButton = rootView.findViewById(R.id.btn_refine)
        culturalAlert = rootView.findViewById(R.id.cultural_alert_badge)

        refineButton?.setOnClickListener {
            handleRefine()
        }
        
        // Setup keyboard button clicks
        setupKeyboardButtons()

        return rootView
    }

    private fun handleRefine() {
        if (isRefining) return // Prevent multiple simultaneous requests
        
        val ic = currentInputConnection
        val selectedText = ic.getSelectedText(0)?.toString() ?: ""

        if (selectedText.isEmpty()) {
            showToast("Please select text to refine")
            return
        }

        // Start loading state
        isRefining = true
        setLoadingState(true)

        // Launch coroutine for AI processing
        coroutineScope.launch {
            try {
                val refinedText = refineTextWithAI(selectedText)
                if (refinedText.isNotEmpty()) {
                    // Replace selected text with refined version
                    ic.commitText(refinedText, 1)
                    showToast("Text refined successfully")
                } else {
                    showToast("Failed to refine text")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                // Reset loading state
                isRefining = false
                setLoadingState(false)
            }
        }
    }

    private suspend fun refineTextWithAI(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Please refine and improve the following text. Make it more professional, clear, and grammatically correct. 
                    If the text is in Thai, improve the Thai. If in English, improve the English.
                    Only return the refined text without any explanation.
                    
                    Original text: $text
                """.trimIndent()
                
                val response = generativeModel.generateContent(prompt)
                response.text?.trim() ?: ""
            } catch (e: Exception) {
                throw Exception("AI processing failed: ${e.message}")
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        mainHandler.post {
            refineButton.isEnabled = !isLoading
            refineButton.text = if (isLoading) "Refining..." else "Refine AI"
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(this@KeyFlowIME, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupKeyboardButtons() {
        // Find all buttons in the custom keyboard layout and set click listeners
        val keyboardButtons = listOf(
            "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
            "A", "S", "D", "F", "G", "H", "J", "K", "L",
            "Z", "X", "C", "V", "B", "N", "M", ",", ".",
            "123", "SPACE", "DEL"
        )
        
        keyboardButtons.forEach { buttonText ->
            val buttonId = when (buttonText) {
                "," -> resources.getIdentifier("btn_comma", "id", packageName)
                "." -> resources.getIdentifier("btn_period", "id", packageName)
                else -> resources.getIdentifier("btn_$buttonText", "id", packageName)
            }
            if (buttonId != 0 && keyboardView != null) {
                val button = keyboardView.findViewById<Button>(buttonId)
                button?.apply {
                    text = buttonText
                    setOnClickListener {
                        handleKeyPress(buttonText)
                    }
                }
            }
        }
        
        // Also set up the duplicate comma and period buttons in the last row
        if (keyboardView != null) {
            val comma2Id = resources.getIdentifier("btn_comma2", "id", packageName)
            if (comma2Id != 0) {
                val button = keyboardView.findViewById<Button>(comma2Id)
                button?.apply {
                    text = ","
                    setOnClickListener { handleKeyPress(",") }
                }
            }
            
            val period2Id = resources.getIdentifier("btn_period2", "id", packageName)
            if (period2Id != 0) {
                val button = keyboardView.findViewById<Button>(period2Id)
                button?.apply {
                    text = "."
                    setOnClickListener { handleKeyPress(".") }
                }
            }
        }
    }
    
    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection
        
        when (key) {
            "SPACE" -> ic.commitText(" ", 1)
            "DEL" -> {
                val selectedText = ic.getSelectedText(0)?.toString()
                if (!selectedText.isNullOrEmpty()) {
                    ic.deleteSurroundingText(selectedText.length, 0)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            "123" -> {
                // TODO: Switch to number layout
                showToast("Number layout not implemented yet")
            }
            else -> ic.commitText(key, 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Clean up coroutines
    }
}
