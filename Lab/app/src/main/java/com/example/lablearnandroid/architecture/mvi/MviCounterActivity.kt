package com.example.lablearnandroid.architecture.mvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lablearnandroid.ui.theme.LabLearnAndroidTheme

/**
 * MVI Activity: The entry point activity for the MVI flow.
 * In MVI, the Activity simply hosts the Screen component.
 */
class MviCounterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabLearnAndroidTheme {
                CounterScreen()
            }
        }
    }
}
