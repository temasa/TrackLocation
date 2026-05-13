package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.ui.theme.LightYellow
import com.kolee.tracklocation.utils.TimeUtilFormatter
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackItemRow(
    item: TrackEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val distanceText = "${String.format("%.2f", item.distance / 1000f)} km"
    val timeText = TimeUtilFormatter.getTime(item.duration)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightYellow),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.padding(vertical = 10.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 5.dp, top = 5.dp)
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                    text = SimpleDateFormat("HH:mm, dd MM yyyy", Locale.ENGLISH).format(item.timestamp),
                    color = Color.Blue.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .weight(1f)
                        .padding(start = 20.dp, bottom = 10.dp),
                    textAlign = TextAlign.Left,
                    text = distanceText,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Text(
                    modifier = Modifier.weight(1f)
                        .padding(start = 20.dp, bottom = 10.dp),
                    textAlign = TextAlign.Right,
                    text = timeText,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }
        }
    }
}
