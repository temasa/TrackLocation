package com.kolee.tracklocation.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val TAG = "CheckAndRequestPermission"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckAndRequestPermissions(
    isGranted: () -> Unit
) {
//    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    val permissionList = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    val permissionState = rememberMultiplePermissionsState(
        permissions = permissionList
    )

    var openDialogState by remember {
        mutableStateOf(false)
    }

    Log.d(TAG, "Enter CheckAndRequestPermission, openDialogState: ${openDialogState}")
    if (permissionState.allPermissionsGranted) {
        isGranted.invoke()
    }
    else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Button(
                modifier = Modifier,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.DarkGray
                ),
                enabled = !permissionState.allPermissionsGranted,
                onClick = {
                    permissionState.launchMultiplePermissionRequest()
                    Log.d(TAG, "launchMultiplePermissionRequest, openDialogState: ${openDialogState}")
                    openDialogState = true
                }
            ) {
                Text(
                    text = "Request permission required",
                    color = Color.White
                )
            }

            Log.d(TAG, "Ask permissions, openDialogState: ${openDialogState}")

            permissionState.permissions.forEach {
                Log.d(TAG, "Ask permission: ${it.permission}, "
                    + "hasPermission: ${it.hasPermission}, "
                    + "shouldShowRationale: ${it.shouldShowRationale}, "
                    + "openDialogState: ${openDialogState}")

                when (it.permission) {
                    Manifest.permission.POST_NOTIFICATIONS -> {
                        when {
                            it.hasPermission -> {}
                            it.shouldShowRationale -> {
                                Log.d(TAG, "Show rationale for ${it.permission}, openDialogState: ${openDialogState}")
                                PermissionDialog(
                                    open = openDialogState,
                                    msgText = "Post notification permission is needed",
                                    callback = { callback ->
                                        if (callback) permissionState.launchMultiplePermissionRequest()
                                    }
                                )
                            }
                            !it.hasPermission && !it.shouldShowRationale -> {
                                PermissionDialog(
                                    open = openDialogState,
                                    msgText = "You can go to the app settings to grant it",
                                    callback = { callback ->
                                        if (callback) activity.openAppSettings()
                                    }
                                )
                            }
                        }
                    }

                    Manifest.permission.ACCESS_FINE_LOCATION -> {
                        when {
                            it.hasPermission -> {}
                            it.shouldShowRationale -> {
                                Log.d(TAG, "Show rationale for ${it.permission}, openDialogState: ${openDialogState}")
                                PermissionDialog(
                                    open = openDialogState,
                                    msgText = "Location permission is needed",
                                    callback = { callback ->
                                        if (callback) permissionState.launchMultiplePermissionRequest()
                                    }
                                )
                            }
                            !it.hasPermission && !it.shouldShowRationale -> {
                                PermissionDialog(
                                    open = openDialogState,
                                    msgText = "You can go to the app settings to grant it",
                                    callback = { callback ->
                                        if (callback) activity.openAppSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}