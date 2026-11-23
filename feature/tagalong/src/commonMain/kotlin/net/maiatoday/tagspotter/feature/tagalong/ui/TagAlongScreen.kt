package net.maiatoday.tagspotter.feature.tagalong.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import net.maiatoday.tagspotter.core.a2ui.A2UIAction
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
import net.maiatoday.tagspotter.core.database.ChatSession
import net.maiatoday.tagspotter.core.database.epochMillis
import net.maiatoday.tagspotter.core.ui.MapMarker
import net.maiatoday.tagspotter.core.ui.image.ZoomableImageOverlay
import net.maiatoday.tagspotter.core.ui.image.formatImageModel
import net.maiatoday.tagspotter.feature.map.MapScreen
import net.maiatoday.tagspotter.feature.tagalong.fixtures.TagAlongMockFixtures

class TagAlongMenuState internal constructor(
    val viewModel: TagAlongViewModel,
    val availableEngines: List<A2UIRendererEngine>,
    private val forcedEngine: A2UIRendererEngine?,
    val showDebugControls: Boolean = true
) {
    var currentEngineIndex by mutableIntStateOf(0)
        private set

    var showHistorySheet by mutableStateOf(false)
    var showSchemaDialog by mutableStateOf(false)

    private val fallbackEngine: A2UIRendererEngine by lazy { KmpA2UIEngine() }

    val activeEngine: A2UIRendererEngine
        get() = forcedEngine ?: if (availableEngines.isNotEmpty()) {
            availableEngines[currentEngineIndex % availableEngines.size]
        } else {
            fallbackEngine
        }

    val canToggleEngine: Boolean
        get() = availableEngines.size > 1 && forcedEngine == null

    fun toggleEngine() {
        if (!canToggleEngine) return
        currentEngineIndex = (currentEngineIndex + 1) % availableEngines.size
        viewModel.switchEngine(activeEngine)
    }

    fun startNewChat() {
        viewModel.startNewConversation(activeEngine)
    }

    fun openHistory() {
        showHistorySheet = true
    }

    fun dismissHistory() {
        showHistorySheet = false
    }

    fun toggleDebugMode() {
        viewModel.toggleA2uiDebugMode()
    }

    fun logActiveSchema() {
        viewModel.logActiveA2uiSchema(activeEngine)
    }

    fun openSchemaDialog() {
        logActiveSchema()
        showSchemaDialog = true
    }

    fun dismissSchemaDialog() {
        showSchemaDialog = false
    }

    fun selectMockFixture(name: String, json: String) {
        viewModel.selectMockFixture(name, json)
    }
}

