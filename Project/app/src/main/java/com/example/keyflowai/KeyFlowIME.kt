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
import com.google.ai.client.generativeai.GenerativeModel
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
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject
import org.json.JSONArray


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
        apiKey = "AIzaSyCKFiJqsfJqLxMZfJUG16e_yf_EFfq06K4"
    )

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
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = "Improve this Thai text or translate to formal English: $textToRefine"
                val refinedText = fetchGeminiResponse(prompt)
                
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
        
        coroutineScope.launch {
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

    private fun handleMusicMode() {
        if (isRefining) return
        
        val currentText = internalInputField.text.toString()
        
        if (currentText.isNotEmpty()) {
            isRefining = true
            setLoadingState(true)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prompt = "Give me one short English song lyric quote and the YouTube search query for it based on this emotion: $currentText. Format: Quote - Artist | SearchQuery"
                    val response = fetchGeminiResponse(prompt)
                    
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
                            lyricRunnerText.visibility = View.VISIBLE
                            lyricRunnerText.isSelected = true // Start marquee
                            
                            showToast("Lyric found! Click to insert")
                        } else {
                            lyricRunnerText.text = "Connection Error"
                            lyricRunnerText.visibility = View.VISIBLE
                            lyricRunnerText.isSelected = true
                        }
                        isRefining = false
                        setLoadingState(false)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        lyricRunnerText.text = "Connection Error"
                        lyricRunnerText.visibility = View.VISIBLE
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
                
                val response = generativeModel.generateContent(prompt)
                response.text?.trim() ?: ""
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
                
                val response = generativeModel.generateContent(prompt)
                response.text?.trim() ?: ""
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
                
                val response = generativeModel.generateContent(prompt)
                response.text?.trim() ?: englishLyric
            } catch (e: Exception) {
                throw Exception("Translation failed: ${e.message}")
            }
        }
    }
    
    // Placeholder AI functions - Replace with actual API calls
    private suspend fun fetchGeminiResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDlq8pc9fC79_I08kwLy6hGJxkcjwJM_eM")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonPayload = JSONObject().apply {
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
                
                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream, "UTF-8")
                writer.write(jsonPayload.toString())
                writer.flush()
                writer.close()
                outputStream.close()
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    inputStream.close()
                    
                    val jsonResponse = JSONObject(response.toString())
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val part = parts.getJSONObject(0)
                            return@withContext part.getString("text").trim()
                        }
                    }
                    return@withContext ""
                } else {
                    throw Exception("HTTP Error: $responseCode")
                }
            } catch (e: Exception) {
                throw Exception("API call failed: ${e.message}")
            } finally {
                // Connection will be automatically closed
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
