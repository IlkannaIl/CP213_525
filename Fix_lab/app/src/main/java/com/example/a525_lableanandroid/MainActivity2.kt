package com.example.a525_lableanandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a525_lableanandroid.ui.theme._525_labLeanAndroidTheme

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting2(
                        name = "Android2",
                        modifier = Modifier.padding(innerPadding)
                    )
                }

        }
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Hello $name! say = "+inputText,
            modifier = modifier
            )
        TextField(
            value = inputText,
            onValueChange = {
                inputText = it
            }
        )
    }
//
    Column{
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Text(
            text = "sayonara $name!",
            modifier = modifier
        )
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    _525_labLeanAndroidTheme {
        Greeting2("Android2")
    }
}