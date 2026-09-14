package com.supermemo.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAllToggle: () -> Unit,
    onMoveToCategory: () -> Unit,
    onBatchArchive: () -> Unit,
    onBatchDelete: () -> Unit,
    onExitSelectionMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(Icons.Filled.Close, contentDescription = "退出多选", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAllToggle) {
                    Icon(
                        Icons.Filled.SelectAll,
                        contentDescription = "全选",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedCount == totalCount) "全不选" else "全选",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = onMoveToCategory) {
                    Icon(Icons.Filled.DriveFileMove, contentDescription = "批量移动分组", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                IconButton(onClick = onBatchArchive) {
                    Icon(Icons.Filled.Archive, contentDescription = "批量归档", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                IconButton(onClick = onBatchDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "批量删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
