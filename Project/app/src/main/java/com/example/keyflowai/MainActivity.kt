package com.example.keyflowai

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnEnableSettings: Button
    private lateinit var btnSwitchInput: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize buttons using findViewById
        btnEnableSettings = findViewById(R.id.btn_enable_settings)
        btnSwitchInput = findViewById(R.id.btn_switch_input)

        btnEnableSettings.setOnClickListener {
            // Open Android keyboard settings
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }

        btnSwitchInput.setOnClickListener {
            // Open keyboard selection menu
            val imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imeManager.showInputMethodPicker()
        }
    }
}
