package com.kolee.tracklocation.viewmodel

import com.kolee.tracklocation.data.roomdb.TrackEntity

sealed class Response {
    object Loading: Response()
    data class Success(val data: List<TrackEntity>): Response()
}