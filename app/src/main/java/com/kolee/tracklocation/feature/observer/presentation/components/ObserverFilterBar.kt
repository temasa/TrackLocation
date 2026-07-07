package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.ui.theme.ObserverAmber

@Composable
fun ObserverFilterBar(
    query: String,
    packageOptions: List<String>,
    selectedPackages: Set<String>,
    matchCount: Int,
    isActive: Boolean,
    onQueryChange: (String) -> Unit,
    onTogglePackage: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F4F0)),
    ) {
        // Search input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF0A0A0A),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF737373),
                            modifier = Modifier.size(18.dp),
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (query.isBlank()) {
                                Text(
                                    text = "Search events",
                                    fontSize = 14.sp,
                                    color = Color(0xFF737373),
                                )
                            }
                            innerTextField()
                        }
                    }
                },
            )
        }

        // Package chips
        if (packageOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(packageOptions) { pkg ->
                    val selected = pkg in selectedPackages
                    val label = pkg.substringAfterLast('.')
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selected) ObserverAmber.copy(alpha = 0.18f) else Color(0xFFF1F4F0),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) ObserverAmber else Color(0xFFE7EAE6),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onTogglePackage(pkg) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0A0A0A),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFF0A0A0A) else Color(0xFF737373),
                            )
                        }
                    }
                }
            }
        }

        // Active-filter status row
        if (isActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$matchCount MATCHES",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF737373),
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "Clear",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ObserverAmber,
                    modifier = Modifier.clickable { onClear() },
                )
            }
        }
    }
}
