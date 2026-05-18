package com.kolee.tracklocation.feature.observer.presentation.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.ui.theme.ObserverAmber
import com.kolee.tracklocation.ui.theme.ObserverAmberBg
import com.kolee.tracklocation.ui.theme.ObserverAmberDark
import com.kolee.tracklocation.ui.theme.ObserverAmberHair
import com.kolee.tracklocation.ui.theme.ObserverGreenBg
import com.kolee.tracklocation.ui.theme.ObserverGreenLight
import com.kolee.tracklocation.ui.theme.ObserverRed
import com.kolee.tracklocation.ui.theme.ObserverRedBg
import com.kolee.tracklocation.ui.theme.ObserverRedDark
import com.kolee.tracklocation.ui.theme.ObserverRedHair
import com.kolee.tracklocation.ui.theme.TripGreen

@Composable
fun ServiceBanner(enabled: Boolean, modifier: Modifier = Modifier) {
    if (enabled) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .background(Color.White, shape = RoundedCornerShape(14.dp))
                .border(1.5.dp, Color(0xFFE7EAE6), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics { contentDescription = "Service, Enabled" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(ObserverGreenBg, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TripGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Service", fontSize = 11.sp, fontWeight = FontWeight.Normal, color = Color(0xFF737373))
                Text("Enabled", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TripGreen)
            }
        }
    } else {
        val context = LocalContext.current
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .background(ObserverRedBg, shape = RoundedCornerShape(14.dp))
                .border(1.5.dp, ObserverRedHair, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Service, Disabled. Tap to open accessibility settings."
                    liveRegion = LiveRegionMode.Assertive
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(ObserverRed, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Service",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = ObserverRedDark.copy(alpha = 0.85f)
                )
                Text(
                    "Disabled",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObserverRedDark
                )
                Text(
                    "Enable the accessibility service to start receiving events.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = ObserverRedDark.copy(alpha = 0.80f),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObserverRedDark)
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "Open settings",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CaptureChip(running: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (running) ObserverGreenLight else ObserverAmberHair
    val tileColor = if (running) ObserverGreenLight else ObserverAmber
    val valueColor = if (running) TripGreen else ObserverAmberDark
    val valueText = if (running) "Running" else "Paused"
    val cd = if (running) "Capture, Running. Double-tap to pause."
             else "Capture, Paused. Double-tap to resume."

    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = cd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(tileColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (running) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Capture", fontSize = 11.sp, fontWeight = FontWeight.Normal, color = Color(0xFF737373))
            Text(valueText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor, letterSpacing = (-0.1).sp)
        }
    }
}

@Composable
fun AutoScrollReadout(running: Boolean, modifier: Modifier = Modifier) {
    val outlineColor = if (running) ObserverGreenLight else ObserverAmber
    val glyphColor = if (running) ObserverGreenLight else ObserverAmber
    val valueColor = if (running) TripGreen else ObserverAmberDark
    val valueText = if (running) "Running" else "Paused"
    val cd = if (running) "Auto-scroll, Running."
             else "Auto-scroll, Paused. Tap the list to resume."

    Row(
        modifier = modifier
            .height(56.dp)
            .padding(start = 4.dp)
            .semantics { contentDescription = cd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.5.dp, outlineColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (running) Icons.Default.ArrowDownward else Icons.Default.Pause,
                contentDescription = null,
                tint = glyphColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "AUTO-SCROLL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF737373),
                letterSpacing = 1.sp
            )
            Text(valueText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor, letterSpacing = (-0.1).sp)
        }
    }
}

@Composable
fun CapturePausedBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .background(ObserverAmberBg, shape = RoundedCornerShape(10.dp))
            .border(1.dp, ObserverAmberHair, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "Capture paused. New events are not being received or stored."
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        Text(
            "Capture paused — new events are not being received or stored. Previously recorded events remain visible.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = ObserverAmberDark,
            lineHeight = 16.sp
        )
    }
}
