package net.maiatoday.tagspotter.wear

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
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
    private var selectedSpotPhotoState = mutableStateOf<Bitmap?>(null)
    private var externalNavigateToSpot = mutableStateOf<SpotDetails?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadCachedSpots()
        Wearable.getMessageClient(this).addListener(this)
        requestNearbySpots()
        handleIntent(intent)

        setContent {
            WearAppTheme {
                val navController = rememberSwipeDismissableNavController()

                LaunchedEffect(externalNavigateToSpot.value) {
                    val spotDetails = externalNavigateToSpot.value
                    if (spotDetails != null) {
                        selectedSpotPhotoState.value = null
                        val jsonStr = Json.encodeToString(spotDetails)
                        navController.currentBackStackEntry?.savedStateHandle?.set("spot_details", jsonStr)
                        if (navController.currentBackStackEntry?.destination?.route == "detail") {
                            navController.popBackStack()
                        }
                        navController.navigate("detail")
                        externalNavigateToSpot.value = null
                    }
                }

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
                                selectedSpotPhotoState.value = null
                                requestSpotPhoto(spotDetails.spot.id)
                                val jsonStr = Json.encodeToString(spotDetails)
                                navController.currentBackStackEntry?.savedStateHandle?.set("spot_details", jsonStr)
                                navController.navigate("detail")
                            }
                        )
                    }
                    composable("detail") {
                        val spotDetailsJson = navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.get<String>("spot_details")
                        val spotDetails = remember(spotDetailsJson) {
                            spotDetailsJson?.let { Json.decodeFromString<SpotDetails>(it) }
                        }
                        
                        if (spotDetails != null) {
                            SpotDetailScreen(
                                spotDetails = spotDetails,
                                photo = selectedSpotPhotoState.value,
                                onOpenOnPhone = { sendOpenOnPhoneMessage(spotDetails.spot.id) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_details_found), color = Color.Gray)
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
        when (messageEvent.path) {
            "/nearby_spots_response" -> {
                try {
                    val json = String(messageEvent.data, Charsets.UTF_8)
                    val list = Json.decodeFromString<List<SpotDetails>>(json)
                    spotsState.clear()
                    spotsState.addAll(list)
                    isLoadingState.value = false
                    errorState.value = null

                    // Cache the spots JSON in SharedPreferences
                    val sharedPref = getSharedPreferences("tagspotter_wear_prefs", MODE_PRIVATE)
                    sharedPref.edit {
                        putString("cached_spots_json", json)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorState.value = getString(R.string.failed_parse_spots)
                    isLoadingState.value = false
                }
            }
            "/show_spot" -> {
                try {
                    val json = String(messageEvent.data, Charsets.UTF_8)
                    val spotDetails = Json.decodeFromString<SpotDetails>(json)
                    activityScope.launch {
                        externalNavigateToSpot.value = spotDetails
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "/spot_photo" -> {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(messageEvent.data, 0, messageEvent.data.size)
                    activityScope.launch {
                        selectedSpotPhotoState.value = bitmap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun loadCachedSpots() {
        try {
            val sharedPref = getSharedPreferences("tagspotter_wear_prefs", MODE_PRIVATE)
            val json = sharedPref.getString("cached_spots_json", null)
            if (json != null) {
                val list = Json.decodeFromString<List<SpotDetails>>(json)
                spotsState.clear()
                spotsState.addAll(list)
                isLoadingState.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val json = intent.getStringExtra("EXTRA_SPOT_DETAILS_JSON")
        if (json != null) {
            try {
                val spotDetails = Json.decodeFromString<SpotDetails>(json)
                externalNavigateToSpot.value = spotDetails
            } catch (e: Exception) {
                e.printStackTrace()
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
                        errorState.value = getString(R.string.no_phone_connected)
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
                    errorState.value = getString(R.string.failed_search_phone)
                    isLoadingState.value = false
                }
            }
        }
    }

    private fun requestSpotPhoto(spotId: Long) {
        Log.d("WearMainActivity", "requestSpotPhoto called with spotId=$spotId")
        activityScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@WearMainActivity)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    Log.d("WearMainActivity", "No nodes connected to request spot photo.")
                    return@launch
                }
                
                val payload = spotId.toString().toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    Wearable.getMessageClient(this@WearMainActivity)
                        .sendMessage(node.id, "/request_spot_photo", payload)
                    Log.d("WearMainActivity", "Requested photo from node: ${node.id}")
                }
            } catch (e: Exception) {
                Log.e("WearMainActivity", "Error requesting spot photo", e)
            }
        }
    }

    private fun sendOpenOnPhoneMessage(spotId: Long) {
        Log.d("WearMainActivity", "sendOpenOnPhoneMessage called with spotId=$spotId")
        activityScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@WearMainActivity)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                Log.d("WearMainActivity", "Found connected nodes: ${nodes.size}")
                if (nodes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Log.d("WearMainActivity", "No nodes connected, showing toast")
                        Toast.makeText(this@WearMainActivity, getString(R.string.no_phone_connected), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val payload = spotId.toString().toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    Log.d("WearMainActivity", "Sending /open_on_phone to node: ${node.id} (${node.displayName})")
                    Wearable.getMessageClient(this@WearMainActivity)
                        .sendMessage(node.id, "/open_on_phone", payload)
                    Log.d("WearMainActivity", "Sent successfully to node: ${node.id}")
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WearMainActivity, getString(R.string.opening_on_phone), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("WearMainActivity", "Error sending open on phone message", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WearMainActivity, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SpotListScreen(
    spots: List<SpotDetails>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onSpotSelect: (SpotDetails) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = onRefresh
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(8.dp)
            .pullRefresh(pullRefreshState),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading && spots.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.searching_loading),
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
                    Text(stringResource(R.string.retry_btn), fontSize = 10.sp)
                }
            }
        } else if (spots.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.no_starred_spots_nearby),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Text(stringResource(R.string.refresh_btn), fontSize = 10.sp)
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
                        text = stringResource(R.string.starred_spots_title),
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

        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun SpotDetailScreen(
    spotDetails: SpotDetails,
    photo: Bitmap?,
    onOpenOnPhone: () -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val imageHeight = (configuration.screenHeightDp * 2 / 3).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (photo != null) {
                item {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = stringResource(R.string.content_desc_spot_photo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item {
                Text(
                    text = spotDetails.spot.category.uppercase(),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            item {
                Text(
                    text = spotDetails.spot.description,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            if (spotDetails.spot.tags.isNotEmpty()) {
                item {
                    Text(
                        text = spotDetails.spot.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Button(
                    onClick = {
                        val lat = spotDetails.spot.latitude
                        val lon = spotDetails.spot.longitude
                        val uri = "geo:$lat,$lon".toUri()
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.toast_maps_app_not_found), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.btn_navigate_on_watch), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            item {
                Button(
                    onClick = onOpenOnPhone,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF222222),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.btn_open_on_phone), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
