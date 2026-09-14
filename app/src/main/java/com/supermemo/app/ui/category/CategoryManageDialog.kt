package com.supermemo.app.ui.category

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.ui.theme.parseHexColor

val PRESET_CATEGORY_COLORS = listOf(
    "#6750A4", // 紫
    "#006495", // 蓝
    "#386A20", // 绿
    "#9C4146", // 红
    "#7D5260", // 棕红
    "#006A6A", // 青
    "#E65100", // 橙
    "#4A6572"  // 灰蓝
)

@Composable
fun CategoryManageDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onCreateCategory: (name: String, colorHex: String) -> Unit,
    onRenameCategory: (id: Long, newName: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("管理分组分类", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "新建分组", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                items(categories, key = { it.id }) { category ->
                    val color = parseHexColor(category.colorHex, MaterialTheme.colorScheme.primary)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { editingCategory = category },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "重命名", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { deletingCategory = category },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )

    // 新建分组弹窗
    if (showCreateDialog) {
        CreateOrEditCategoryDialog(
            title = "新建分组",
            initialName = "",
            initialColor = PRESET_CATEGORY_COLORS[0],
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color ->
                onCreateCategory(name, color)
                showCreateDialog = false
            }
        )
    }

    // 重命名分组弹窗
    editingCategory?.let { category ->
        CreateOrEditCategoryDialog(
            title = "重命名分组",
            initialName = category.name,
            initialColor = category.colorHex,
            onDismiss = { editingCategory = null },
            onConfirm = { newName, _ ->
                onRenameCategory(category.id, newName)
                editingCategory = null
            }
        )
    }

    // 删除确认弹窗
    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("确认删除分组？") },
            text = { Text("删除分组「${category.name}」后，该组下的备忘录将自动归入默认备忘，内容不会丢失。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(category)
                        deletingCategory = null
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CreateOrEditCategoryDialog(
    title: String,
    initialName: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分组名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("选择专属色彩：", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PRESET_CATEGORY_COLORS) { hex ->
                        val isSelected = hex.equals(selectedColor, ignoreCase = true)
                        val color = parseHexColor(hex, Color.Gray)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
