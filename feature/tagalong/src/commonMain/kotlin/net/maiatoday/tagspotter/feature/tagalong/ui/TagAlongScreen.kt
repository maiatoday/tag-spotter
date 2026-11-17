package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
import net.maiatoday.tagspotter.core.database.ChatSession
import net.maiatoday.tagspotter.core.database.epochMillis
import net.maiatoday.tagspotter.core.ui.MapMarker
import net.maiatoday.tagspotter.core.ui.SpotMapView
import net.maiatoday.tagspotter.core.ui.theme.DarkBackground
import net.maiatoday.tagspotter.core.ui.theme.DarkPrimaryContainer
import net.maiatoday.tagspotter.core.ui.theme.DarkSecondaryContainer
import net.maiatoday.tagspotter.core.ui.theme.DarkSurfaceVariant
import net.maiatoday.tagspotter.core.ui.theme.DarkTertiaryContainer
import net.maiatoday.tagspotter.feature.tagalong.fixtures.TagAlongMockFixtures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagAlongScreen(
    viewModel: TagAlongViewModel = koinViewModel(),
    engine: A2UIRendererEngine? = null,
    onPickPhoto: ((onPicked: (String) -> Unit) -> Unit)? = null,
    onOpenSpotDetail: ((Long) -> Unit)? = null,
    onViewOnMainMap: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val availableEngines = rememberAvailableEngines()
    var currentEngineIndex by remember { mutableIntStateOf(0) }
    val activeEngine = engine ?: if (availableEngines.isNotEmpty()) {
        availableEngines[currentEngineIndex % availableEngines.size]
    } else {
        remember { KmpA2UIEngine() }
    }

    LaunchedEffect(activeEngine) {
        viewModel.attachEngine(activeEngine)
    }

    val uiState by viewModel.uiState.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    var showHistorySheet by remember { mutableStateOf(false) }

    TagAlongScreenContent(
        uiState = uiState,
        engine = activeEngine,
        onToggleEngine = if (availableEngines.size > 1 && engine == null) {
            {
                currentEngineIndex = (currentEngineIndex + 1) % availableEngines.size
                val nextEngine = availableEngines[currentEngineIndex]
                viewModel.switchEngine(nextEngine)
            }
        } else null,
        onSendMessage = { viewModel.sendMessage(it) },
        onSelectMockFixture = { name, json -> viewModel.selectMockFixture(name, json) },
        onAction = { viewModel.handleAction(it) },
        onInputTextChanged = { viewModel.onInputTextChanged(it) },
        onClearPhoto = { viewModel.onClearPhoto() },
        onPickPhoto = {
            onPickPhoto?.invoke { pickedUri ->
                viewModel.onPhotoAttached(pickedUri)
            }
        },
        onNewChat = { viewModel.startNewConversation(activeEngine) },
        onOpenHistory = { showHistorySheet = true },
        onOpenSpotDetail = onOpenSpotDetail,
        onViewOnMainMap = onViewOnMainMap,
        modifier = modifier
    )

    if (showHistorySheet) {
        HistoryBottomSheet(
            sessions = allSessions,
            activeSessionId = uiState.sessionId,
            onDismissRequest = { showHistorySheet = false },
            onSwitchSession = { sessionId ->
                viewModel.switchSession(sessionId, activeEngine)
                showHistorySheet = false
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteSession(sessionId, activeEngine)
            },
            onNewChat = {
                viewModel.startNewConversation(activeEngine)
                showHistorySheet = false
            }
        )
    }
}

