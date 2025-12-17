package com.kolee.tracklocation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int,
    tint: Int? = null
): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)!!
    tint?.let {
        vectorDrawable.setTint(it)
    }

    vectorDrawable.setBounds( 0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

    val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}