@Composable
fun rememberTagAlongMenuState(
    viewModel: TagAlongViewModel = koinViewModel(),
    engine: A2UIRendererEngine? = null,
    showDebugControls: Boolean = true
): TagAlongMenuState {
    val availableEngines = rememberAvailableEngines()
    return remember(viewModel, availableEngines, engine, showDebugControls) {
        TagAlongMenuState(
            viewModel = viewModel,
            availableEngines = availableEngines,
            forcedEngine = engine,
            showDebugControls = showDebugControls
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagAlongDrawerSection(
    menuState: TagAlongMenuState,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by menuState.viewModel.uiState.collectAsState()
    var showMockCatalog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "TAGALONG",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        NavigationDrawerItem(
            label = { Text("New Chat", fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = {
                menuState.startNewChat()
                onCloseDrawer()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        NavigationDrawerItem(
            label = { Text("Chat History", fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = {
                menuState.openHistory()
                onCloseDrawer()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Chat History",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        NavigationDrawerItem(
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A2UI Engine", fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = menuState.activeEngine.engineType.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            )
                            if (menuState.canToggleEngine) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⇄",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }
                        }
                    }
                }
            },
            selected = false,
            onClick = {
                if (menuState.canToggleEngine) {
                    menuState.toggleEngine()
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Switch A2UI Engine",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        NavigationDrawerItem(
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A2UI Mock Catalog", fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (showMockCatalog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showMockCatalog) "Collapse Mock Catalog" else "Expand Mock Catalog",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            selected = false,
            onClick = { showMockCatalog = !showMockCatalog },
            icon = {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "A2UI Mock Catalog",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        if (showMockCatalog) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagAlongMockFixtures.ALL_FIXTURES.forEach { (name, json) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                menuState.selectMockFixture(name, json)
                                onCloseDrawer()
                            }
                    ) {
                        Text(
                            text = "+ $name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (menuState.showDebugControls) {
            NavigationDrawerItem(
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A2UI Debug Mode", fontWeight = FontWeight.Bold)
                        Switch(
                            checked = uiState.isA2uiDebugMode,
                            onCheckedChange = { menuState.toggleDebugMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.tertiaryContainer,
                                checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    }
                },
                selected = false,
                onClick = { menuState.toggleDebugMode() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "A2UI Debug Mode",
                        tint = if (uiState.isA2uiDebugMode) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("View A2UI Schema", fontWeight = FontWeight.Bold) },
                selected = false,
                onClick = {
                    menuState.openSchemaDialog()
                    onCloseDrawer()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "View A2UI Schema",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagAlongScreen(
    viewModel: TagAlongViewModel = koinViewModel(),
    engine: A2UIRendererEngine? = null,
    showDebugControls: Boolean = true,
    menuState: TagAlongMenuState = rememberTagAlongMenuState(
        viewModel = viewModel,
        engine = engine,
        showDebugControls = showDebugControls
    ),
    onMenuClick: (() -> Unit)? = null,
    onPickPhoto: ((onPicked: (String) -> Unit) -> Unit)? = null,
    onOpenSpotDetail: ((Long) -> Unit)? = null,
    onViewOnMainMap: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeViewModel = menuState.viewModel
    val activeEngine = menuState.activeEngine
    val effectiveShowDebugControls = showDebugControls && menuState.showDebugControls

    LaunchedEffect(activeEngine) {
        activeViewModel.attachEngine(activeEngine)
    }

    var isWideLayout by remember { mutableStateOf(false) }
    val currentIsWideLayout by rememberUpdatedState(isWideLayout)
    val currentOnOpenSpotDetail by rememberUpdatedState(onOpenSpotDetail)
    val currentOnViewOnMainMap by rememberUpdatedState(onViewOnMainMap)

    LaunchedEffect(activeViewModel) {
        activeViewModel.navigationEvents.collect { event ->
            when (event) {
                is TagAlongNavEvent.OpenSpotDetail -> {
                    currentOnOpenSpotDetail?.invoke(event.spotId)
                }
                is TagAlongNavEvent.ViewOnMainMap -> {
                    if (!currentIsWideLayout) {
                        currentOnViewOnMainMap?.invoke(event.latitude, event.longitude)
                    }
                }
            }
        }
    }

    val uiState by activeViewModel.uiState.collectAsState()
    val allSessions by activeViewModel.allSessions.collectAsState()

    TagAlongScreenContent(
        uiState = uiState,
        engine = activeEngine,
        showDebugControls = effectiveShowDebugControls,
        onMenuClick = onMenuClick,
        onNewChat = { menuState.startNewChat() },
        onToggleDebugMode = { menuState.toggleDebugMode() },
        onViewSchema = { menuState.openSchemaDialog() },
        onLogSchema = { menuState.logActiveSchema() },
        onSendMessage = { activeViewModel.sendMessage(it) },
        onAction = { activeViewModel.handleAction(it) },
        onInputTextChanged = { activeViewModel.onInputTextChanged(it) },
        onClearPhoto = { activeViewModel.onClearPhoto() },
        onPickPhoto = {
            onPickPhoto?.invoke { pickedUri ->
                activeViewModel.onPhotoAttached(pickedUri)
            }
        },
        onOpenSpotDetail = onOpenSpotDetail,
        onViewOnMainMap = onViewOnMainMap,
        onWideLayoutChanged = { isWideLayout = it },
        modifier = modifier
    )

    if (menuState.showHistorySheet) {
        HistoryBottomSheet(
            sessions = allSessions,
            activeSessionId = uiState.sessionId,
            onDismissRequest = { menuState.dismissHistory() },
            onSwitchSession = { sessionId ->
                activeViewModel.switchSession(sessionId, activeEngine)
                menuState.dismissHistory()
            },
            onDeleteSession = { sessionId ->
                activeViewModel.deleteSession(sessionId, activeEngine)
            },
            onNewChat = {
                menuState.startNewChat()
                menuState.dismissHistory()
            }
        )
    }

    if (menuState.showSchemaDialog) {
        val schemaJson = remember(activeEngine) { activeEngine.getCatalogSchemaJson() }
        A2uiSchemaDialog(
            engineTypeName = activeEngine.engineType.name,
            schemaJson = schemaJson,
            onLogSchema = { menuState.logActiveSchema() },
            onDismiss = { menuState.dismissSchemaDialog() }
        )
    }
}

@Composable
fun TagAlongScreenContent(
    uiState: TagAlongUiState,
    engine: A2UIRendererEngine,
    showDebugControls: Boolean = true,
    onMenuClick: (() -> Unit)? = null,
    onNewChat: () -> Unit = {},
    onToggleDebugMode: () -> Unit = {},
    onViewSchema: (() -> Unit)? = null,
    onLogSchema: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onAction: (A2UIAction) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onClearPhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenSpotDetail: ((Long) -> Unit)? = null,
    onViewOnMainMap: ((Double, Double) -> Unit)? = null,
    onWideLayoutChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var zoomedPhotoUri by remember { mutableStateOf<String?>(null) }
    var showLocalSchemaDialog by remember { mutableStateOf(false) }

    val handleViewSchema: () -> Unit = {
        if (onViewSchema != null) {
            onViewSchema()
        } else {
            onLogSchema()
            showLocalSchemaDialog = true
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isDesktopTwoPane = maxWidth >= 760.dp

        LaunchedEffect(isDesktopTwoPane) {
            onWideLayoutChanged(isDesktopTwoPane)
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
                        showDebugControls = showDebugControls,
                        isA2uiDebugMode = uiState.isA2uiDebugMode,
                        onMenuClick = onMenuClick,
                        onNewChat = onNewChat,
                        onToggleDebugMode = onToggleDebugMode,
                        onViewSchema = handleViewSchema,
                        onLogSchema = onLogSchema,
                        inputText = uiState.inputText,
                        onInputTextChanged = onInputTextChanged,
                        attachedPhotoUri = uiState.attachedPhotoUri,
                        onClearPhoto = onClearPhoto,
                        onPickPhoto = onPickPhoto,
                        onSendMessage = onSendMessage,
                        onAction = onAction,
                        onPhotoClick = { zoomedPhotoUri = it },
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
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Right Pane: Synchronized Mini Map & Live Info (44% width)
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    MapPane(
                        latitude = if (uiState.hasExplicitMapFocus) uiState.mapCenterLat else null,
                        longitude = if (uiState.hasExplicitMapFocus) uiState.mapCenterLng else null,
                        zoomLevel = uiState.mapZoom,
                        focusedTitle = uiState.focusedTitle,
                        extraMarkers = uiState.mapMarkers,
                        onSpotClick = { spotId -> onOpenSpotDetail?.invoke(spotId) }
                    )
                }
            }
        } else {
            ChatPane(
                messages = uiState.messages,
                listState = listState,
                engine = engine,
                showDebugControls = showDebugControls,
                isA2uiDebugMode = uiState.isA2uiDebugMode,
                onMenuClick = onMenuClick,
                onNewChat = onNewChat,
                onToggleDebugMode = onToggleDebugMode,
                onViewSchema = handleViewSchema,
                onLogSchema = onLogSchema,
                inputText = uiState.inputText,
                onInputTextChanged = onInputTextChanged,
                attachedPhotoUri = uiState.attachedPhotoUri,
                onClearPhoto = onClearPhoto,
                onPickPhoto = onPickPhoto,
                onSendMessage = onSendMessage,
                onAction = onAction,
                onPhotoClick = { zoomedPhotoUri = it },
                isOffline = uiState.isOffline,
                offlineMessage = uiState.offlineMessage,
                isGenerating = uiState.isGenerating
            )
        }
    }

    val activeZoomPhoto = zoomedPhotoUri
    if (activeZoomPhoto != null) {
        Dialog(
            onDismissRequest = { zoomedPhotoUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ZoomableImageOverlay(
                imagePath = activeZoomPhoto,
                onClose = { zoomedPhotoUri = null }
            )
        }
    }
    if (showLocalSchemaDialog) {
        val schemaJson = remember(engine) { engine.getCatalogSchemaJson() }
        A2uiSchemaDialog(
            engineTypeName = engine.engineType.name,
            schemaJson = schemaJson,
            onLogSchema = onLogSchema,
            onDismiss = { showLocalSchemaDialog = false }
        )
    }
}

@Composable
private fun ChatPane(
    messages: List<TagAlongMessage>,
    listState: LazyListState,
    engine: A2UIRendererEngine,
    showDebugControls: Boolean = true,
    isA2uiDebugMode: Boolean = false,
    onMenuClick: (() -> Unit)? = null,
    onNewChat: () -> Unit = {},
    onToggleDebugMode: () -> Unit = {},
    onViewSchema: () -> Unit = {},
    onLogSchema: () -> Unit = {},
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    attachedPhotoUri: String?,
    onClearPhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onSendMessage: (String) -> Unit,
    onAction: (A2UIAction) -> Unit,
    onPhotoClick: (String) -> Unit = {},
    isOffline: Boolean = false,
    offlineMessage: String? = null,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val submitMessage: () -> Unit = {
        if (inputText.isNotBlank() || attachedPhotoUri != null) {
            onSendMessage(inputText)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Clean Top Bar with Hamburger Menu & Essential Actions (standard 24.dp icons in 48.dp targets)
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TAGALONG",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (showDebugControls) {
                        IconButton(onClick = onToggleDebugMode) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isA2uiDebugMode) {
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = if (isA2uiDebugMode) "Disable A2UI Debug" else "Enable A2UI Debug",
                                    tint = if (isA2uiDebugMode) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNewChat) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }
        }

        // Inline A2UI Debug Banner (when debug toggle is ON)
        if (showDebugControls && isA2uiDebugMode) {
            A2uiDebugBanner(
                engineTypeName = engine.engineType.name,
                onViewSchema = onViewSchema,
                onLogSchema = onLogSchema
            )
        }

        // Inline Offline Fallback Banner
        if (isOffline) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
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
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = offlineMessage ?: "Agent running locally. Cloud generative features paused.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
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
                    isA2uiDebugMode = showDebugControls && isA2uiDebugMode,
                    onAction = onAction,
                    onPhotoClick = onPhotoClick
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TagAlong is analyzing urban art...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Input & Attachment Bar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (attachedPhotoUri != null) {
                    val previewModel = remember(attachedPhotoUri) {
                        formatImageModel(attachedPhotoUri, "")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = previewModel,
                                    contentDescription = "Attached photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPhotoClick(attachedPhotoUri) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Photo Attached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "✕",
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .clickable { onClearPhoto() }
                                        .padding(horizontal = 2.dp)
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
                            contentColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("📷")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChanged,
                        placeholder = {
                            Text(
                                text = "Ask TagAlong...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { keyEvent ->
                                if (
                                    keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) &&
                                    !keyEvent.isShiftPressed
                                ) {
                                    submitMessage()
                                    true
                                } else {
                                    false
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitMessage() }),
                        singleLine = true
                    )

                    Button(
                        onClick = submitMessage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
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
    isA2uiDebugMode: Boolean = false,
    onAction: (A2UIAction) -> Unit,
    onPhotoClick: (String) -> Unit = {}
) {
    val isUser = message.sender == TagAlongSender.USER
    val codeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val codeFg = MaterialTheme.colorScheme.tertiaryContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!message.text.isNullOrBlank()) {
            val formattedText = remember(message.text, codeBg, codeFg) {
                parseInlineMarkdown(
                    text = message.text,
                    codeBackground = codeBg,
                    codeColor = codeFg
                )
            }
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUser) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
            ) {
                Text(
                    text = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!message.photoUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            val imageModel = remember(message.photoUri) {
                formatImageModel(message.photoUri, "")
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPhotoClick(message.photoUri) }
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Attached photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .heightIn(min = 100.dp, max = 180.dp)
                        .fillMaxWidth(0.65f)
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
                if (isA2uiDebugMode && !message.a2uiPayload.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val surfaceJson = remember(message.a2uiPayload, surfaceId) {
                        extractSurfaceJsonForDebug(message.a2uiPayload, surfaceId)
                    }
                    A2uiDebugJsonCard(
                        label = surfaceId,
                        rawJson = surfaceJson
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
            if (isA2uiDebugMode) {
                Spacer(modifier = Modifier.height(6.dp))
                A2uiDebugJsonCard(
                    label = "payload",
                    rawJson = message.a2uiPayload
                )
            }
        }
    }
}

@Composable
private fun MapPane(
    latitude: Double?,
    longitude: Double?,
    zoomLevel: Double,
    focusedTitle: String,
    extraMarkers: List<MapMarker>,
    onSpotClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Map Top Bar (aligned with ChatPane's 56.dp top bar and safe against landscape cutouts/nav bar)
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                        .height(56.dp)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE URBAN MAP",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = focusedTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (latitude != null && longitude != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Text(
                                text = "${(latitude * 1000).toInt() / 1000.0}, ${(longitude * 1000).toInt() / 1000.0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }
        }

        // Live Map Container reusing MapScreen (without bulky floating filter overlays in the compact pane)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            MapScreen(
                onSpotClick = onSpotClick,
                initialLatitude = latitude,
                initialLongitude = longitude,
                zoomLevel = zoomLevel,
                extraMarkers = extraMarkers,
                applyStatusBarInsets = false,
                showFilterControls = false,
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
                    color = MaterialTheme.colorScheme.primaryContainer,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = onNewChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                }
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
                                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
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
