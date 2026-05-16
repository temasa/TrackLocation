package com.kolee.tracklocation.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.screens.list.components.ListContent
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.viewmodel.ShareViewModel

@Composable
fun ListScreen(
    onSelect: (trackIdx: Int) -> Unit
) {
    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory
    )
    val responseState = viewModel.responseState

    Scaffold(
        containerColor = TripBackground,
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TripBackground)
                    .padding(padding)
            ) {
                ListContent(
                    responseState = responseState,
                    onSelect = onSelect,
                    viewModel = viewModel
                )
            }
        }
    )
}
