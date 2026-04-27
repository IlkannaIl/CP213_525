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
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.HorizontalScrollView


class KeyFlowIME : InputMethodService() {

    private lateinit var keyboardView: CustomKeyboardView
    private lateinit var refineButton: Button
    private lateinit var musicModeButton: View
    private lateinit var lyricRunnerText: TextView
    private lateinit var internalInputField: EditText
    private lateinit var sendToAppButton: Button
    private lateinit var suggestionContainer: LinearLayout
    private lateinit var clipboardManager: ClipboardManager
    
    // Clipboard functionality
    private val clipboardHistory = mutableListOf<String>()
    private val maxClipboardItems = 5
    
    // Internal input toggle state
    private var originalInternalText: String = ""
    private var isInternalTextTranslated = false
    
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
    
    // Key preview disabled to prevent misalignment issues
    
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
        suggestionContainer = rootView.findViewById(R.id.suggestion_container)

        refineButton?.setOnClickListener {
            handleRefine()
        }
        musicModeButton?.setOnClickListener {
            handleMusicMode()
        }
        
        // Setup clipboard listener
        setupClipboardListener()
        
        // Initialize clipboard display
        updateClipboardDisplay()
        
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
    
    // Key preview disabled
    
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
                                        appendToInternalInput(button.text.toString())
                                        true
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        // Key preview disabled - handle input on down event
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
        
        // Space button - simple click for single space
        view.findViewById<Button>(R.id.btn_SPACE)?.setOnClickListener {
            appendToInternalInput(" ")
            
            // Optional: If using Mirror Input, also commit to external app
            val ic = currentInputConnection
            ic?.commitText(" ", 1)
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
            
            // Commit text to external app at current cursor position
            ic?.commitText(textToSend, 1)
            
            // Add a space after the committed text for better typing flow
            ic?.commitText(" ", 1)
            
            // Clear internal input field
            internalInputField.setText("")
            
            // Ensure focus stays on internal input field for continued typing
            internalInputField.requestFocus()
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
        
        // First, check if user has selected text in internal_input_field
        val currentText = internalInputField.text.toString()
        val selectionStart = internalInputField.selectionStart
        val selectionEnd = internalInputField.selectionEnd
        
        if (selectionStart != selectionEnd) {
            // User has selected text in internal input field - delete the selection
            val newText = currentText.substring(0, selectionStart) + currentText.substring(selectionEnd)
            internalInputField.setText(newText)
            internalInputField.setSelection(selectionStart)
            return
        }
        
        // Check if user has selected text in external app
        val selectedText = ic.getSelectedText(0)?.toString()
        if (selectedText != null && selectedText.isNotEmpty()) {
            ic.commitText("", 1) // Delete selected text in external app
            return
        }
        
        // Handle normal delete in internal input field
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
        val currentText = internalInputField.text.toString()
        if (currentText.isEmpty()) {
            showToast("Type some text to refine")
            return
        }
        
        // Simple toggle between original and refined text (placeholder for future AI integration)
        if (isInternalTextTranslated && originalInternalText.isNotEmpty()) {
            // Toggle back to original text
            internalInputField.setText(originalInternalText)
            internalInputField.setSelection(originalInternalText.length)
            isInternalTextTranslated = false
            showToast("Reverted to original text")
        } else {
            // Placeholder for future AI refinement
            showToast("AI refinement disabled - clipboard mode active")
        }
    }
    
    private fun handleMusicMode() {
        // Keep music mode functionality intact for demo purposes
        // Show a simple demo popup
        showMusicDemoPopup()
    }
    
    private fun showMusicDemoPopup() {
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
            elevation = 12f
        }
        
        // Setup popup content
        val lyricTextView = popupView.findViewById<TextView>(R.id.popup_lyric_text)
        val listenButton = popupView.findViewById<Button>(R.id.popup_listen_button)
        val closeButton = popupView.findViewById<Button>(R.id.popup_close_button)
        
        lyricTextView?.text = "🎵 Music Mode Demo\nClipboard mode is now active!"
        
        listenButton?.setOnClickListener {
            musicPopupWindow?.dismiss()
            showToast("Music feature preserved for demo")
        }
        
        closeButton?.setOnClickListener {
            musicPopupWindow?.dismiss()
        }
        
        // Show popup above the keyboard, centered on screen
        musicPopupWindow?.showAtLocation(
            musicModeButton,
            Gravity.CENTER,
            0, // X offset (centered by Gravity.CENTER)
            -400 // Negative Y offset to force popup to upper half of screen
        )
    }
    
    private fun setupClipboardListener() {
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener {
            // Handle clipboard change
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val newText = clipData.getItemAt(0).text?.toString()
                if (!newText.isNullOrEmpty() && newText != "") {
                    addToClipboardHistory(newText)
                }
            }
        }
    }
    
    private fun addToClipboardHistory(text: String) {
        // Remove if already exists to avoid duplicates
        clipboardHistory.removeAll { it == text }
        
        // Add to beginning
        clipboardHistory.add(0, text)
        
        // Limit to max items
        while (clipboardHistory.size > maxClipboardItems) {
            clipboardHistory.removeAt(clipboardHistory.size - 1)
        }
        
        // Update display
        updateClipboardDisplay()
    }
    
    private fun updateClipboardDisplay() {
        mainHandler.post {
            suggestionContainer.removeAllViews()
            
            if (clipboardHistory.isEmpty()) {
                // Show empty state with gradient background
                lyricRunnerText.text = "Clipboard empty - copy some text!"
                lyricRunnerText.visibility = View.VISIBLE
                suggestionContainer.addView(lyricRunnerText)
            } else {
                // Show clipboard items
                for (item in clipboardHistory) {
                    val textView = TextView(this@KeyFlowIME).apply {
                        text = if (item.length > 30) item.take(27) + "..." else item
                        setTextColor(resources.getColor(R.color.dark_grey_green, null))
                        textSize = 14f
                        setPadding(16, 8, 16, 8)
                        setOnClickListener {
                            pasteClipboardItem(item)
                        }
                        setBackgroundResource(R.drawable.clipboard_item_background)
                    }
                    suggestionContainer.addView(textView)
                }
                lyricRunnerText.visibility = View.GONE
            }
        }
    }
    
    private fun pasteClipboardItem(text: String) {
        appendToInternalInput(text)
        showToast("Pasted: ${text.take(20)}${if (text.length > 20) "..." else ""}")
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
        // Clear clipboard listener
        clipboardManager.removePrimaryClipChangedListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopDeleteRepeat()
        musicPopupWindow?.dismiss()
        // Key preview disabled - no popup to dismiss
        // Clear clipboard listener
        clipboardManager.removePrimaryClipChangedListener(null)
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
