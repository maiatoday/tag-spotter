package net.maiatoday.tagspotter.feature.gallery

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.feature.map.MapViewModel
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.location.LocationHelper
import net.maiatoday.tagspotter.core.model.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    currentCenter: FilterCenter?,
    currentRadiusMeters: Double,
    homeCityName: String,
    onApplyFilter: (FilterCenter?, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Local State
    var selectedType by remember {
        mutableStateOf(
            when (currentCenter) {
                is FilterCenter.GPS -> "GPS"
                is FilterCenter.HomeCity -> "Home"
                is FilterCenter.FocusCity -> "Focus"
                else -> "GPS" // default if null
            }
        )
    }

    var selectedFocusCityName by remember {
        mutableStateOf(
            if (currentCenter is FilterCenter.FocusCity) currentCenter.cityName else "London"
        )
    }

    var sliderValue by remember {
        mutableStateOf(LocationUtils.getSliderValueForRadius(currentRadiusMeters))
    }

    val activeRadius = LocationUtils.getLogarithmicRadiusMeters(sliderValue)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "FILTER SEARCH RADIUS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = TextUnit.Unspecified
            )

            // Center Option Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SEARCH CENTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // 1. Current GPS Location Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedType == "GPS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                selectedType = "GPS"
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == "GPS",
                            onClick = {
                                if (!hasLocationPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    selectedType = "GPS"
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Current Location (GPS)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!hasLocationPermission) {
                                Text(
                                    text = "Tap to grant permission",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // 2. Home City Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedType == "Home") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = "Home" }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == "Home",
                            onClick = { selectedType = "Home" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Home City ($homeCityName)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Focus City Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedType == "Focus") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = "Focus" }
                        ) {
                            RadioButton(
                                selected = selectedType == "Focus",
                                onClick = { selectedType = "Focus" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Focus City",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedType == "Focus") {
                            Spacer(modifier = Modifier.height(12.dp))
                            var expandedDropdown by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedFocusCityName)
                                }
                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MapViewModel.CITIES.keys.forEach { cityName ->
                                        DropdownMenuItem(
                                            text = { Text(cityName) },
                                            onClick = {
                                                selectedFocusCityName = cityName
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Radius Slider
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEARCH RADIUS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = LocationUtils.getRadiusLabel(activeRadius),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                // Slider Legend Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("500m", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("5km", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("50km", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // Actions Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onApplyFilter(null, activeRadius)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Clear Filter")
                }

                Button(
                    onClick = {
                        if (selectedType == "GPS") {
                            scope.launch {
                                val currentLoc = LocationHelper.getCurrentLocation(context)
                                val finalCenter = if (currentLoc != null) {
                                    FilterCenter.GPS(currentLoc.latitude, currentLoc.longitude)
                                } else {
                                    val gp = MapViewModel.CITIES[homeCityName] ?: MapViewModel.CITIES["Milan"]!!
                                    FilterCenter.GPS(gp.latitude, gp.longitude)
                                }
                                onApplyFilter(finalCenter, activeRadius)
                                onDismissRequest()
                            }
                        } else {
                            val finalCenter = when (selectedType) {
                                "Home" -> {
                                    val gp = MapViewModel.CITIES[homeCityName] ?: MapViewModel.CITIES["Milan"]!!
                                    FilterCenter.HomeCity(homeCityName, gp.latitude, gp.longitude)
                                }
                                "Focus" -> {
                                    val gp = MapViewModel.CITIES[selectedFocusCityName] ?: MapViewModel.CITIES["Milan"]!!
                                    FilterCenter.FocusCity(selectedFocusCityName, gp.latitude, gp.longitude)
                                }
                                else -> null
                            }
                            onApplyFilter(finalCenter, activeRadius)
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Apply Filter")
                }
            }
        }
    }
}
