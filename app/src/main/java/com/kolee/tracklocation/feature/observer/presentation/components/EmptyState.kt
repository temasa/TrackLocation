package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.kolee.tracklocation.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ObserverEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFF1F4F0), shape = CircleShape)
                .border(1.5.dp, Color(0xFFE7EAE6), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_session_signal),
                contentDescription = null,
                tint = Color(0xFF737373),
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Waiting for events",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A0A0A),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Captured accessibility events will appear here as soon as they're received.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF737373),
            textAlign = TextAlign.Center,
            lineHeight = 19.5.sp,
            modifier = Modifier.widthIn(max = 260.dp),
        )
    }
}
