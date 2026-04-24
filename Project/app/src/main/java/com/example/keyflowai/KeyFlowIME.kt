package com.example.keyflowai

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.*
import com.google.ai.client.generativeai.GenerativeModel
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo


class KeyFlowIME : InputMethodService() {

    private lateinit var keyboardView: CustomKeyboardView
    private lateinit var refineButton: Button
    private lateinit var culturalAlert: View
    
    // Keyboard layout states
    enum class LayoutState {
        THAI_NORMAL,
        THAI_SHIFT,
        NUMBERS_BASIC,
        SYMBOLS_EXTRA
    }
    
    private var currentLayoutState = LayoutState.THAI_NORMAL
    private var lastThaiState = LayoutState.THAI_NORMAL
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRefining = false
    
    // UI Handler for main thread operations
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Delete long press handling
    private var deleteHandler: Handler? = null
    private var deleteRunnable: Runnable? = null
    private var isDeletePressed = false

    // Gemini AI
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
        
        culturalAlert?.setOnClickListener {
            handleCulturalAlert()
        }
        
        // Initialize with Thai normal layout
        switchLayout(LayoutState.THAI_NORMAL)

        return rootView
    }

    private fun switchLayout(newState: LayoutState) {

        if (newState == LayoutState.THAI_NORMAL || newState == LayoutState.THAI_SHIFT) {
            lastThaiState = newState
        }

        currentLayoutState = newState
        val layoutRes = when (newState) {
            LayoutState.THAI_NORMAL -> R.layout.custom_keyboard_layout_thai
            LayoutState.THAI_SHIFT -> R.layout.custom_keyboard_layout_thai_shift
            LayoutState.NUMBERS_BASIC -> R.layout.custom_keyboard_layout_numbers
            LayoutState.SYMBOLS_EXTRA -> R.layout.custom_keyboard_layout_symbols
        }
        
        // Inflate new layout
        keyboardView.removeAllViews()
        layoutInflater.inflate(layoutRes, keyboardView, true)
        
        // Setup buttons by reading text from XML - this fixes the dots issue
        setupKeyboardButtons()
    }

