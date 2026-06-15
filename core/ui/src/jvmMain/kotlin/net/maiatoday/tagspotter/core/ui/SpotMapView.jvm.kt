package net.maiatoday.tagspotter.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import javax.swing.JPanel
import java.awt.BorderLayout

class WebBridge(
    private val onMapClick: ((Double, Double) -> Unit)?,
    private val onMapReady: (() -> Unit)?
) {
    fun onMapClick(lat: Double, lng: Double) {
        onMapClick?.invoke(lat, lng)
    }

    fun onMapReady() {
        onMapReady?.invoke()
    }
}

@Composable
actual fun SpotMapView(
    latitude: Double,
    longitude: Double,
    zoomLevel: Double,
    markers: List<MapMarker>,
    useDarkMap: Boolean,
    modifier: Modifier,
    radiusCircleCenterLatitude: Double?,
    radiusCircleCenterLongitude: Double?,
    radiusCircleMeters: Double,
    onMapClick: ((Double, Double) -> Unit)?,
    onMapReady: (() -> Unit)?
) {
    // Initialize JavaFX toolkit by creating the JFXPanel.
    // JFXPanel handles Platform.startup internally and safely.
    var jfxPanel by remember { mutableStateOf<JFXPanel?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }


    Box(modifier = modifier) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                JPanel().apply {
                    layout = BorderLayout()
                    val panel = JFXPanel()
                    jfxPanel = panel
                    add(panel, BorderLayout.CENTER)
                    
                    Platform.runLater {
                        val view = WebView()
                        webView = view
                        panel.scene = Scene(view)
                        
                        val engine = view.engine
                        engine.isJavaScriptEnabled = true
                        
                        // Load local HTML file
                        val resourceUrl = Thread.currentThread().contextClassLoader.getResource("leaflet_map.html")
                        if (resourceUrl != null) {
                            engine.load(resourceUrl.toExternalForm())
                        } else {
                            println("Could not find leaflet_map.html resource")
                        }
                        
                        // Setup JS bridge
                        engine.loadWorker.stateProperty().addListener { _, _, newState ->
                            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                                val window = engine.executeScript("window") as JSObject
                                window.setMember("app", WebBridge(onMapClick, onMapReady))
                            }
                        }
                    }
                }
            },
            update = {
                // Update map state
                Platform.runLater {
                    webView?.engine?.let { engine ->
                        if (engine.loadWorker.state == javafx.concurrent.Worker.State.SUCCEEDED) {
                            engine.executeScript("setMapCenter($latitude, $longitude, $zoomLevel);")
                            engine.executeScript("setDarkMap($useDarkMap);")
                            engine.executeScript("clearMarkers();")
                            markers.forEach { marker ->
                                engine.executeScript("addMarker(${marker.latitude}, ${marker.longitude});")
                            }
                        }
                    }
                }
            }
        )
    }
}
