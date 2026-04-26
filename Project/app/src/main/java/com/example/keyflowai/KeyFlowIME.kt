package com.example.keyflowai

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.widget.PopupWindow
import android.widget.LinearLayout
import android.view.Gravity
import android.view.LayoutInflater
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONException
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log


class KeyFlowIME : InputMethodService() {

    private lateinit var keyboardView: CustomKeyboardView
    private lateinit var refineButton: Button
    private lateinit var musicModeButton: View
    private lateinit var lyricRunnerText: TextView
    private lateinit var internalInputField: EditText
    private lateinit var sendToAppButton: Button
    
    // Lyricist Keyboard state
    private var currentLyricQuote: String = ""
    private var originalLyricQuote: String = ""
    private var isThaiTranslation = false
    private var currentMusicUrl: String = ""
    
    // Internal input toggle state
    private var originalInternalText: String = ""
    private var isInternalTextTranslated = false
    
    // Lyric runner toggle state
    private var originalLyricRunnerText: String = ""
    private var isLyricRunnerTranslated = false
    
    // Music popup window
    private var musicPopupWindow: PopupWindow? = null
    
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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isRefining = false
    
    // UI Handler for main thread operations
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Delete long press handling
    private var deleteHandler: Handler? = null
    private var deleteRunnable: Runnable? = null
    private var isDeletePressed = false
    
    // Key preview handling
    private var keyPreviewPopup: PopupWindow? = null
    private var keyPreviewHandler: Handler? = null
    private var keyPreviewRunnable: Runnable? = null
    
    // Trackpad mode handling
    private var isTrackpadMode = false
    private var trackpadStartX = 0f
    private var trackpadInitialCursorPos = 0

    override fun onCreateInputView(): View {
        
        val rootView = layoutInflater.inflate(R.layout.input_method, null)
        
        // Find the actual keyboard view within the inflated layout
        keyboardView = rootView.findViewById(R.id.keyboard_view)

        refineButton = rootView.findViewById(R.id.btn_refine)
        musicModeButton = rootView.findViewById(R.id.btn_music_mode)
        lyricRunnerText = rootView.findViewById(R.id.lyric_runner_text)
        internalInputField = rootView.findViewById(R.id.internal_input_field)
        sendToAppButton = rootView.findViewById(R.id.btn_send_to_app)

        refineButton?.setOnClickListener {
            handleRefine()
        }
        
        musicModeButton?.setOnClickListener {
            handleMusicMode()
        }
        
        lyricRunnerText?.setOnClickListener {
            handleLyricClick()
        }
        
        sendToAppButton?.setOnClickListener {
            handleSendToApp()
        }
        
        // Setup TextWatcher for send button visibility
        setupTextWatcher()
        
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
        
        // Setup buttons
        setupKeyboardButtons()
    }

//    setup keyboard buttons
    private fun setupKeyboardButtons() {
        val view = keyboardView
        
        // Setup buttons (btn_row_col)
        setupCharacterButtons(view)
        
        // Setup functional buttons
        setupFunctionalButtons(view)
        
        // Setup key preview
        setupKeyPreview()
    }
    
    private fun setupKeyPreview() {
        // Initialize key preview popup
        val previewView = layoutInflater.inflate(R.layout.key_preview_layout, null) as TextView
        keyPreviewPopup = PopupWindow(
            previewView,
            48,
            48,
            false
        ).apply {
            setBackgroundDrawable(resources.getDrawable(android.R.drawable.dialog_frame, null))
            elevation = 8f
        }
    }
    
    private fun showKeyPreview(button: Button) {
        val previewView = keyPreviewPopup?.contentView as? TextView
        previewView?.text = button.text
        
        val location = IntArray(2)
        button.getLocationOnScreen(location)
        val x = location[0] + button.width / 2 - 24
        val y = location[1] - 60
        
        keyPreviewPopup?.showAtLocation(button, Gravity.NO_GRAVITY, x, y)
        
        keyPreviewHandler = Handler(Looper.getMainLooper())
        keyPreviewRunnable = object : Runnable {
            override fun run() {
                keyPreviewPopup?.dismiss()
            }
        }
        keyPreviewHandler?.postDelayed(keyPreviewRunnable!!, 100)
    }
    
