package com.kolee.tracklocation.screens.list

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
import com.kolee.tracklocation.screens.list.components.TrackItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    navHostController: NavHostController
) {
//    Text(text = "List", fontSize = 20.sp)
    val  trackDemoList = listOf(
        TrackEntity(idx = 1, timestamp = System.currentTimeMillis(), distance = 3100F, duration = 500000L, pathPoints = ""),
        TrackEntity(idx = 2, timestamp = System.currentTimeMillis(), distance = 2100F, duration = 300000L, pathPoints = ""),
        TrackEntity(idx = 3, timestamp = System.currentTimeMillis(), distance = 3500F, duration = 300000L, pathPoints = ""),
        TrackEntity(idx = 4, timestamp = System.currentTimeMillis(), distance = 1100F, duration = 800000L, pathPoints = ""),
        TrackEntity(idx = 5, timestamp = System.currentTimeMillis(), distance = 6100F, duration = 100000L, pathPoints = ""),
        TrackEntity(idx = 6, timestamp = System.currentTimeMillis(), distance = 3200F, duration = 900000L, pathPoints = ""),
        TrackEntity(idx = 7, timestamp = System.currentTimeMillis(), distance = 4200F, duration = 600000L, pathPoints = ""),
        TrackEntity(idx = 8, timestamp = System.currentTimeMillis(), distance = 7100F, duration = 520000L, pathPoints = ""),
        TrackEntity(idx = 9, timestamp = System.currentTimeMillis(), distance = 8900F, duration = 700000L, pathPoints = ""),
    )

    Scaffold(
        topBar = {
            ListAppBar()
        },
        content = { padding ->
            ListContent(
                navHostController = navHostController,
                trackList = trackDemoList,
                modifier = Modifier.padding(padding)
            )
        },
        floatingActionButton = {

        }
    )
}

@Composable
fun ListAppBar() {
    TopAppBar(
        backgroundColor = Color.Blue
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

@Composable
fun ListContent(
    navHostController: NavHostController,
    trackList: List<TrackEntity>,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.padding(bottom = 10.dp)
    ) {
        items(
            items = trackList,
            key = { trackItem -> trackItem.idx}
        ) { item ->
            TrackItemRow(item = item)
        }
    }
}