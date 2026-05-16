package com.kolee.tracklocation.screens.track.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kolee.tracklocation.R
import com.kolee.tracklocation.utils.TimeUtilFormatter

private const val TAG = "RunningCardTime"

@Composable
fun RunningCardTime(
    modifier: Modifier = Modifier,
    durationTimer: Long,
    isTracking: Boolean,
    onPlayStopClicked: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isTracking) "Trip Time" else "Ready for trip",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = TimeUtilFormatter.getTime(durationTimer),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        IconButton(
            onClick = {
                Log.d(TAG, "Play button clicked")
                onPlayStopClicked()
            },
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    id = if (isTracking) R.drawable.ic_finish else R.drawable.ic_play
                ),
                contentDescription = if (isTracking) "Stop trip" else "Start trip",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
