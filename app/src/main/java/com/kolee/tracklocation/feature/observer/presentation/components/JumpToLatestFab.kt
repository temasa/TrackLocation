package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JumpToLatestFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0A0A))
                .height(40.dp)
                .clickable { onClick() }
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Jump to latest",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