    private fun setupCharacterButtons(view: CustomKeyboardView) {
        for (row in 1..5) {
            for (col in 1..12) {
                val idName = "btn_${row}_${col}"
                val resId = resources.getIdentifier(idName, "id", packageName)
                if (resId != 0) {
                    view.findViewById<Button>(resId)?.let { button ->

                        if (isCharacterButton(idName)) {
                            button.setOnTouchListener { _, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        showKeyPreview(button)
                                        true
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        appendToInternalInput(button.text.toString())
                                        keyPreviewPopup?.dismiss()
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun isCharacterButton(idName: String): Boolean {

        val functionalButtons = setOf(
            "btn_SHIFT", "btn_123", "btn_SYMBOL", "btn_thai", 
            "btn_SPACE", "btn_DEL", "btn_ENTER"
        )
        return !functionalButtons.contains(idName)
    }
    
    private fun setupFunctionalButtons(view: CustomKeyboardView) {
        // Delete button
        view.findViewById<Button>(R.id.btn_DEL)?.let { setupDeleteButton(it) }
        
        // Space button with trackpad mode
        view.findViewById<Button>(R.id.btn_SPACE)?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    trackpadStartX = event.rawX
                    trackpadInitialCursorPos = internalInputField.selectionStart
                    isTrackpadMode = true
                    keyPreviewHandler = Handler(Looper.getMainLooper())
                    keyPreviewRunnable = object : Runnable {
                        override fun run() {
                            if (isTrackpadMode) {
                                keyPreviewHandler?.postDelayed(this, 100)
                            }
                        }
                    }
                    keyPreviewHandler?.postDelayed(keyPreviewRunnable!!, 500) // Start trackpad after 500ms
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isTrackpadMode) {
                        val deltaX = event.rawX - trackpadStartX
                        val moveThreshold = 30f
                        
                        if (kotlin.math.abs(deltaX) > moveThreshold) {
                            val currentText = internalInputField.text.toString()
                            val maxPos = currentText.length
                            
                            // Move cursor based on swipe direction
                            val newPos = when {
                                deltaX > 0 -> (trackpadInitialCursorPos + 1).coerceAtMost(maxPos)
                                deltaX < 0 -> (trackpadInitialCursorPos - 1).coerceAtLeast(0)
                                else -> trackpadInitialCursorPos
                            }
                            
                            internalInputField.setSelection(newPos)
                            trackpadInitialCursorPos = newPos
                            trackpadStartX = event.rawX
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isTrackpadMode) {
                        isTrackpadMode = false
                        keyPreviewHandler?.removeCallbacks(keyPreviewRunnable!!)
                        // If it was a short press (less than 500ms), insert space
                        if (System.currentTimeMillis() - event.downTime < 500) {
                            appendToInternalInput(" ")
                        }
                    }
                    true
                }
                else -> false
            }
        }
        
        // Enter button
        view.findViewById<Button>(R.id.btn_ENTER)?.setOnClickListener {
            appendToInternalInput("\n")
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

//    setup delete button
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
    
    private fun appendToInternalInput(text: String) {
        val currentText = internalInputField.text.toString()
        val cursorPosition = internalInputField.selectionStart
        val newText = currentText.substring(0, cursorPosition) + text + currentText.substring(cursorPosition)
        internalInputField.setText(newText)
        internalInputField.setSelection(cursorPosition + text.length)
    }
    
    private fun handleSendToApp() {
        val textToSend = internalInputField.text.toString()
        if (textToSend.isNotEmpty()) {
            val ic = currentInputConnection
            ic?.commitText(textToSend, 1)
            internalInputField.setText("")
        }
    }
    
    private fun setupTextWatcher() {
        internalInputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Show send button when field is not empty
                sendToAppButton.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
        })
    }

    private fun handleDelete() {
        val ic = currentInputConnection
        val selectedText = ic.getSelectedText(0)?.toString()
        
        // If user has selected text, delete the entire selection
        if (selectedText != null && selectedText.isNotEmpty()) {
            ic.commitText("", 1) // Delete selected text
            return
        }
        
        val currentText = internalInputField.text.toString()
        if (currentText.isNotEmpty()) {
            val cursorPosition = internalInputField.selectionStart
            if (cursorPosition > 0) {
                val newText = currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition)
                internalInputField.setText(newText)
                internalInputField.setSelection(cursorPosition - 1)
            }
        } else {
            // If internal input field is empty, send backspace to external app
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun handleRefine() {
        if (isRefining) return
        
        val currentText = internalInputField.text.toString()
        if (currentText.isEmpty()) {
            showToast("Type some text to refine")
            return
        }
        
        // Independent toggle logic: Check if we should toggle back to original text
        if (isInternalTextTranslated && originalInternalText.isNotEmpty()) {
            // Toggle back to original Thai text
            internalInputField.setText(originalInternalText)
            internalInputField.setSelection(originalInternalText.length)
            isInternalTextTranslated = false
            showToast("Reverted to original text")
            return
        }
        
        // Save original text and translate to English if Thai, or refine if English
        originalInternalText = currentText
        isRefining = true
        setLoadingState(true)
        
        serviceScope.launch {
            try {
                val prompt = if (isThaiText(currentText)) {
                    "Translate this Thai text to professional English. Output ONLY the polished English translation, no explanations."
                } else {
                    "Refine and improve this English text to be more professional and clear. Output ONLY the refined English text, no explanations."
                }
                
                val refinedText = withContext(Dispatchers.IO) {
                    fetchGeminiResponse(prompt)
                }
                
                withContext(Dispatchers.Main) {
                    if (refinedText.isNotEmpty()) {
                        internalInputField.setText(refinedText)
                        internalInputField.setSelection(refinedText.length)
                        isInternalTextTranslated = true
                        val action = if (isThaiText(currentText)) "translated" else "refined"
                        showToast("Text $action successfully")
                    } else {
                        showToast("Failed to process text")
                    }
                    isRefining = false
                    setLoadingState(false)
                }
            } catch (e: Exception) {
                Log.e("GEMINI_ERROR", "Error: ", e)
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                    isRefining = false
                    setLoadingState(false)
                }
            }
        }
    }
    
    private fun isThaiText(text: String): Boolean {
        return text.any { char -> char.code in 0x0E00..0x0E7F }
    }
    
    private fun refineInternalInput() {
        val currentText = internalInputField.text.toString()
        if (currentText.isEmpty()) {
            showToast("Type some text to refine")
            return
        }
        
        // Check if we should toggle back to original Thai text
        if (isInternalTextTranslated && originalInternalText.isNotEmpty()) {
            // Toggle back to original Thai
            internalInputField.setText(originalInternalText)
            internalInputField.setSelection(originalInternalText.length)
            isInternalTextTranslated = false
            showToast("Reverted to original Thai text")
            return
        }
        
        // Save original Thai text and translate to English
        originalInternalText = currentText
        isRefining = true
        setLoadingState(true)
        
        serviceScope.launch {
            try {
                val translatedText = withContext(Dispatchers.IO) {
                    val prompt = "Translate this Thai text to professional English. Output ONLY the polished English translation, no explanations."
                    fetchGeminiResponse(prompt)
                }
                
                withContext(Dispatchers.Main) {
                    if (translatedText.isNotEmpty()) {
                        internalInputField.setText(translatedText)
                        internalInputField.setSelection(translatedText.length)
                        isInternalTextTranslated = true
                        showToast("Text translated successfully")
                    } else {
                        showToast("Failed to translate text")
                    }
                    isRefining = false
                    setLoadingState(false)
                }
            } catch (e: Exception) {
                Log.e("GEMINI_ERROR", "Error: ", e)
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                    isRefining = false
                    setLoadingState(false)
                }
            }
        }
    }
    
    private fun toggleLyricTranslation() {
        if (isRefining) return
        
        isRefining = true
        setLoadingState(true)
        
        serviceScope.launch {
            try {
                if (isThaiTranslation) {
                    // Show original English lyric
                    lyricRunnerText.text = originalLyricQuote
                    isThaiTranslation = false
                } else {
                    // Show Thai translation
                    val thaiTranslation = translateLyricToThai(originalLyricQuote)
                    lyricRunnerText.text = thaiTranslation
                    isThaiTranslation = true
                }
            } catch (e: Exception) {
                showToast("Translation error: ${e.message}")
            } finally {
                isRefining = false
                setLoadingState(false)
            }
        }
    }
    
    private fun refineInputText() {
        val ic = currentInputConnection
        val selectedText = ic.getSelectedText(0)?.toString() ?: ""

        if (selectedText.isEmpty()) {
            showToast("Please select text to refine")
            return
        }

        isRefining = true
        setLoadingState(true)

        serviceScope.launch {
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

    private fun handleMusicMode() {
        if (isRefining) return
        
        val currentText = internalInputField.text.toString()
        
        if (currentText.isNotEmpty()) {
            isRefining = true
            setLoadingState(true)
            
            serviceScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        val prompt = "Give me one short English song lyric quote and the YouTube search query for it based on this emotion: $currentText. Format: Quote - Artist | SearchQuery"
                        fetchGeminiResponse(prompt)
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (response.isNotEmpty()) {
                            // Parse the response (format: "Quote - Artist | SearchQuery")
                            val parts = response.split(" | ")
                            val lyricPart = if (parts.isNotEmpty()) parts[0] else response
                            val searchQuery = if (parts.size > 1) parts[1] else lyricPart
                            
                            currentLyricQuote = lyricPart
                            originalLyricQuote = lyricPart
                            isThaiTranslation = false
                            currentMusicUrl = "https://www.youtube.com/results?search_query=${searchQuery.replace(" ", "+")}"
                            
                            // Show popup window with song info
                            showMusicPopup(lyricPart, currentMusicUrl)
                            
                            // Also update lyric runner text
                            lyricRunnerText.text = lyricPart
                            lyricRunnerText.isSelected = true // Start marquee
                            
                            showToast("Song found! Click lyric to translate")
                        } else {
                            showToast("No response from AI")
                        }
                        isRefining = false
                        setLoadingState(false)
                    }
                } catch (e: Exception) {
                    Log.e("GEMINI_ERROR", "Error: ", e)
                    withContext(Dispatchers.Main) {
                        showToast("Error: ${e.message}")
                        isRefining = false
                        setLoadingState(false)
                    }
                }
            }
        } else {
            showToast("Type some text to get a matching lyric")
        }
    }
    
    private fun showMusicPopup(lyric: String, musicUrl: String) {
        // Dismiss any existing popup
        musicPopupWindow?.dismiss()
        
        // Create popup view
        val popupView = layoutInflater.inflate(R.layout.music_popup_layout, null)
        musicPopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // focusable
        ).apply {
            // Set background with rounded corners and shadow
            setBackgroundDrawable(resources.getDrawable(android.R.drawable.dialog_frame, null))
            elevation = 12f // Increased elevation for better shadow
        }
        
        // Setup popup content
        val lyricTextView = popupView.findViewById<TextView>(R.id.popup_lyric_text)
        val listenButton = popupView.findViewById<Button>(R.id.popup_listen_button)
        val closeButton = popupView.findViewById<Button>(R.id.popup_close_button)
        
        lyricTextView?.text = lyric
        
        listenButton?.setOnClickListener {
            // Dismiss popup first, then open music link
            musicPopupWindow?.dismiss()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(musicUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Could not open music app")
            }
        }
        
        closeButton?.setOnClickListener {
            // Simply dismiss the popup
            musicPopupWindow?.dismiss()
        }
        
        // Show popup above the keyboard, centered on screen
        musicPopupWindow?.showAtLocation(
            musicModeButton,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            0, // X offset (centered by Gravity.CENTER_HORIZONTAL)
            100 // Y offset from top to avoid overlapping with status bar
        )
        
        // No auto-dismiss - popup stays visible until user interaction
    }
    
    private fun handleLyricClick() {
        val currentLyricText = lyricRunnerText.text.toString()
        
        // Check if we should toggle back to original lyric
        if (isLyricRunnerTranslated && originalLyricRunnerText.isNotEmpty()) {
            lyricRunnerText.text = originalLyricRunnerText
            isLyricRunnerTranslated = false
            showToast("Reverted to original lyric")
            return
        }
        
        // If we have a current lyric quote, translate it
        if (currentLyricQuote.isNotEmpty() && currentLyricText != "♪ Welcome to The Lyricist Keyboard ♪") {
            originalLyricRunnerText = currentLyricText
            isRefining = true
            
            serviceScope.launch {
                try {
                    val thaiTranslation = withContext(Dispatchers.IO) {
                        val prompt = "Translate this English lyric to Thai with poetic expression. Output ONLY the Thai translation, no explanations."
                        fetchGeminiResponse(prompt)
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (thaiTranslation.isNotEmpty()) {
                            lyricRunnerText.text = thaiTranslation
                            isLyricRunnerTranslated = true
                            showToast("Lyric translated to Thai")
                        } else {
                            showToast("Failed to translate lyric")
                        }
                        isRefining = false
                    }
                } catch (e: Exception) {
                    Log.e("GEMINI_ERROR", "Error translating lyric: ", e)
                    withContext(Dispatchers.Main) {
                        showToast("Translation error: ${e.message}")
                        isRefining = false
                    }
                }
            }
        } else if (currentMusicUrl.isNotEmpty()) {
            // Original behavior: open YouTube search
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentMusicUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Could not open YouTube search")
            }
        }
    }
    
