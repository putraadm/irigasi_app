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
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun MainScreen(avgData: NodeData, node1Data: NodeData, node2Data: NodeData) {
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
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DataCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Filled.WaterDrop, contentDescription = "Flow", tint = Color(0xFF7EC8F5)) },
                label = "Flow",
                value = data.flow?.toString() ?: "-"
            )
            DataCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Filled.Thermostat, contentDescription = "Temp", tint = Color(0xFFDD7B4B)) },
                label = "Temp",
                value = data.temp?.toString() ?: "-"
            )
            DataCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Filled.Science, contentDescription = "Level", tint = Color(0xFF8C7BFF)) },
                label = "Level",
                value = data.level?.toString() ?: "-"
            )
        }

        Text(text = "Data Over Time", style = MaterialTheme.typography.headlineSmall)

        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            dataPoints = listOf(0f, 10f, 50f, 70f, 65f, 80f, 120f, 160f, 170f)
        )
    }
}

@Composable
fun DataCard(modifier: Modifier = Modifier, icon: @Composable () -> Unit, label: String, value: String) {
    Card(
        modifier = modifier
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun LineChart(modifier: Modifier = Modifier, dataPoints: List<Float>) {
    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val padding = 5.dp.toPx()

        // Draw background rounded rect
        drawRoundRect(
            color = Color(0xFFF8F8FF),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            size = size
        )

        // Draw grid lines
        val gridLines = 5
        val stepY = (height - 2 * padding) / gridLines
        val stepX = (width - 2 * padding) / (dataPoints.size - 1)

        val gridColor = Color.LightGray.copy(alpha = 0.3f)
        for (i in 0..gridLines) {
            val y = padding + i * stepY
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Draw line chart
        val maxValue = dataPoints.maxOrNull() ?: 0f
        if (maxValue == 0f) return@Canvas

        val points = dataPoints.mapIndexed { index, value ->
            Offset(
                x = padding + index * stepX,
                y = height - padding - (value / maxValue) * (height - 2 * padding)
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (point in points.drop(1)) {
                lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF8C7BFF),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // Draw fill under the line
        val fillPath = Path().apply {
            moveTo(points.first().x, height - padding)
            lineTo(points.first().x, points.first().y)
            for (point in points.drop(1)) {
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, height - padding)
            close()
        }

        drawPath(
            path = fillPath,
            color = Color(0xFF8C7BFF).copy(alpha = 0.2f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    val avgData = NodeData(flow = 0.0, temp = 27.4, level = 197.3)
    val node1Data = NodeData(flow = 12.3, temp = 25.6, level = 7.8)
    val node2Data = NodeData(flow = 15.0, temp = 22.0, level = 8.5)
    MainScreen(avgData, node1Data, node2Data)
}
