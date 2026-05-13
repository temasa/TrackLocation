package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.LightGray,
    strokeWidth: Dp = 4.dp
) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = strokeWidth,
        modifier = modifier.fillMaxSize()
            .wrapContentSize(align = Alignment.Center)
    )
}
