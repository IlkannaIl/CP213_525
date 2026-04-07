package com.example.a525_lableanandroid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a525_lableanandroid.ui.theme._525_labLeanAndroidTheme

class PokedexActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Lifecycle", "PokedexActivity : onCreate")
        enableEdgeToEdge()
        setContent {
            ListScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("Lifecycle", "PokedexActivity : onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("Lifecycle", "PokedexActivity : onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i("Lifecycle", "PokedexActivity : onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("Lifecycle", "PokedexActivity : onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Lifecycle", "PokedexActivity : onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("Lifecycle", "PokedexActivity : onRestart")
    }

    @Composable
    fun ListScreen() {
        // ท่าง่าย
        Column(modifier = Modifier.fillMaxSize().background(Color.Red).padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().background(Color.Gray).padding(16.dp)) {
                LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
                    items(items = allKantoPokemon) { item ->
                        Row{
                            Text(text= item.number.toString())
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text= item.name)
                        }
                    }
                }
            }
        }
    }

    data class Pokemon(
        val name: String,
        val number: Int
    )

    val allKantoPokemon = listOf(
        Pokemon("Bulbasaur", 1),
        Pokemon("Ivysaur", 2),
        Pokemon("Venusaur", 3),
        Pokemon("Charmander", 4),
        Pokemon("Charmeleon", 5),
        Pokemon("Charizard", 6),
        Pokemon("Squirtle", 7),
        Pokemon("Wartortle", 8),
        Pokemon("Blastoise", 9),
        Pokemon("Caterpie", 10),
        Pokemon("Metapod", 11),
        Pokemon("Butterfree", 12),
        Pokemon("Weedle", 13),
        Pokemon("Kakuna", 14),
        Pokemon("Beedrill", 15),
        Pokemon("Pidgey", 16),
        Pokemon("Pidgeotto", 17),
        Pokemon("Pidgeot", 18),
        Pokemon("Rattata", 19),
        Pokemon("Raticate", 20),
        Pokemon("Spearow", 21),
        Pokemon("Fearow", 22),
        Pokemon("Ekans", 23),
        Pokemon("Arbok", 24),
        Pokemon("Pikachu", 25),
        Pokemon("Raichu", 26),
        Pokemon("Sandshrew", 27),
        Pokemon("Sandslash", 28),
        Pokemon("Nidoran♀", 29),
        Pokemon("Nidorina", 30),
        Pokemon("Nidoqueen", 31),
        Pokemon("Nidoran♂", 32),
        Pokemon("Nidorino", 33),
        Pokemon("Nidoking", 34),
        Pokemon("Clefairy", 35),
    )

//    val imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${item.number}.png"
//
//    AsyncImage(
//        model = imageUrl,
//        contentDescription = "Sprite of ${item.name}",
//        modifier = Modifier.size(64.dp),
//        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
//        error = painterResource(id = R.drawable.ic_launcher_background)
//    )

}








//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    _525_labLeanAndroidTheme {
//        Greeting("Android")
//    }
//}