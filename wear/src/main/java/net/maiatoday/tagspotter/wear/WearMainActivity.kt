package net.maiatoday.tagspotter.wear

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.SpotDetails

class WearMainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private val activityScope = CoroutineScope(Dispatchers.Main)
    private var spotsState = mutableStateListOf<SpotDetails>()
    private var isLoadingState = mutableStateOf(true)
    private var errorState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Wearable.getMessageClient(this).addListener(this)
        requestNearbySpots()

        setContent {
            WearAppTheme {
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "list"
                ) {
                    composable("list") {
                        SpotListScreen(
                            spots = spotsState,
                            isLoading = isLoadingState.value,
                            error = errorState.value,
                            onRefresh = { requestNearbySpots() },
                            onSpotSelect = { spotDetails ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("spot_details", spotDetails)
                                navController.navigate("detail")
                            }
                        )
                    }
                    composable("detail") {
                        val spotDetails = navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<SpotDetails>("spot_details")
                        
                        if (spotDetails != null) {
                            SpotDetailScreen(
                                spotDetails = spotDetails,
                                onOpenOnPhone = { sendOpenOnPhoneMessage(spotDetails.spot.id) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No Details Found", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/nearby_spots_response") {
            try {
                val json = String(messageEvent.data, Charsets.UTF_8)
                val list = Json.decodeFromString<List<SpotDetails>>(json)
                spotsState.clear()
                spotsState.addAll(list)
                isLoadingState.value = false
                errorState.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                errorState.value = "Failed to parse spots."
                isLoadingState.value = false
            }
        }
    }

    private fun requestNearbySpots() {
        isLoadingState.value = true
        errorState.value = null
        activityScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@WearMainActivity)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        errorState.value = "No phone connected."
                        isLoadingState.value = false
                    }
                    return@launch
                }
                
                // Request from each connected phone node
                for (node in nodes) {
                    Wearable.getMessageClient(this@WearMainActivity)
                        .sendMessage(node.id, "/query_nearby_spots", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorState.value = "Failed to search phone."
                    isLoadingState.value = false
                }
            }
        }
    }

    private fun sendOpenOnPhoneMessage(spotId: Long) {
        activityScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@WearMainActivity)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WearMainActivity, "No phone connected.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val payload = spotId.toString().toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    Wearable.getMessageClient(this@WearMainActivity)
                        .sendMessage(node.id, "/open_on_phone", payload)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WearMainActivity, "Opening on phone...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WearMainActivity, "Connection failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFF00FFCC), // Urban electric neon cyan
            secondary = Color(0xFFFF007F), // Neon pink
            background = Color(0xFF121212), // Dark charcoal
            onPrimary = Color.Black,
            onSecondary = Color.White
        ),
        content = content
    )
}

@Composable
fun SpotListScreen(
    spots: List<SpotDetails>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onSpotSelect: (SpotDetails) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Searching...",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
        } else if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Text("Retry", fontSize = 10.sp)
                }
            }
        } else if (spots.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No spots found\nwithin 10km",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Text("Refresh", fontSize = 10.sp)
                }
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "NEARBY SPOTS",
                        style = MaterialTheme.typography.caption1,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(spots) { details ->
                    Chip(
                        onClick = { onSpotSelect(details) },
                        label = { Text(details.spot.description, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        secondaryLabel = { Text(details.spot.category.uppercase(), color = Color.Gray) },
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color(0xFF222222),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SpotDetailScreen(
    spotDetails: SpotDetails,
    onOpenOnPhone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(12.dp)
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = spotDetails.spot.category.uppercase(),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    text = spotDetails.spot.description,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (spotDetails.spot.tags.isNotEmpty()) {
                item {
                    Text(
                        text = spotDetails.spot.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Button(
                    onClick = onOpenOnPhone,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Open on Phone", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
