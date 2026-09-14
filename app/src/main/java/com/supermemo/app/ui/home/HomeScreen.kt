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
import com.supermemo.app.ui.category.CategoryManageDialog
import com.supermemo.app.ui.home.components.CategoryDrawer
import com.supermemo.app.ui.home.components.DrawerDestination
import com.supermemo.app.ui.home.components.MultiSelectActionBar
import com.supermemo.app.ui.home.components.NoteCard
import com.supermemo.app.util.BiometricHelper
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showClearTrashDialog by remember { mutableStateOf(false) }

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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.currentDestination == DrawerDestination.TRASH) "回收站空空如也" else "还没有备忘录，点击右下角开启灵感记录吧",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
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
                                        noteDetails = item,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        onClick = { handleNoteClick(item) },
                                        onLongClick = {
                                            if (!uiState.isSelectionMode) {
                                                viewModel.enterSelectionMode(item.note.id)
                                            }
                                        },
                                        onToggleChecklistItem = { lineIdx ->
                                            viewModel.toggleChecklistItem(item.note.id, lineIdx)
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
                                        noteDetails = item,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        onClick = { handleNoteClick(item) },
                                        onLongClick = {
                                            if (!uiState.isSelectionMode) {
                                                viewModel.enterSelectionMode(item.note.id)
                                            }
                                        },
                                        onToggleChecklistItem = { lineIdx ->
                                            viewModel.toggleChecklistItem(item.note.id, lineIdx)
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
}
