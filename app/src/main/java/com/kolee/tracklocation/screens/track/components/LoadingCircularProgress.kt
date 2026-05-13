package com.kolee.tracklocation.screens.track.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BoxScope.LoadingCircularProgress(
    isMapLoaded: Boolean = false
) {
    AnimatedVisibility(
        modifier = Modifier.matchParentSize(),
        visible = !isMapLoaded,
        enter = EnterTransition.None,
        exit = fadeOut()
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .wrapContentSize()
        )
    }
}
