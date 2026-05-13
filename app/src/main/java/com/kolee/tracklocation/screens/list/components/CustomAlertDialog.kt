package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun CustomAlertDialog(
    title: String,
    text: String? = null,
    onClick: (isDelete: Boolean) -> Unit
) {
    var isShowDialog by remember { mutableStateOf(true) }

    if (isShowDialog) {
        AlertDialog(
            modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(24.dp)),
            title = { Text(text = title) },
            text = null,
            dismissButton = {
                TextButton(
                    onClick  = {
                        isShowDialog = false
                        onClick.invoke(false)
                    }
                ) {
                    Text(text = "Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowDialog = false
                        onClick.invoke(true)
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "Confirm")
                }
            },
            onDismissRequest = {}
        )
    }
}