package net.maiatoday.tagspotter.feature.gallery

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.feature.gallery.res.rememberLocationPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    currentCenter: FilterCenter?,
    currentRadiusMeters: Double,
    homeCityName: String,
    onApplyFilter: (FilterCenter?, Double) -> Unit,
    modifier: Modifier = Modifier,
    selectedSource: String? = null,
    onApplySourceFilter: ((String) -> Unit)? = null,
    showStarredOnly: Boolean? = null,
    onApplyStarredFilter: ((Boolean) -> Unit)? = null
) {
    val permissionState = rememberLocationPermissionState { granted ->
        if (granted) {
            // Permission granted, do nothing or apply GPS filter
        }
    }

    // Local State
    var selectedType by remember {
        mutableStateOf(
            when (currentCenter) {
                is FilterCenter.GPS -> "GPS"
                is FilterCenter.HomeCity -> "Home"
                is FilterCenter.FocusCity -> "Focus"
                else -> "None"
            }
        )
    }

    var selectedFocusCityName by remember {
        mutableStateOf(
            if (currentCenter is FilterCenter.FocusCity) currentCenter.cityName else "London"
        )
    }

    var sliderValue by remember {
        mutableFloatStateOf(LocationUtils.getSliderValueForRadius(currentRadiusMeters))
    }

    val activeRadius = LocationUtils.getLogarithmicRadiusMeters(sliderValue)

    var localSource by remember(selectedSource) {
        mutableStateOf(selectedSource ?: "All")
    }

    var localStarredOnly by remember(showStarredOnly) {
        mutableStateOf(showStarredOnly ?: false)
    }

    val updateFilter = { type: String, focusCity: String, radius: Double ->
        if (type == "GPS") {
            if (!permissionState.hasPermission) {
                permissionState.requestPermission()
            } else {
                onApplyFilter(FilterCenter.GPS(0.0, 0.0), radius)
            }
        } else {
            val finalCenter = when (type) {
                "Home" -> {
                    val gp = LocationUtils.CITIES[homeCityName] ?: LocationUtils.CITIES["Milan"]!!
                    FilterCenter.HomeCity(homeCityName, gp.first, gp.second)
                }

                "Focus" -> {
                    val gp = LocationUtils.CITIES[focusCity] ?: LocationUtils.CITIES["Milan"]!!
                    FilterCenter.FocusCity(focusCity, gp.first, gp.second)
                }

                else -> null
            }
            onApplyFilter(finalCenter, radius)
        }
    }

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
            val isAdvancedFilter = selectedSource != null && showStarredOnly != null
            Text(
                text = if (isAdvancedFilter) "FILTER SEARCH" else "FILTER SEARCH RADIUS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = TextUnit.Unspecified
            )

            if (isAdvancedFilter) {
                // Source selection horizontal chip row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SOURCE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sourcesList = listOf("All", "My Spots", "Imported")
                        sourcesList.forEach { src ->
                            val isSelected = localSource == src
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        localSource = src
                                        onApplySourceFilter?.invoke(src)
                                    }
                            ) {
                                Text(
                                    text = src,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else Color.Gray
                                )
                            }
                        }
                    }
                }

                // Starred Only Switch section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "STARRED SPOTS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Show starred spots only",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = localStarredOnly,
                        onCheckedChange = { checked ->
                            localStarredOnly = checked
                            onApplyStarredFilter?.invoke(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            // Center Option Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SEARCH CENTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // 0. None (No Location Filter) Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedType == "None") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedType = "None"
                            updateFilter("None", selectedFocusCityName, activeRadius)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == "None",
                            onClick = {
                                selectedType = "None"
                                updateFilter("None", selectedFocusCityName, activeRadius)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "None (No Location Filter)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. Current GPS Location Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedType == "GPS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!permissionState.hasPermission) {
                                permissionState.requestPermission()
                            } else {
                                selectedType = "GPS"
                                updateFilter("GPS", selectedFocusCityName, activeRadius)
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
                                if (!permissionState.hasPermission) {
                                    permissionState.requestPermission()
                                } else {
                                    selectedType = "GPS"
                                    updateFilter("GPS", selectedFocusCityName, activeRadius)
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
                            if (!permissionState.hasPermission) {
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
                    color = if (selectedType == "Home") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedType = "Home"
                            updateFilter("Home", selectedFocusCityName, activeRadius)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == "Home",
                            onClick = {
                                selectedType = "Home"
                                updateFilter("Home", selectedFocusCityName, activeRadius)
                            },
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
                    color = if (selectedType == "Focus") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                selectedType = "Focus"
                                updateFilter("Focus", selectedFocusCityName, activeRadius)
                            }
                        ) {
                            RadioButton(
                                selected = selectedType == "Focus",
                                onClick = {
                                    selectedType = "Focus"
                                    updateFilter("Focus", selectedFocusCityName, activeRadius)
                                },
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
                                    LocationUtils.CITIES.keys.forEach { cityName ->
                                        DropdownMenuItem(
                                            text = { Text(cityName) },
                                            onClick = {
                                                selectedFocusCityName = cityName
                                                expandedDropdown = false
                                                updateFilter("Focus", cityName, activeRadius)
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
                    onValueChange = {
                        sliderValue = it
                        val r = LocationUtils.getLogarithmicRadiusMeters(it)
                        updateFilter(selectedType, selectedFocusCityName, r)
                    },
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
        }
    }
}