@Composable
fun TagAlongScreenContent(
    uiState: TagAlongUiState,
    engine: A2UIRendererEngine,
    onToggleEngine: (() -> Unit)? = null,
    onSendMessage: (String) -> Unit,
    onSelectMockFixture: (name: String, json: String) -> Unit,
    onAction: (A2UIAction) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onClearPhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onNewChat: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSpotDetail: ((Long) -> Unit)? = null,
    onViewOnMainMap: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isDesktopTwoPane = maxWidth >= 760.dp

        val wrappedOnAction: (A2UIAction) -> Unit = { action ->
            onAction(action)
            if (!isDesktopTwoPane) {
                when (action) {
                    is A2UIAction.ViewOnMap -> {
                        if (action.latitude != 0.0 && action.longitude != 0.0) {
                            onViewOnMainMap?.invoke(action.latitude, action.longitude)
                        }
                    }
                    is A2UIAction.NavigateToStop -> {
                        if (action.latitude != 0.0 && action.longitude != 0.0) {
                            onViewOnMainMap?.invoke(action.latitude, action.longitude)
                        }
                    }
                    else -> {}
                }
            }
            if (action is A2UIAction.OpenSpotDetail) {
                val spotId = action.spotId.toLongOrNull()
                if (spotId != null) {
                    onOpenSpotDetail?.invoke(spotId)
                }
            } else if (action is A2UIAction.ClientEvent) {
                if (action.name == "openSpotDetail" || action.name == "open_spot_detail" || action.name == "openDetail") {
                    val spotId = (action.context["spotId"]?.toString() ?: action.context["id"]?.toString())?.toLongOrNull()
                    if (spotId != null) {
                        onOpenSpotDetail?.invoke(spotId)
                    }
                }
            }
        }

        if (isDesktopTwoPane) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Chat Stream & A2UI Render host (56% width)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                ) {
                    ChatPane(
                        messages = uiState.messages,
                        listState = listState,
                        engine = engine,
                        onToggleEngine = onToggleEngine,
                        inputText = uiState.inputText,
                        onInputTextChanged = onInputTextChanged,
                        attachedPhotoUri = uiState.attachedPhotoUri,
                        onClearPhoto = onClearPhoto,
                        onPickPhoto = onPickPhoto,
                        onSendMessage = onSendMessage,
                        onSelectMockFixture = onSelectMockFixture,
                        onAction = wrappedOnAction,
                        onNewChat = onNewChat,
                        onOpenHistory = onOpenHistory,
                        isOffline = uiState.isOffline,
                        offlineMessage = uiState.offlineMessage,
                        isGenerating = uiState.isGenerating
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(DarkSurfaceVariant)
                )

                // Right Pane: Synchronized Mini Map & Live Info (44% width)
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    MapPane(
                        latitude = uiState.mapCenterLat,
                        longitude = uiState.mapCenterLng,
                        zoomLevel = uiState.mapZoom,
                        focusedTitle = uiState.focusedTitle,
                        markers = uiState.mapMarkers
                    )
                }
            }
        } else {
            ChatPane(
                messages = uiState.messages,
                listState = listState,
                engine = engine,
                onToggleEngine = onToggleEngine,
                inputText = uiState.inputText,
                onInputTextChanged = onInputTextChanged,
                attachedPhotoUri = uiState.attachedPhotoUri,
                onClearPhoto = onClearPhoto,
                onPickPhoto = onPickPhoto,
                onSendMessage = onSendMessage,
                onSelectMockFixture = onSelectMockFixture,
                onAction = wrappedOnAction,
                onNewChat = onNewChat,
                onOpenHistory = onOpenHistory,
                isOffline = uiState.isOffline,
                offlineMessage = uiState.offlineMessage,
                isGenerating = uiState.isGenerating
            )
        }
    }
}

