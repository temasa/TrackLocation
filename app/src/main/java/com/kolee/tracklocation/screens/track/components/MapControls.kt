package com.kolee.tracklocation.screens.track.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kolee.tracklocation.ui.theme.MapFabBg

private val IconColor = Color(0xFF0A0A0A)

@Composable
fun MapControls(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 240.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        MapControlButton(contentDescription = "Recenter map") { drawRecenterIcon() }
        MapControlButton(contentDescription = "Map layers") { drawLayersIcon() }
    }
}

@Composable
private fun MapControlButton(
    contentDescription: String,
    drawIcon: DrawScope.() -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x1F000000)
            )
            .clip(CircleShape)
            .background(MapFabBg)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) fabBlurModifier()
                else Modifier
            )
            .semantics { this.contentDescription = contentDescription }
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawIcon()
        }
    }
}

private fun DrawScope.drawRecenterIcon() {
    val strokePx = 2.dp.toPx()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val circleR = size.width * 0.22f
    val tickInner = circleR + size.width * 0.09f
    val tickOuter = size.width * 0.50f

    drawCircle(IconColor, radius = circleR, style = Stroke(strokePx, cap = StrokeCap.Round))
    drawLine(IconColor, Offset(cx, cy - tickOuter), Offset(cx, cy - tickInner), strokePx)
    drawLine(IconColor, Offset(cx, cy + tickInner), Offset(cx, cy + tickOuter), strokePx)
    drawLine(IconColor, Offset(cx - tickOuter, cy), Offset(cx - tickInner, cy), strokePx)
    drawLine(IconColor, Offset(cx + tickInner, cy), Offset(cx + tickOuter, cy), strokePx)
}

private fun DrawScope.drawLayersIcon() {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val cx = size.width / 2f
    val hw = size.width * 0.38f   // diamond half-width
    val hh = size.height * 0.10f  // diamond half-height
    val yPositions = listOf(size.height * 0.22f, size.height * 0.50f, size.height * 0.78f)

    val path = Path()
    for (cy in yPositions) {
        path.moveTo(cx, cy - hh)
        path.lineTo(cx + hw, cy)
        path.lineTo(cx, cy + hh)
        path.lineTo(cx - hw, cy)
        path.close()
    }
    drawPath(path, IconColor, style = stroke)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun fabBlurModifier(): Modifier = Modifier.graphicsLayer {
    renderEffect = android.graphics.RenderEffect
        .createBlurEffect(10f, 10f, android.graphics.Shader.TileMode.CLAMP)
        .asComposeRenderEffect()
}