    private fun showMusicLinkDialog() {
        if (currentMusicUrl.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Listen to Music")
                .setMessage("Would you like to listen to this song?")
                .setPositiveButton("Listen") { _, _ ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentMusicUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } catch (e: Exception) {
                        showToast("Could not open music app")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
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
                
                return@withContext fetchGeminiResponse(prompt)
            } catch (e: Exception) {
                throw Exception("AI processing failed: ${e.message}")
            }
        }
    }
    
    private suspend fun fetchLyricQuoteFromGemini(userEmotion: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Based on the following text/emotion, return an English song lyric quote that matches the feeling.
                    Format: "Quote - Artist"
                    Keep it concise and meaningful.
                    
                    Text/Emotion: $userEmotion
                """.trimIndent()
                
                return@withContext fetchGeminiResponse(prompt)
            } catch (e: Exception) {
                throw Exception("Failed to fetch lyric: ${e.message}")
            }
        }
    }
    
    private suspend fun translateLyricToThai(englishLyric: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Translate the following English lyric to Thai with poetic and artistic expression.
                    Keep the meaning and emotion intact.
                    Return only the Thai translation without any explanation.
                    
                    English lyric: $englishLyric
                """.trimIndent()
                
                val result = fetchGeminiResponse(prompt)
                return@withContext if (result.isNotEmpty()) result else englishLyric
            } catch (e: Exception) {
                throw Exception("Translation failed: ${e.message}")
            }
        }
    }
    
    // Manual OkHttp implementation for Gemini API
    private suspend fun fetchGeminiResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = "AIzaSyBJ7Hon6CNFixnNgUqJRZFUoCBVas_WRmc"
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                
                // Create JSON body
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }
                
                // Create OkHttp client
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                // Create request
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                Log.d("GEMINI_API", "Making manual OkHttp call to: $url")
                
                // Execute request
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    
                    if (!response.isSuccessful) {
                        Log.e("GEMINI_ERROR", "HTTP Error: ${response.code} - $responseBody")
                        
                        // Try to parse error message from JSON
                        val errorMessage = try {
                            val errorJson = JSONObject(responseBody)
                            errorJson.getJSONObject("error")
                                .getString("message")
                        } catch (e: JSONException) {
                            "HTTP ${response.code}: ${response.message}"
                        }
                        
                        throw Exception("API Error ($response.code): $errorMessage")
                    }
                    
                    // Parse successful response
                    try {
                        val responseJson = JSONObject(responseBody)
                        val candidates = responseJson.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                val firstPart = parts.getJSONObject(0)
                                val result = firstPart.getString("text").trim()
                                
                                // Update UI on main thread
                                withContext(Dispatchers.Main) {
                                    Log.d("GEMINI_API", "Manual OkHttp response: $result")
                                }
                                
                                return@withContext result
                            }
                        }
                        throw Exception("No content in response")
                    } catch (e: JSONException) {
                        Log.e("GEMINI_ERROR", "JSON parsing error: ", e)
                        throw Exception("Failed to parse response: ${e.message}")
                    }
                }
            } catch (e: IOException) {
                Log.e("GEMINI_ERROR", "Network error: ", e)
                throw Exception("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("GEMINI_ERROR", "Error in manual API call: ", e)
                withContext(Dispatchers.Main) {
                    Log.e("GEMINI_ERROR", "Main thread error: ${e.message}")
                }
                throw Exception("API call failed: ${e.message}")
            }
        }
    }
    
    private suspend fun callGeminiRefine(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Rewrite this Thai text to be very professional/polite or translate to English if appropriate: $text"
                return@withContext fetchGeminiResponse(prompt)
            } catch (e: Exception) {
                throw Exception("AI processing failed: ${e.message}")
            }
        }
    }
    
    private suspend fun callGeminiLyric(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Based on this emotion $text, suggest one English song lyric quote with artist name. Return only the quote and artist."
                return@withContext fetchGeminiResponse(prompt)
            } catch (e: Exception) {
                throw Exception("Failed to fetch lyric: ${e.message}")
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

    override fun onFinishInput() {
        super.onFinishInput()
        // Auto-clear internal input field when keyboard closes
        internalInputField.setText("")
        // Reset translation states
        isInternalTextTranslated = false
        originalInternalText = ""
        isLyricRunnerTranslated = false
        originalLyricRunnerText = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopDeleteRepeat()
        musicPopupWindow?.dismiss()
        keyPreviewPopup?.dismiss()
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
