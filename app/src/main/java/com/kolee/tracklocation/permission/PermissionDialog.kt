package com.kolee.tracklocation.permission

import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random


private const val TAG = "PermissionDialog"

@Composable
fun PermissionDialog(
    open: Boolean,
    msgText: String,
    callback: (Boolean) -> Unit
) {
    val rnd = Random.nextInt()

    Log.d(TAG, "Enter PermissionDialog, parameter open: ${open}, callId: ${rnd}")

    var openDialog by remember {
        val amut = mutableStateOf(open)
        Log.d(TAG, "by remember, open: ${open}, amut: ${amut.value}")
        amut
    }

    Log.d(TAG, "Enter PermissionDialog, parameter open: ${open}, openDialog: ${openDialog}, callId: ${rnd}")
    if (openDialog) {
        Log.d(TAG, "Showing PermissionDialog, openDialog: ${openDialog}, callId: ${rnd}")
        AlertDialog(
            onDismissRequest = {

            },
            title = { Text(text = "Request Permission") },
            text = { Text(text = msgText)},
            confirmButton = {
                Button(
                    onClick = {
                        openDialog = false
                        callback.invoke(true)
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        openDialog = false
                        callback.invoke(false)
                    }
                ) {
                    Text(text = "Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.Blue,
            textContentColor = Color.White
        )
    }

}