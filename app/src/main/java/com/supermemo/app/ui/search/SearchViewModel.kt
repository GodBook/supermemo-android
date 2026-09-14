package com.supermemo.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.data.repository.NoteRepository
import com.supermemo.app.domain.engine.SearchEngine
import com.supermemo.app.domain.model.DateRangeFilter
import com.supermemo.app.domain.model.SearchFilter
import com.supermemo.app.domain.model.SearchResult
import com.supermemo.app.domain.model.SearchSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val filter: SearchFilter = SearchFilter(),
    val results: List<SearchResult> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchHistory: List<String> = listOf("工作", "清单", "项目", "会议", "待办")
)

class SearchViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SearchFilter())
    private val _searchHistory = MutableStateFlow(listOf("会议", "工作", "待办", "购物"))

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteWithDetails>> = repository.getActiveNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        _filter,
        activeNotes,
        categories,
        _searchHistory
    ) { filter, notes, cats, history ->
        val searchResults = SearchEngine.search(notes, filter)
        SearchUiState(
            filter = filter,
            results = searchResults,
            categories = cats,
            searchHistory = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun updateQuery(query: String) {
        _filter.value = _filter.value.copy(query = query)
        if (query.trim().length >= 2 && !_searchHistory.value.contains(query.trim())) {
            _searchHistory.value = (listOf(query.trim()) + _searchHistory.value).take(10)
        }
    }

    fun setCategoryFilter(categoryId: Long?) {
        val next = if (_filter.value.categoryId == categoryId) null else categoryId
        _filter.value = _filter.value.copy(categoryId = next)
    }

    fun setDateRangeFilter(range: DateRangeFilter) {
        _filter.value = _filter.value.copy(dateRange = range)
    }

    fun toggleChecklistFilter() {
        val next = if (_filter.value.hasChecklist == true) null else true
        _filter.value = _filter.value.copy(hasChecklist = next)
    }

    fun toggleImagesFilter() {
        val next = if (_filter.value.hasImages == true) null else true
        _filter.value = _filter.value.copy(hasImages = next)
    }

    fun togglePinnedFilter() {
        _filter.value = _filter.value.copy(isPinnedOnly = !_filter.value.isPinnedOnly)
    }

    fun clearHistory() {
        _searchHistory.value = emptyList()
    }
}
