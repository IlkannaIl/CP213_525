package com.example.keyflowai

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
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
    }
    
    private fun setupCharacterButtons(view: CustomKeyboardView) {
        for (row in 1..5) {
            for (col in 1..12) {
                val idName = "btn_${row}_${col}"
                val resId = resources.getIdentifier(idName, "id", packageName)
                if (resId != 0) {
                    view.findViewById<Button>(resId)?.let { button ->

                        if (isCharacterButton(idName)) {
                            button.setOnClickListener {
                                appendToInternalInput(button.text.toString())
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
        
        // Space button
        view.findViewById<Button>(R.id.btn_SPACE)?.setOnClickListener {
            appendToInternalInput(" ")
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
            val ic = currentInputConnection
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun handleRefine() {
        if (isRefining) return
        
        // Dual-mode logic: Check if we have a lyric quote to translate
        if (currentLyricQuote.isNotEmpty()) {
            // Toggle between English and Thai translation
            toggleLyricTranslation()
        } else {
            // Refine text from internal input field
            refineInternalInput()
        }
    }
    
    private fun refineInternalInput() {
        val textToRefine = internalInputField.text.toString()
        if (textToRefine.isEmpty()) {
            showToast("Type some text to refine")
            return
        }
        
        isRefining = true
        setLoadingState(true)
        
        serviceScope.launch {
            try {
                val refinedText = withContext(Dispatchers.IO) {
                    val prompt = "Improve this Thai text or translate to formal English: $textToRefine"
                    fetchGeminiResponse(prompt)
                }
                
                withContext(Dispatchers.Main) {
                    if (refinedText.isNotEmpty()) {
                        internalInputField.setText(refinedText)
                        internalInputField.setSelection(refinedText.length) // Move cursor to end
                        showToast("Text refined successfully")
                    } else {
                        showToast("Failed to refine text")
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
            
            // Show "Connecting..." immediately
            lyricRunnerText.text = "Connecting..."
            lyricRunnerText.visibility = View.VISIBLE
            lyricRunnerText.isSelected = true
            
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
                            
                            lyricRunnerText.text = lyricPart
                            lyricRunnerText.isSelected = true // Start marquee
                            
                            showToast("Lyric found! Click to insert")
                        } else {
                            lyricRunnerText.text = "No response from AI"
                            lyricRunnerText.isSelected = true
                        }
                        isRefining = false
                        setLoadingState(false)
                    }
                } catch (e: Exception) {
                    Log.e("GEMINI_ERROR", "Error: ", e)
                    withContext(Dispatchers.Main) {
                        lyricRunnerText.text = "Error: ${e.message}"
                        lyricRunnerText.isSelected = true
                        isRefining = false
                        setLoadingState(false)
                    }
                }
            }
        } else {
            showToast("Type some text to get a matching lyric")
        }
    }
    
    private fun handleLyricClick() {
        if (currentLyricQuote.isNotEmpty()) {
            val ic = currentInputConnection
            ic.commitText(currentLyricQuote, 1)
            
            // Open YouTube search using the currentMusicUrl
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
                val apiKey = "AIzaSyBQm7RxFUtj2FMAQ_XGLtzaZIrAjf0R6xU"
                val url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=$apiKey"
                
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
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