@Composable
private fun ChatPane(
    messages: List<TagAlongMessage>,
    listState: LazyListState,
    engine: A2UIRendererEngine,
    onToggleEngine: (() -> Unit)? = null,
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    attachedPhotoUri: String?,
    onClearPhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSelectMockFixture: (name: String, json: String) -> Unit,
    onAction: (A2UIAction) -> Unit,
    onNewChat: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    isOffline: Boolean = false,
    offlineMessage: String? = null,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // App Header Banner
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, DarkPrimaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DarkPrimaryContainer,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TAGALONG",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = DarkPrimaryContainer,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COMPANION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkSecondaryContainer
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkTertiaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, DarkTertiaryContainer),
                            modifier = if (onToggleEngine != null) {
                                Modifier.clip(RoundedCornerShape(6.dp)).clickable { onToggleEngine() }
                            } else Modifier
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = engine.engineType.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = DarkTertiaryContainer
                                )
                                if (onToggleEngine != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "⇄",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = DarkTertiaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Chat History",
                                tint = DarkPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onNewChat,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = DarkPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fast Fixture Selector Chips
                Text(
                    text = "A2UI Mock Catalog Tester:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagAlongMockFixtures.ALL_FIXTURES.forEach { (name, json) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, DarkPrimaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectMockFixture(name, json) }
                        ) {
                            Text(
                                text = "+ $name",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Inline Offline Fallback Banner
        if (isOffline) {
            Surface(
                color = Color(0xFF332200),
                border = BorderStroke(1.dp, Color(0xFFFFB300)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "OFFLINE FALLBACK MODE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = offlineMessage ?: "Agent running locally. Cloud generative features paused.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    engine = engine,
                    onAction = onAction
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DarkPrimaryContainer,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TagAlong is analyzing urban art...",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkPrimaryContainer
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Input & Attachment Bar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, DarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (attachedPhotoUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSecondaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, DarkSecondaryContainer),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🖼️ Photo Attached", style = MaterialTheme.typography.labelSmall, color = DarkSecondaryContainer)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "✕",
                                    color = DarkSecondaryContainer,
                                    modifier = Modifier.clickable { onClearPhoto() }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickPhoto,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkTertiaryContainer
                        ),
                        border = BorderStroke(1.dp, DarkTertiaryContainer),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("📷")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChanged,
                        placeholder = { Text("Ask TagAlong (e.g., 'Find neon murals in Mitte')...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkPrimaryContainer,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = { onSendMessage(inputText) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimaryContainer,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: TagAlongMessage,
    engine: A2UIRendererEngine,
    onAction: (A2UIAction) -> Unit
) {
    val isUser = message.sender == TagAlongSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!message.text.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) DarkSecondaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUser) DarkSecondaryContainer else DarkSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (message.photoUri != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSecondaryContainer.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, DarkSecondaryContainer)
            ) {
                Text(
                    text = "📎 Attached Photo: ${message.photoUri}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkSecondaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        if (message.surfaceIds.isNotEmpty()) {
            message.surfaceIds.forEach { surfaceId ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    engine.RenderSurface(
                        surfaceId = surfaceId,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (message.a2uiPayload != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                engine.RenderSurface(
                    payloadJson = message.a2uiPayload,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MapPane(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    focusedTitle: String,
    markers: List<MapMarker>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Map Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, DarkTertiaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE URBAN MAP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DarkTertiaryContainer
                    )
                    Text(
                        text = focusedTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkTertiaryContainer.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, DarkTertiaryContainer)
                ) {
                    Text(
                        text = "${(latitude * 1000).toInt() / 1000.0}, ${(longitude * 1000).toInt() / 1000.0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Live Map Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            SpotMapView(
                latitude = latitude,
                longitude = longitude,
                zoomLevel = zoomLevel,
                markers = markers,
                useDarkMap = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    sessions: List<ChatSession>,
    activeSessionId: String,
    onDismissRequest: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHAT HISTORY",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = DarkPrimaryContainer,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = onNewChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPrimaryContainer,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Chat",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No conversation history yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.sessionId }) { session ->
                        val isActive = session.sessionId == activeSessionId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) {
                                    DarkPrimaryContainer.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) DarkPrimaryContainer else DarkSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchSession(session.sessionId) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = session.title.ifBlank { "Street Art Exploration" },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isActive) DarkPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = DarkPrimaryContainer.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, DarkPrimaryContainer)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = DarkPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatSessionTime(session.lastActiveAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSession(session.sessionId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatSessionTime(timestamp: Long, now: Long = epochMillis()): String {
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        diff < 7 * 86400_000L -> "${diff / 86400_000L}d ago"
        else -> "${diff / (7 * 86400_000L)}w ago"
    }
}
