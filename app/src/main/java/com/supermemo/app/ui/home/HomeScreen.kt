package com.supermemo.app.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.supermemo.app.ui.theme.NoteCardColors
import com.supermemo.app.ui.theme.parseHexColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.domain.engine.ChecklistItem
import com.supermemo.app.domain.engine.MarkdownParser
import com.supermemo.app.ui.category.CategoryManageDialog
import com.supermemo.app.ui.home.components.CategoryDrawer
import com.supermemo.app.ui.home.components.DrawerDestination
import com.supermemo.app.ui.home.components.MultiSelectActionBar
import com.supermemo.app.ui.home.components.NoteCard
import com.supermemo.app.util.BiometricHelper
import com.supermemo.app.util.PreferenceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (noteId: Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val blinkSettings by PreferenceManager.settingsFlow.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val haptic = LocalHapticFeedback.current

    var showClearTrashDialog by remember { mutableStateOf(false) }
    var activeChecklistItem by remember { mutableStateOf<Pair<Long, ChecklistItem>?>(null) }
    var editingChecklistItem by remember { mutableStateOf<Triple<Long, ChecklistItem, String>?>(null) }
    var activeNoteForQuickAction by remember { mutableStateOf<NoteWithDetails?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CategoryDrawer(
                categories = uiState.categories,
                selectedDestination = uiState.currentDestination,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectDestination = { dest, catId ->
                    scope.launch { drawerState.close() }
                    when (dest) {
                        DrawerDestination.BACKUP_RESTORE -> onNavigateToBackupRestore()
                        DrawerDestination.SETTINGS -> onNavigateToSettings()
                        else -> viewModel.setDestination(dest, catId)
                    }
                },
                onManageCategories = {
                    scope.launch { drawerState.close() }
                    viewModel.setShowCategoryManage(true)
                },
                onAddCategory = {
                    scope.launch { drawerState.close() }
                    viewModel.setShowCategoryManage(true)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val titleText = when (uiState.currentDestination) {
                            DrawerDestination.ALL_NOTES -> "全部备忘录"
                            DrawerDestination.CATEGORY -> uiState.categories.find { it.id == uiState.selectedCategoryId }?.name ?: "分类备忘"
                            DrawerDestination.ARCHIVE -> "归档箱"
                            DrawerDestination.TRASH -> "回收站"
                            else -> "超级备忘录"
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "打开菜单")
                        }
                    },
                    actions = {
                        // 搜索按钮
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "搜索")
                        }
                        // 网格/列表切换按钮
                        IconButton(onClick = { viewModel.toggleLayout() }) {
                            Icon(
                                imageVector = if (uiState.isGridLayout) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                                contentDescription = "切换视图布局"
                            )
                        }
                        // 回收站模式清空按钮
                        if (uiState.currentDestination == DrawerDestination.TRASH && uiState.notes.isNotEmpty()) {
                            IconButton(onClick = { showClearTrashDialog = true }) {
                                Icon(
                                    Icons.Filled.DeleteSweep,
                                    contentDescription = "清空回收站",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (!uiState.isSelectionMode && uiState.currentDestination != DrawerDestination.TRASH) {
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToEditor(0L) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("记一笔") },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 顶部快速过滤 Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(QuickFilterType.values()) { filter ->
                            val isSelected = uiState.currentFilter == filter
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(filter.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // 备忘录列表
                    if (uiState.notes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (uiState.currentDestination == DrawerDestination.TRASH) Icons.Outlined.DeleteOutline else Icons.AutoMirrored.Outlined.FactCheck,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (uiState.currentDestination == DrawerDestination.TRASH) "回收站空空如也" else "还没有备忘录",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (uiState.currentDestination == DrawerDestination.TRASH) "已删除的内容会自动保留在此处" else "点击右下角「记一笔」开启灵感记录与待办清单吧",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val handleNoteClick: (NoteWithDetails) -> Unit = { item ->
                            if (uiState.isSelectionMode) {
                                viewModel.toggleNoteSelection(item.note.id)
                            } else {
                                if (item.note.isLocked) {
                                    if (activity != null && BiometricHelper.canAuthenticate(context)) {
                                        BiometricHelper.showBiometricPrompt(
                                            activity = activity,
                                            title = "解锁私密备忘录",
                                            onSuccess = { onNavigateToEditor(item.note.id) },
                                            onError = { Toast.makeText(context, "指纹识别未通过", Toast.LENGTH_SHORT).show() }
                                        )
                                    } else {
                                        onNavigateToEditor(item.note.id)
                                    }
                                } else {
                                    onNavigateToEditor(item.note.id)
                                }
                            }
                        }

                        if (uiState.isGridLayout) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalItemSpacing = 10.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.notes, key = { it.note.id }) { item ->
                                    val isSelected = uiState.selectedNoteIds.contains(item.note.id)
                                    NoteCard(
                                        modifier = Modifier.animateItem(),
                                        noteDetails = item,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isBlinkEnabled = blinkSettings.isEnabled,
                                        blinkColorHex = blinkSettings.colorHex,
                                        onClick = { handleNoteClick(item) },
                                        onLongClick = {
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleNoteSelection(item.note.id)
                                            } else {
                                                activeNoteForQuickAction = item
                                            }
                                        },
                                        onToggleChecklistItem = { lineIdx ->
                                            viewModel.toggleChecklistItem(item.note.id, lineIdx)
                                        },
                                        onChecklistItemLongClick = { noteId, chkItem ->
                                            activeChecklistItem = noteId to chkItem
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.notes, key = { it.note.id }) { item ->
                                    val isSelected = uiState.selectedNoteIds.contains(item.note.id)
                                    NoteCard(
                                        modifier = Modifier.animateItem(),
                                        noteDetails = item,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isBlinkEnabled = blinkSettings.isEnabled,
                                        blinkColorHex = blinkSettings.colorHex,
                                        onClick = { handleNoteClick(item) },
                                        onLongClick = {
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleNoteSelection(item.note.id)
                                            } else {
                                                activeNoteForQuickAction = item
                                            }
                                        },
                                        onToggleChecklistItem = { lineIdx ->
                                            viewModel.toggleChecklistItem(item.note.id, lineIdx)
                                        },
                                        onChecklistItemLongClick = { noteId, chkItem ->
                                            activeChecklistItem = noteId to chkItem
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 批量操作悬浮条
                AnimatedVisibility(
                    visible = uiState.isSelectionMode,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    MultiSelectActionBar(
                        selectedCount = uiState.selectedNoteIds.size,
                        totalCount = uiState.notes.size,
                        onSelectAllToggle = {
                            viewModel.toggleSelectAll(uiState.notes.map { it.note.id })
                        },
                        onMoveToCategory = {
                            viewModel.setShowMoveToCategoryDialog(true)
                        },
                        onBatchArchive = {
                            viewModel.batchArchive()
                        },
                        onBatchDelete = {
                            viewModel.batchDelete()
                        },
                        onExitSelectionMode = {
                            viewModel.exitSelectionMode()
                        }
                    )
                }
            }
        }
    }

    // 分类管理弹窗
    if (uiState.showCategoryManage) {
        CategoryManageDialog(
            categories = uiState.categories,
            onDismiss = { viewModel.setShowCategoryManage(false) },
            onCreateCategory = { name, color -> viewModel.createCategory(name, color) },
            onRenameCategory = { id, name -> viewModel.renameCategory(id, name) },
            onDeleteCategory = { category -> viewModel.deleteCategory(category) }
        )
    }

    // 批量移动分组选择弹窗
    if (uiState.showMoveToCategoryDialog) {
        var targetCatId by remember { mutableStateOf<Long?>(null) }
        AlertDialog(
            onDismissRequest = { viewModel.setShowMoveToCategoryDialog(false) },
            title = { Text("移动到分组") },
            text = {
                LazyColumn {
                    items(uiState.categories) { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = targetCatId == cat.id,
                                onClick = { targetCatId = cat.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.batchMoveToCategory(targetCatId) }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowMoveToCategoryDialog(false) }) {
                    Text("取消")
                }
            }
        )
    }

    // 清空回收站确认弹窗
    if (showClearTrashDialog) {
        AlertDialog(
            onDismissRequest = { showClearTrashDialog = false },
            title = { Text("清空回收站？") },
            text = { Text("回收站中的所有备忘录将被彻底永久删除，无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearTrash()
                        showClearTrashDialog = false
                    }
                ) {
                    Text("彻底清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTrashDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 长按待办事项快捷操作弹窗
    activeChecklistItem?.let { (noteId, item) ->
        AlertDialog(
            onDismissRequest = { activeChecklistItem = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "待办事项快捷操作",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (item.isCompleted) "当前状态：已完成 ✓" else "当前状态：待办进行中 ⏳",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 设为待办选项卡片
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!item.isCompleted) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (!item.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setChecklistItemStatus(noteId, item.lineIndex, false)
                                Toast.makeText(context, "已设置为待办事项", Toast.LENGTH_SHORT).show()
                                activeChecklistItem = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "设置为待办",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "恢复为未完成待办，开启彩色呼吸提醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 标记完成选项卡片
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setChecklistItemStatus(noteId, item.lineIndex, true)
                                Toast.makeText(context, "已标记为已完成", Toast.LENGTH_SHORT).show()
                                activeChecklistItem = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "标记为已完成",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "标记完成打勾，自动划线并停止闪烁",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. 修改此项内容
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                editingChecklistItem = Triple(noteId, item, item.text)
                                activeChecklistItem = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "修改此项内容",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "就地快速修改待办文字并保存",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. 删除此事项选项卡片
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.deleteChecklistItem(noteId, item.lineIndex)
                                Toast.makeText(context, "已删除此待办事项", Toast.LENGTH_SHORT).show()
                                activeChecklistItem = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "删除此事项",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "彻底从清单正文中移除此行",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeChecklistItem = null }) {
                    Text("取消", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    // 就地修改待办事项内容弹窗
    editingChecklistItem?.let { (noteId, item, initialText) ->
        var editText by remember { mutableStateOf(initialText) }
        AlertDialog(
            onDismissRequest = { editingChecklistItem = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("修改待办事项内容", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("待办文字") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editText.isNotBlank()) {
                        viewModel.editChecklistItemText(noteId, item.lineIndex, editText.trim())
                        Toast.makeText(context, "已更新待办内容", Toast.LENGTH_SHORT).show()
                    }
                    editingChecklistItem = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingChecklistItem = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 长按备忘录卡片快捷操作弹窗
    activeNoteForQuickAction?.let { item ->
        val note = item.note
        val checklistItems = MarkdownParser.extractChecklistItems(note.content)
        val hasChecklist = checklistItems.isNotEmpty()

        AlertDialog(
            onDismissRequest = { activeNoteForQuickAction = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = note.title.ifBlank { "备忘录快捷操作" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasChecklist) {
                                "待办清单 (${checklistItems.count { it.isCompleted }}/${checklistItems.size})"
                            } else {
                                item.category?.let { "分类: ${it.name}" } ?: "普通备忘录"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 🌟 事项待办核心操作区（最为显眼、不可错过）
                    Text(
                        text = "事项待办操作",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. 设置为待办 (核心功能)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setNoteAsTodo(note.id)
                                Toast.makeText(context, "已设置为待办事项", Toast.LENGTH_SHORT).show()
                                activeNoteForQuickAction = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "设置为待办",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasChecklist) "重置所有事项为未完成待办，开启彩色呼吸提醒" else "将此备忘录转为待办事项清单，开启彩色呼吸提醒",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. 标记为已完成
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setNoteAllCompleted(note.id)
                                Toast.makeText(context, "已标记为全部完成", Toast.LENGTH_SHORT).show()
                                activeNoteForQuickAction = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "标记为已完成",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "将所有待办事项标记打勾完成，划线并停止闪烁",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. 删除此事项
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.moveToTrash(note.id)
                                Toast.makeText(context, "已移至回收站", Toast.LENGTH_SHORT).show()
                                activeNoteForQuickAction = null
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "删除此事项",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "将此备忘录放入回收站",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // 4. 若有单独的待办清单，展开展示每条细分条目与单独操作
                    if (hasChecklist) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "清单单项管理 (${checklistItems.count { it.isCompleted }}/${checklistItems.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.outline
                            )
                            TextButton(
                                onClick = {
                                    viewModel.sinkCompletedChecklist(note.id)
                                    Toast.makeText(context, "已将已完成事项移至底部", Toast.LENGTH_SHORT).show()
                                    activeNoteForQuickAction = null
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.South, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("已完成沉底", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        checklistItems.forEach { chkItem ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (chkItem.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                        contentDescription = null,
                                        tint = if (chkItem.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                viewModel.toggleChecklistItem(note.id, chkItem.lineIndex)
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = chkItem.text,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = if (chkItem.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // 设为待办 / 标记完成 快速切换文字按钮
                                    TextButton(
                                        onClick = {
                                            viewModel.setChecklistItemStatus(note.id, chkItem.lineIndex, !chkItem.isCompleted)
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (chkItem.isCompleted) "设为待办" else "完成",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    // 编辑文字
                                    IconButton(
                                        onClick = {
                                            editingChecklistItem = Triple(note.id, chkItem, chkItem.text)
                                            activeNoteForQuickAction = null
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "修改内容",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    // 删除该待办项
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteChecklistItem(note.id, chkItem.lineIndex)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "删除此项",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // 便签级更多操作
                    Text(
                        text = "便签其他管理",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.togglePin(note)
                                activeNoteForQuickAction = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (note.isPinned) "取消置顶" else "置顶便签", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.toggleArchive(note)
                                activeNoteForQuickAction = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (note.isArchived) "取消归档" else "归档便签", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            activeNoteForQuickAction = null
                            viewModel.enterSelectionMode(note.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("进入批量多选模式", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "便签色彩",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NoteCardColors) { hex ->
                            val isSelected = hex.equals(note.colorHex, ignoreCase = true) || (note.colorHex == null && hex == "#FFFFFF")
                            val color = parseHexColor(hex, Color.White)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.changeNoteColor(note.id, if (hex == "#FFFFFF") null else hex)
                                        Toast.makeText(context, "已更新便签色彩", Toast.LENGTH_SHORT).show()
                                        activeNoteForQuickAction = null
                                    }
                            )
                        }
                    }

                    if (uiState.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "所属分组",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                ElevatedFilterChip(
                                    selected = note.categoryId == null,
                                    onClick = {
                                        viewModel.changeNoteCategory(note.id, null)
                                        Toast.makeText(context, "已移至默认无分组", Toast.LENGTH_SHORT).show()
                                        activeNoteForQuickAction = null
                                    },
                                    label = { Text("无分组", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            items(uiState.categories) { cat ->
                                ElevatedFilterChip(
                                    selected = note.categoryId == cat.id,
                                    onClick = {
                                        viewModel.changeNoteCategory(note.id, cat.id)
                                        Toast.makeText(context, "已移至分组: ${cat.name}", Toast.LENGTH_SHORT).show()
                                        activeNoteForQuickAction = null
                                    },
                                    label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeNoteForQuickAction = null }) {
                    Text("关闭")
                }
            }
        )
    }
}
