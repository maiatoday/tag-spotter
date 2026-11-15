package net.maiatoday.tagalong.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.maiatoday.tagspotter.core.a2ui.A2UIEngineType
import net.maiatoday.tagspotter.core.a2ui.A2UIRendererEngine
import net.maiatoday.tagspotter.core.a2ui.catalog.NeonA2UICatalog
import net.maiatoday.tagspotter.core.a2ui.engine.JetpackA2UIEngine
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongScreen
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                val viewModel: TagAlongViewModel = koinViewModel()
                val catalog = remember { NeonA2UICatalog() }

                val kmpEngine = remember { KmpA2UIEngine() }
                val jetpackEngine = remember { JetpackA2UIEngine() }

                var currentEngineType by remember { mutableStateOf(A2UIEngineType.A2UI_KMP_BINARYTAPE) }
                val currentEngine: A2UIRendererEngine = when (currentEngineType) {
                    A2UIEngineType.A2UI_KMP_BINARYTAPE -> kmpEngine
                    A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL -> jetpackEngine
                }

                var showDevDialog by remember { mutableStateOf(false) }

                var pendingPhotoCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        pendingPhotoCallback?.invoke(uri.toString())
                    }
                    pendingPhotoCallback = null
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TAGALONG",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.2.sp,
                                        color = Color(0xFF00FFCC)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• AI Companion",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            },
                            actions = {
                                // Engine Indicator Chip
                                FilterChip(
                                    selected = true,
                                    onClick = { showDevDialog = true },
                                    label = {
                                        Text(
                                            text = if (currentEngineType == A2UIEngineType.A2UI_KMP_BINARYTAPE) "a2ui-kmp" else "androidx.a2ui",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (currentEngineType == A2UIEngineType.A2UI_KMP_BINARYTAPE) Color(0xFF00FFCC) else Color(0xFFFF007F)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DeveloperMode,
                                            contentDescription = "A2UI Engine Switcher",
                                            modifier = Modifier.size(14.dp),
                                            tint = if (currentEngineType == A2UIEngineType.A2UI_KMP_BINARYTAPE) Color(0xFF00FFCC) else Color(0xFFFF007F)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1E1E28)
                                    )
                                )

                                IconButton(onClick = { showDevDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Dev Settings",
                                        tint = Color(0xFFE0E0E0)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF111118)
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        TagAlongScreen(
                            viewModel = viewModel,
                            engine = currentEngine,
                            catalog = catalog,
                            onPickPhoto = { onPicked ->
                                pendingPhotoCallback = onPicked
                                photoPickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (showDevDialog) {
                    DevSettingsDialog(
                        currentEngineType = currentEngineType,
                        onSelectEngine = {
                            currentEngineType = it
                            showDevDialog = false
                        },
                        onDismiss = { showDevDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun DevSettingsDialog(
    currentEngineType: A2UIEngineType,
    onSelectEngine: (A2UIEngineType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ A2UI Dual Engine Runtime Switcher",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FFCC),
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select active A2UI generative rendering engine to evaluate performance and fidelity side-by-side:",
                    fontSize = 13.sp,
                    color = Color(0xFFCCCCCC)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: a2ui-kmp
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentEngineType == A2UIEngineType.A2UI_KMP_BINARYTAPE) Color(0xFF1B2E28) else Color(0xFF1E1E24),
                    onClick = { onSelectEngine(A2UIEngineType.A2UI_KMP_BINARYTAPE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentEngineType == A2UIEngineType.A2UI_KMP_BINARYTAPE,
                            onClick = { onSelectEngine(A2UIEngineType.A2UI_KMP_BINARYTAPE) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "a2ui-kmp (BinaryTape)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Kotlin Multiplatform cross-platform engine in commonMain",
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: androidx.a2ui
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentEngineType == A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL) Color(0xFF321E28) else Color(0xFF1E1E24),
                    onClick = { onSelectEngine(A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentEngineType == A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL,
                            onClick = { onSelectEngine(A2UIEngineType.ANDROIDX_JETPACK_OFFICIAL) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "androidx.a2ui (Official Jetpack)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF007F),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Google official Jetpack A2UI runtime adapter in androidMain",
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF00FFCC))
            }
        },
        containerColor = Color(0xFF12121A)
    )
}
