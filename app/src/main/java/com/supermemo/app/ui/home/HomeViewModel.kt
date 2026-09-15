package com.supermemo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.data.repository.NoteRepository
import com.supermemo.app.ui.home.components.DrawerDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class QuickFilterType(val label: String) {
    ALL("全部"),
    PINNED("📌 置顶"),
    CHECKLIST("☑ 待办清单"),
    IMAGE("🖼 含图片")
}

data class HomeUiState(
    val notes: List<NoteWithDetails> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currentDestination: DrawerDestination = DrawerDestination.ALL_NOTES,
    val selectedCategoryId: Long? = null,
    val currentFilter: QuickFilterType = QuickFilterType.ALL,
    val isGridLayout: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val showCategoryManage: Boolean = false,
    val showMoveToCategoryDialog: Boolean = false
)

class HomeViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _currentDestination = MutableStateFlow(DrawerDestination.ALL_NOTES)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _currentFilter = MutableStateFlow(QuickFilterType.ALL)
    private val _isGridLayout = MutableStateFlow(true)
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _showCategoryManage = MutableStateFlow(false)
    private val _showMoveToCategoryDialog = MutableStateFlow(false)

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteWithDetails>> = repository.getActiveNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<NoteWithDetails>> = repository.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<NoteWithDetails>> = repository.getTrashNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        activeNotes,
        archivedNotes,
        trashNotes,
        categories,
        _currentDestination,
        _selectedCategoryId,
        _currentFilter,
        _isGridLayout,
        _isSelectionMode,
        _selectedNoteIds,
        _showCategoryManage,
        _showMoveToCategoryDialog
    ) { params ->
        val active = params[0] as List<NoteWithDetails>
        val archived = params[1] as List<NoteWithDetails>
        val trash = params[2] as List<NoteWithDetails>
        val cats = params[3] as List<CategoryEntity>
        val dest = params[4] as DrawerDestination
        val catId = params[5] as Long?
        val filter = params[6] as QuickFilterType
        val grid = params[7] as Boolean
        val selMode = params[8] as Boolean
        val selIds = params[9] as Set<Long>
        val showManage = params[10] as Boolean
        val showMove = params[11] as Boolean

        val rawList = when (dest) {
            DrawerDestination.ALL_NOTES -> active
            DrawerDestination.CATEGORY -> active.filter { it.note.categoryId == catId }
            DrawerDestination.ARCHIVE -> archived
            DrawerDestination.TRASH -> trash
            else -> active
        }

        // 应用快速过滤
        val filteredList = when (filter) {
            QuickFilterType.ALL -> rawList
            QuickFilterType.PINNED -> rawList.filter { it.note.isPinned }
            QuickFilterType.CHECKLIST -> rawList.filter {
                it.note.content.contains("- [ ]") || it.note.content.contains("- [x]")
            }
            QuickFilterType.IMAGE -> rawList.filter { it.attachments.isNotEmpty() }
        }

        HomeUiState(
            notes = filteredList,
            categories = cats,
            currentDestination = dest,
            selectedCategoryId = catId,
            currentFilter = filter,
            isGridLayout = grid,
            isSelectionMode = selMode,
            selectedNoteIds = selIds,
            showCategoryManage = showManage,
            showMoveToCategoryDialog = showMove
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setDestination(dest: DrawerDestination, categoryId: Long? = null) {
        _currentDestination.value = dest
        _selectedCategoryId.value = categoryId
        exitSelectionMode()
    }

    fun setFilter(filter: QuickFilterType) {
        _currentFilter.value = filter
    }

    fun toggleLayout() {
        _isGridLayout.value = !_isGridLayout.value
    }

    fun toggleChecklistItem(noteId: Long, lineIndex: Int) {
        viewModelScope.launch {
            repository.toggleChecklistItem(noteId, lineIndex)
        }
    }

    fun setChecklistItemStatus(noteId: Long, lineIndex: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setChecklistItemStatus(noteId, lineIndex, isCompleted)
        }
    }

    fun deleteChecklistItem(noteId: Long, lineIndex: Int) {
        viewModelScope.launch {
            repository.deleteChecklistItem(noteId, lineIndex)
        }
    }

    fun setNoteAsTodo(noteId: Long) {
        viewModelScope.launch {
            repository.setNoteAsTodo(noteId)
        }
    }

    fun setNoteAllCompleted(noteId: Long) {
        viewModelScope.launch {
            repository.setNoteAllCompleted(noteId)
        }
    }

    fun addChecklistItem(noteId: Long, text: String) {
        viewModelScope.launch {
            repository.addChecklistItem(noteId, text)
        }
    }

    fun editChecklistItemText(noteId: Long, lineIndex: Int, newText: String) {
        viewModelScope.launch {
            repository.updateChecklistItemContent(noteId, lineIndex, newText)
        }
    }

    fun sinkCompletedChecklist(noteId: Long) {
        viewModelScope.launch {
            repository.sinkCompletedChecklist(noteId)
        }
    }

    fun changeNoteColor(noteId: Long, colorHex: String?) {
        viewModelScope.launch {
            repository.updateNoteColor(noteId, colorHex)
        }
    }

    fun changeNoteCategory(noteId: Long, categoryId: Long?) {
        viewModelScope.launch {
            repository.updateNoteCategory(noteId, categoryId)
        }
    }


    fun enterSelectionMode(initialNoteId: Long? = null) {
        _isSelectionMode.value = true
        _selectedNoteIds.value = if (initialNoteId != null) setOf(initialNoteId) else emptySet()
    }

    fun toggleNoteSelection(noteId: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun toggleSelectAll(allIds: List<Long>) {
        if (_selectedNoteIds.value.size == allIds.size) {
            _selectedNoteIds.value = emptySet()
            _isSelectionMode.value = false
        } else {
            _selectedNoteIds.value = allIds.toSet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedNoteIds.value = emptySet()
    }

    fun setShowCategoryManage(show: Boolean) {
        _showCategoryManage.value = show
    }

    fun setShowMoveToCategoryDialog(show: Boolean) {
        _showMoveToCategoryDialog.value = show
    }

    fun createCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createCategory(name, colorHex, "folder")
        }
    }

    fun renameCategory(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameCategory(id, newName)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category, moveNotesToDefault = true)
        }
    }

    fun batchMoveToCategory(categoryId: Long?) {
        viewModelScope.launch {
            repository.batchMoveToCategory(_selectedNoteIds.value.toList(), categoryId)
            exitSelectionMode()
            _showMoveToCategoryDialog.value = false
        }
    }

    fun batchDelete() {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value.toList()
            if (_currentDestination.value == DrawerDestination.TRASH) {
                repository.batchDeletePermanently(ids)
            } else {
                repository.batchTrash(ids)
            }
            exitSelectionMode()
        }
    }

    fun batchArchive() {
        viewModelScope.launch {
            val ids = _selectedNoteIds.value.toList()
            val isCurrentlyArchived = _currentDestination.value == DrawerDestination.ARCHIVE
            repository.batchArchive(ids, !isCurrentlyArchived)
            exitSelectionMode()
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note.id, !note.isPinned)
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleArchive(note.id, !note.isArchived)
        }
    }

    fun moveToTrash(noteId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
        }
    }
}
