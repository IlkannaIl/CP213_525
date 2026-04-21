package com.example.keyflowai

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.*
import com.google.ai.client.generativeai.GenerativeModel
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent

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

    private fun setupKeyboardButtons() {
        // Get all button IDs based on current layout
        val buttonIds = when (currentLayoutState) {
            LayoutState.THAI_NORMAL, LayoutState.THAI_SHIFT -> {
                listOf(
                    "btn_Q", "btn_W", "btn_E", "btn_R", "btn_T", "btn_Y", "btn_U", "btn_I", "btn_O", "btn_P",
                    "btn_A", "btn_S", "btn_D", "btn_F", "btn_G", "btn_H", "btn_J", "btn_K", "btn_L",
                    "btn_Z", "btn_X", "btn_C", "btn_V", "btn_B", "btn_N", "btn_M", "btn_comma", "btn_period",
                    "btn_comma2", "btn_period2", "btn_SHIFT", "btn_123", "btn_SPACE", "btn_DEL"
                )
            }
            LayoutState.NUMBERS_BASIC -> {
                listOf(
                    "btn_1", "btn_2", "btn_3", "btn_4", "btn_5", "btn_6", "btn_7", "btn_8", "btn_9", "btn_0",
                    "btn_at", "btn_hash", "btn_dollar", "btn_percent", "btn_amp", "btn_star", "btn_minus", "btn_plus", "btn_equal",
                    "btn_exclaim", "btn_question", "btn_slash", "btn_backslash", "btn_pipe", "btn_colon", "btn_semicolon", "btn_parenL", "btn_parenR",
                    "btn_1234", "btn_ABC", "btn_SPACE", "btn_comma", "btn_period", "btn_DEL"
                )
            }
            LayoutState.SYMBOLS_EXTRA -> {
                listOf(
                    "btn_tilde", "btn_grave", "btn_pipe", "btn_sqrt", "btn_pi", "btn_divide", "btn_multiply", "btn_degree", "btn_caret", "btn_euro",
                    "btn_bracketL", "btn_bracketR", "btn_braceL", "btn_braceR", "btn_less", "btn_greater", "btn_bullet", "btn_dagger", "btn_copyright",
                    "btn_registered", "btn_trademark", "btn_section", "btn_paragraph", "btn_ellipsis", "bnd_emdash", "btn_endash", "btn_quoteL", "btn_quoteR",
                    "btn_1234", "btn_ABC", "btn_SPACE", "btn_period", "btn_comma", "btn_DEL"
                )
            }
        }
        
        buttonIds.forEach { buttonName ->
            val resourceId = resources.getIdentifier(buttonName, "id", packageName)
            if (resourceId != 0) {
                val button = keyboardView.findViewById<Button>(resourceId)
                button?.apply {
                    // Get text from XML - this fixes the dots issue
                    val buttonText = text.toString()
                    
                    when (buttonName) {
                        // Layout navigation buttons
                        "btn_SHIFT" -> {
                            when (currentLayoutState) {
                                LayoutState.THAI_NORMAL -> setOnClickListener { switchLayout(LayoutState.THAI_SHIFT) }
                                LayoutState.THAI_SHIFT -> setOnClickListener { switchLayout(LayoutState.THAI_NORMAL) }
                                else -> {}
                            }
                        }
                        "btn_123", "btn_thai" -> {
                            when (currentLayoutState) {
                                LayoutState.THAI_NORMAL, LayoutState.THAI_SHIFT -> setOnClickListener { switchLayout(LayoutState.NUMBERS_BASIC) }
                                LayoutState.NUMBERS_BASIC -> setOnClickListener { switchLayout(LayoutState.THAI_NORMAL) }
                                LayoutState.SYMBOLS_EXTRA -> setOnClickListener { switchLayout(LayoutState.NUMBERS_BASIC) }
                            }
                        }
                        "btn_symbols_number" -> {
                            when (currentLayoutState) {
                                LayoutState.NUMBERS_BASIC -> setOnClickListener { switchLayout(LayoutState.SYMBOLS_EXTRA) }
                                LayoutState.SYMBOLS_EXTRA -> setOnClickListener { switchLayout(LayoutState.NUMBERS_BASIC) }
                                else -> {}
                            }
                        }
                        "btn_SPACE" -> setOnClickListener { handleKeyPress(" ") }
                        "btn_DEL" -> setupDeleteButton(this)
                        else -> setOnClickListener { handleKeyPress(buttonText) }
                    }
                }
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
