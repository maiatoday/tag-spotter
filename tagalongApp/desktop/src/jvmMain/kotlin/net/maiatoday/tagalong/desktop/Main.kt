package net.maiatoday.tagalong.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import net.maiatoday.tagspotter.core.a2ui.catalog.NeonA2UICatalog
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
import net.maiatoday.tagspotter.core.agent.createDesktopTagAlongRunner
import net.maiatoday.tagspotter.core.database.LocalSpotRepository
import net.maiatoday.tagspotter.core.database.RoomChatRepository
import net.maiatoday.tagspotter.core.database.configureSpotDatabase
import net.maiatoday.tagspotter.core.database.getDatabaseBuilder
import net.maiatoday.tagspotter.core.database.getTagAlongDatabaseBuilder
import net.maiatoday.tagspotter.core.photo.RealPhotoProcessor
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository
import net.maiatoday.tagspotter.core.sync.NonWebSyncManager
import net.maiatoday.tagspotter.core.ui.theme.MyApplicationTheme
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongScreen
import net.maiatoday.tagspotter.feature.tagalong.ui.TagAlongViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(width = 1150.dp, height = 820.dp)
    val engine = remember { KmpA2UIEngine() }
    val catalog = remember { NeonA2UICatalog() }

    val chatRepository = remember {
        val db = getTagAlongDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
        RoomChatRepository(db.chatDao())
    }

    val hybridSpotRepository = remember {
        val spotDb = getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .configureSpotDatabase()
            .fallbackToDestructiveMigration(true)
            .build()
        val localRepo = LocalSpotRepository(spotDb.spotDao(), RealPhotoProcessor())
        HybridSpotRepository(localRepository = localRepo, syncManager = NonWebSyncManager(localRepo))
    }

    val viewModel = remember {
        val secrets = DesktopSecretsProvider()
        val apiKey = secrets.getGeminiApiKey()
        val runner = if (apiKey.isNotBlank()) {
            try {
                createDesktopTagAlongRunner(
                    apiKey = apiKey,
                    hybridSpotRepository = hybridSpotRepository
                )
            } catch (e: Exception) {
                println("Warning: failed to create desktop TagAlong runner: ${e.message}")
                null
            }
        } else {
            println("Info: No GEMINI_API_KEY found, running in offline/catalog mode.")
            null
        }

        TagAlongViewModel(
            runner = runner,
            chatRepository = chatRepository,
            hybridSpotRepository = hybridSpotRepository
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "TagAlong Desktop — Agentic Street Art Companion",
    ) {
        MyApplicationTheme {
            TagAlongScreen(
                viewModel = viewModel,
                engine = engine,
                catalog = catalog,
                onPickPhoto = { onPhotoPicked ->
                    val fileDialog = FileDialog(null as Frame?, "Select Street Art Photo", FileDialog.LOAD)
                    fileDialog.setFilenameFilter { _, name ->
                        val lower = name.lowercase()
                        lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
                    }
                    fileDialog.isVisible = true
                    val directory = fileDialog.directory
                    val file = fileDialog.file
                    if (directory != null && file != null) {
                        val selectedFile = File(directory, file)
                        onPhotoPicked(selectedFile.toURI().toString())
                    }
                }
            )
        }
    }
}
