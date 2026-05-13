package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.viewmodel.Response
import com.kolee.tracklocation.viewmodel.ShareViewModel
import com.kolee.tracklocation.R


@Composable
fun ListContent(
    modifier: Modifier = Modifier,
    responseState: Response,
    onSelect: (trackIdx: Int) -> Unit,
    viewModel: ShareViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
//        modifier = Modifier.padding(10.dp)
    ) {
        when (responseState) {
            is Response.Loading -> LoadingIndicator()

            is Response.Success -> {
                val trackList = responseState.data

                TrackSuccessState(
                    trackList = trackList,
                    onSelect = onSelect,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun TrackSuccessState(
    trackList: List<TrackEntity>,
    onSelect: (trackIdx: Int) -> Unit,
    viewModel: ShareViewModel
) {
    if (trackList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_item_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )
        }
    }
    else {
        LazyColumn(
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(
                items = trackList,
                key = { trackItem -> trackItem.idx }
            ) { item ->
                var showDialogForDeletion by remember { mutableStateOf(false) }

                TrackItemRow(
                    item = item,
                    onClick = { onSelect.invoke(item.idx) },
                    onLongClick = { showDialogForDeletion = true }
                )

                if (showDialogForDeletion) {
                    CustomAlertDialog(
                        title = "Are you sure to delete?"
                    ) { isDelete ->
                        showDialogForDeletion = false
                        if (isDelete) viewModel.deleteTrack(item)
                    }
                }
            }
        }
    }
}