//    setup keyboard buttons
    private fun setupKeyboardButtons() {
        val view = keyboardView
        
        // Setup character buttons only (btn_row_col pattern)
        setupCharacterButtons(view)
        
        // Setup functional buttons separately
        setupFunctionalButtons(view)
    }
    
    private fun setupCharacterButtons(view: CustomKeyboardView) {
        for (row in 1..5) {
            for (col in 1..12) {
                val idName = "btn_${row}_${col}"
                val resId = resources.getIdentifier(idName, "id", packageName)
                if (resId != 0) {
                    view.findViewById<Button>(resId)?.let { button ->
                        // Only set click listener for character buttons
                        // Skip functional buttons that might have row_col pattern
                        if (isCharacterButton(idName)) {
                            button.setOnClickListener {
                                handleKeyPress(button.text.toString())
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun isCharacterButton(idName: String): Boolean {
        // List of functional button IDs that should NOT be treated as character buttons
        val functionalButtons = setOf(
            "btn_SHIFT", "btn_123", "btn_SYMBOL", "btn_thai", 
            "btn_SPACE", "btn_DEL", "btn_ENTER"
        )
        return !functionalButtons.contains(idName)
    }
    
    private fun setupFunctionalButtons(view: CustomKeyboardView) {
        // Delete button
        view.findViewById<Button>(R.id.btn_DEL)?.let { setupDeleteButton(it) }
        
        // Space button
        view.findViewById<Button>(R.id.btn_SPACE)?.setOnClickListener {
            handleKeyPress(" ")
        }
        
        // Enter button
        view.findViewById<Button>(R.id.btn_ENTER)?.setOnClickListener {
            val ic = currentInputConnection
            val editorInfo = currentInputEditorInfo

            if (editorInfo != null && editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION != 0) {
                ic?.performEditorAction(editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
        }
        
        // Shift button
        view.findViewById<Button>(R.id.btn_SHIFT)?.setOnClickListener {
            when (currentLayoutState) {
                LayoutState.THAI_NORMAL -> switchLayout(LayoutState.THAI_SHIFT)
                LayoutState.THAI_SHIFT -> switchLayout(LayoutState.THAI_NORMAL)
                else -> { /* Shift not applicable for other layouts */ }
            }
        }
        
        // 123 button (switch to numbers)
        view.findViewById<Button>(R.id.btn_123)?.setOnClickListener {
            if (currentLayoutState == LayoutState.THAI_NORMAL || currentLayoutState == LayoutState.THAI_SHIFT) {
                lastThaiState = currentLayoutState
            }
            switchLayout(LayoutState.NUMBERS_BASIC)
        }
        
        // Thai button (return to Thai layout)
        view.findViewById<Button>(R.id.btn_thai)?.setOnClickListener {
            when (currentLayoutState) {
                LayoutState.NUMBERS_BASIC, LayoutState.SYMBOLS_EXTRA -> {
                    switchLayout(lastThaiState)
                }
                else -> { /* Already in Thai layout */ }
            }
        }
        
        // Symbol button
        view.findViewById<Button>(R.id.btn_SYMBOL)?.setOnClickListener {
            when (currentLayoutState) {
                LayoutState.NUMBERS_BASIC -> switchLayout(LayoutState.SYMBOLS_EXTRA)
                LayoutState.SYMBOLS_EXTRA -> switchLayout(LayoutState.NUMBERS_BASIC)
                else -> { /* Symbol not applicable for Thai layouts */ }
            }
        }
    }

    private fun setupDeleteButton(button: Button) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDeletePressed = true
                    handleDelete()
                    startDeleteRepeat()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDeletePressed = false
                    stopDeleteRepeat()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection
        ic.commitText(key, 1)
    }

    private fun handleDelete() {
        val ic = currentInputConnection
        val selectedText = ic.getSelectedText(0)?.toString()
        if (!selectedText.isNullOrEmpty()) {
            ic.deleteSurroundingText(selectedText.length, 0)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun handleRefine() {
        if (isRefining) return
        
        val ic = currentInputConnection
        val selectedText = ic.getSelectedText(0)?.toString() ?: ""

        if (selectedText.isEmpty()) {
            showToast("Please select text to refine")
            return
        }

        isRefining = true
        setLoadingState(true)

        coroutineScope.launch {
            try {
                val refinedText = refineTextWithAI(selectedText)
                if (refinedText.isNotEmpty()) {
                    ic.commitText(refinedText, 1)
                    showToast("Text refined successfully")
                } else {
                    showToast("Failed to refine text")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isRefining = false
                setLoadingState(false)
            }
        }
    }

    private fun handleCulturalAlert() {
        val ic = currentInputConnection
        val currentText = ic.getSelectedText(0)?.toString() ?: getCurrentSentence(ic)
        
        if (currentText.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val culturalInsight = getCulturalInsight(currentText)
                    showToast(culturalInsight)
                } catch (e: Exception) {
                    showToast("Thai culture values respect and kindness")
                }
            }
        } else {
            showToast("Type or select text for cultural insight")
        }
    }
    
    private fun getCurrentSentence(ic: android.view.inputmethod.InputConnection): String {
        val cursorPos = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val afterCursor = ic.getTextAfterCursor(100, 0)?.toString() ?: ""
        val fullContext = cursorPos + afterCursor
        
        val sentences = fullContext.split("[.!?]".toRegex())
        val currentSentence = sentences.find { it.contains(cursorPos) } ?: fullContext.take(50)
        return currentSentence.trim()
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
    
    private suspend fun getCulturalInsight(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Based on the following text, provide a brief cultural insight related to Thai culture, language, or traditions.
                    Keep it concise (under 100 characters) and educational.
                    If the text is not Thai-related, provide a general Thai cultural fact.
                    
                    Text: $text
                """.trimIndent()
                
                val response = generativeModel.generateContent(prompt)
                response.text?.trim()?.take(100) ?: "Thai culture values respect and kindness."
            } catch (e: Exception) {
                "Thai culture values respect and kindness."
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
            Toast.makeText(this@KeyFlowIME, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        stopDeleteRepeat()
    }

    private fun startDeleteRepeat() {
        deleteHandler = Handler(Looper.getMainLooper())
        deleteRunnable = object : Runnable {
            override fun run() {
                if (isDeletePressed) {
                    handleDelete()
                    deleteHandler?.postDelayed(this, 50) // Repeat every 50ms
                }
            }
        }
        deleteHandler?.postDelayed(deleteRunnable!!, 500) // Start repeat after 500ms
    }

    private fun stopDeleteRepeat() {
        deleteHandler?.removeCallbacks(deleteRunnable!!)
        deleteHandler = null
        deleteRunnable = null
    }
}
