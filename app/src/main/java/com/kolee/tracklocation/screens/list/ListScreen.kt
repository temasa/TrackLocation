package com.kolee.tracklocation.screens.list

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kolee.tracklocation.data.roomdb.TrackEntity
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.screens.list.components.ListContent
import com.kolee.tracklocation.screens.list.components.TrackItemRow
import com.kolee.tracklocation.ui.theme.RoyalBlue
import com.kolee.tracklocation.viewmodel.Response
import com.kolee.tracklocation.viewmodel.ShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onSelect: (trackIdx: Int) -> Unit
) {

    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory
    )

    val responseState = viewModel.responseState

    Scaffold(
        topBar = {
            ListAppBar()
        },
        content = { padding ->
            ListContent(
                modifier = Modifier.padding(padding),
                responseState = responseState,
                onSelect = onSelect,
                viewModel = viewModel
            )
        },
        floatingActionButton = {

        }
    )
}

@Composable
fun ListAppBar() {
    TopAppBar(
        backgroundColor = RoyalBlue,
        modifier = Modifier
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
    ){
        Text(
            text = "TRACK LIST",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

