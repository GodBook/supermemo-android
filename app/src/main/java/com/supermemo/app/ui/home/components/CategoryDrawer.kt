package com.supermemo.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.ui.theme.parseHexColor

enum class DrawerDestination {
    ALL_NOTES,
    CATEGORY,
    ARCHIVE,
    TRASH,
    BACKUP_RESTORE,
    SETTINGS
}

@Composable
fun CategoryDrawer(
    categories: List<CategoryEntity>,
    selectedDestination: DrawerDestination,
    selectedCategoryId: Long?,
    onSelectDestination: (DrawerDestination, Long?) -> Unit,
    onManageCategories: () -> Unit,
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // App 标志与名称
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "超级备忘录",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Android 16 离线私密版",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 全部备忘录
            NavigationDrawerItem(
                label = { Text("全部备忘录") },
                icon = { Icon(Icons.Filled.Notes, contentDescription = null) },
                selected = selectedDestination == DrawerDestination.ALL_NOTES,
                onClick = { onSelectDestination(DrawerDestination.ALL_NOTES, null) },
                shape = RoundedCornerShape(12.dp),
                colors = NavigationDrawerItemDefaults.colors()
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // 分组列表表头与管理
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分组分类",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onManageCategories) {
                    Text("管理", style = MaterialTheme.typography.labelSmall)
                }
            }

            // 分组各项
            categories.forEach { category ->
                val isSelected = selectedDestination == DrawerDestination.CATEGORY && selectedCategoryId == category.id
                val catColor = parseHexColor(category.colorHex, MaterialTheme.colorScheme.primary)

                NavigationDrawerItem(
                    label = { Text(category.name) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(catColor, CircleShape)
                        )
                    },
                    selected = isSelected,
                    onClick = { onSelectDestination(DrawerDestination.CATEGORY, category.id) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 添加分组快捷按钮
            TextButton(
                onClick = onAddCategory,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("新建分组", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // 归档箱
            NavigationDrawerItem(
                label = { Text("归档箱") },
                icon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                selected = selectedDestination == DrawerDestination.ARCHIVE,
                onClick = { onSelectDestination(DrawerDestination.ARCHIVE, null) },
                shape = RoundedCornerShape(12.dp)
            )

            // 回收站
            NavigationDrawerItem(
                label = { Text("回收站") },
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                selected = selectedDestination == DrawerDestination.TRASH,
                onClick = { onSelectDestination(DrawerDestination.TRASH, null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // 备份与恢复
            NavigationDrawerItem(
                label = { Text("备份与恢复") },
                icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
                selected = selectedDestination == DrawerDestination.BACKUP_RESTORE,
                onClick = { onSelectDestination(DrawerDestination.BACKUP_RESTORE, null) },
                shape = RoundedCornerShape(12.dp)
            )

            // 设置与关于
            NavigationDrawerItem(
                label = { Text("设置与关于") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                selected = selectedDestination == DrawerDestination.SETTINGS,
                onClick = { onSelectDestination(DrawerDestination.SETTINGS, null) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
