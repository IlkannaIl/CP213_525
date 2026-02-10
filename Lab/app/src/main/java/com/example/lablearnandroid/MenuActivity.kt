package com.example.lablearnandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(onClick = {
                    startActivity(Intent(this@MenuActivity, MainActivity::class.java))
                }) {
                    Text("MainActivity")
                }
                Button(onClick = {
                    startActivity(Intent(this@MenuActivity, MainActivity2::class.java))
                }) {
                    Text("MainActivity2")
                }
                Button(onClick = {
                    startActivity(Intent(this@MenuActivity, MainActivity3::class.java))
                }) {
                    Text("MainActivity3")
                }
            }
        }
    }
}



//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.lablearnandroid.ui.theme.LabLearnAndroidTheme
//
//class MenuActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
//                Button(onClick = {
//                    startActivity(Intent(this@MenuActivity, MainActivity::class.java))
//                }) {
//                    Text("MainActivity")
//                }
//                Button(onClick = {
//                    startActivity(Intent(this@MenuActivity, MainActivity2::class.java))
//                }) {
//                    Text("MainActivity2")
//                }
//                Button(onClick = {
//                    startActivity(Intent(this@MenuActivity, MainActivity3::class.java))
//                }) {
//                    Text("MainActivity3")
//                }
//            }
//        }
//    }
//}
//