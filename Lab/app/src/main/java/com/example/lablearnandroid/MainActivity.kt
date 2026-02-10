package com.example.lablearnandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current

            var str by remember { mutableIntStateOf(5) }
            var agi by remember { mutableIntStateOf(10) }
            var int by remember { mutableIntStateOf(15) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Gray)
                    .padding(32.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(color = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .background(color = Color.Red)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))


                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            val intent = Intent(context, MainActivity2::class.java)
                            context.startActivity(intent)
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, MainActivity2::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "ไปหน้าถัดไป")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusControl(label = "Str", value = str, onValueChange = { str = it })
                    StatusControl(label = "Agi", value = agi, onValueChange = { agi = it })
                    StatusControl(label = "Int", value = int, onValueChange = { int = it })
                }
            }
        }
    }
}

@Composable
fun StatusControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { onValueChange(value + 1) }) {
            Text(text = "+", fontSize = 24.sp)
        }
        Text(text = label, fontSize = 24.sp)
        Text(text = value.toString(), fontSize = 24.sp)
        Button(onClick = { if (value > 0) onValueChange(value - 1) }) {
            Text(text = "-", fontSize = 24.sp)
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun previewScreen(){
//
//}