package com.polinema.irigasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*

@Composable
fun IrigasiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        content = content
    )
}

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    data object Home : Screen("home", "Home", { Icon(Icons.Filled.Home, contentDescription = "Home") })
    data object Node1 : Screen("node1", "Node 1", { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Node 1") })
    data object Node2 : Screen("node2", "Node 2", { Icon(Icons.Filled.Build, contentDescription = "Node 2") })
}

data class NodeData(val flow: Double?, val temp: Double?, val level: Double?)

class MainActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        setContent {
            IrigasiTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val node1Data = remember { mutableStateOf(NodeData(null, null, null)) }
                    val node2Data = remember { mutableStateOf(NodeData(null, null, null)) }
                    val avgData = remember { mutableStateOf(NodeData(null, null, null)) }

                    // Listen for Firebase data changes
                    LaunchedEffect(Unit) {
                        database.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val node1 = snapshot.child("data_mentah").child("node1")
                                val node2 = snapshot.child("data_mentah").child("node2")
                                val rata = snapshot.child("data_mentah").child("rata_rata")

                                node1Data.value = NodeData(
                                    node1.child("flow").getValue(Double::class.java),
                                    node1.child("temp").getValue(Double::class.java),
                                    node1.child("level").getValue(Double::class.java)
                                )
                                node2Data.value = NodeData(
                                    node2.child("flow").getValue(Double::class.java),
                                    node2.child("temp").getValue(Double::class.java),
                                    node2.child("level").getValue(Double::class.java)
                                )
                                avgData.value = NodeData(
                                    rata.child("flow").getValue(Double::class.java),
                                    rata.child("temp").getValue(Double::class.java),
                                    rata.child("level").getValue(Double::class.java)
                                )
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle error if needed
                            }
                        })
                    }

                    MainScreen(avgData.value, node1Data.value, node2Data.value)
                }
            }
        }
    }
}

@Composable
fun MainScreen(node1Data: NodeData, node2Data: NodeData, avgData: NodeData) {
    val screens = listOf(Screen.Home, Screen.Node1, Screen.Node2)
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon() },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                is Screen.Home -> NodeScreen("Average Data", avgData)
                is Screen.Node1 -> NodeScreen("Node 1 Data", node1Data)
                is Screen.Node2 -> NodeScreen("Node 2 Data", node2Data)
            }
        }
    }
}

@Composable
fun NodeScreen(title: String, data: NodeData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Flow: ${data.flow ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Temp: ${data.temp ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Level: ${data.level ?: "-"}", style = MaterialTheme.typography.bodyMedium)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    val avgData = NodeData(flow = 13.65, temp = 23.8, level = 8.15)
    val node1Data = NodeData(flow = 12.3, temp = 25.6, level = 7.8)
    val node2Data = NodeData(flow = 15.0, temp = 22.0, level = 8.5)
    MainScreen(avgData, node1Data, node2Data)
}