package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistScope
import com.kolee.tracklocation.ui.theme.ObserverAmber
import com.kolee.tracklocation.ui.theme.TripGreen

@Composable
fun FeedHeaderBar(
    count: Int,
    scope: AllowlistScope,
    captureRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    val scopeLabel = when (scope) {
        is AllowlistScope.AllPackages -> "ALL PACKAGES"
        is AllowlistScope.FilteredCount -> "${scope.active} OF ${scope.total} PACKAGES"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F4F0))
            .border(
                width = 1.dp,
                color = Color(0xFFE7EAE6),
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$count EVENTS · $scopeLabel",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF737373),
            letterSpacing = 1.sp,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (captureRunning) TripGreen else ObserverAmber,
                        shape = CircleShape,
                    )
            )
            Text(
                text = if (captureRunning) "live" else "paused",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = if (captureRunning) TripGreen else ObserverAmber,
            )
        }
    }
}
