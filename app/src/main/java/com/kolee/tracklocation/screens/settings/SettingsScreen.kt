package com.kolee.tracklocation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kolee.tracklocation.R
import com.kolee.tracklocation.navigation.Screen

@Composable
fun SettingsScreen(navController: NavController? = null) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F4F0))
            .padding(horizontal = 18.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-1.2).sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
        }

        item { SettingsSectionHeader("GENERAL") }
        item {
            SettingsSectionGroup {
                SettingsRow(
                    label = "Units",
                    value = "Metric (km, km/h)",
                    iconId = R.drawable.baseline_adjust_24,
                    onClick = {},
                )
                SettingsRowDivider()
                SettingsRow(
                    label = "Map style",
                    value = "System default",
                    iconId = R.drawable.baseline_settings_24,
                    onClick = {},
                )
                SettingsRowDivider()
                SettingsRow(
                    label = "Location permission",
                    value = "Granted while in use",
                    iconId = R.drawable.baseline_location_on_24,
                    isLast = true,
                    onClick = {},
                )
            }
        }

        item { SettingsSectionHeader("TOOLS") }
        item {
            SettingsSectionGroup {
                SettingsRow(
                    label = "Observer",
                    supporting = "Inspect accessibility events captured by the service",
                    iconId = R.drawable.ic_session_signal,
                    isLast = true,
                    onClick = { navController?.navigate(Screen.ObserverFeedScreen.route) },
                )
            }
        }

        item { SettingsSectionHeader("ABOUT") }
        item {
            SettingsSectionGroup {
                SettingsRow(
                    label = "About TrackLocation",
                    value = "Version 1.6.2",
                    iconId = R.drawable.baseline_adjust_24,
                    isLast = true,
                    onClick = {},
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF737373),
        letterSpacing = 1.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsSectionGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFFEEF0EC), RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color(0xFFEEF0EC))
    )
}

@Composable
private fun SettingsRow(
    label: String,
    supporting: String? = null,
    value: String? = null,
    iconId: Int,
    isLast: Boolean = false,
    onClick: () -> Unit,
) {
    val cd = if (supporting != null) "$label, $supporting" else label
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = cd }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading icon tile
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF0A0A0A),
            )
        }

        // Label + supporting
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-0.1).sp,
            )
            supporting?.let {
                Text(
                    text = it,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF737373),
                    lineHeight = 17.sp,
                )
            }
        }

        // Trailing value + chevron
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            value?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF737373),
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFFA3A3A3),
            )
        }
    }
}
