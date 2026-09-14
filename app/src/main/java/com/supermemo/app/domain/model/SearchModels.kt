package com.supermemo.app.domain.model

import androidx.compose.ui.text.AnnotatedString
import com.supermemo.app.data.local.model.NoteWithDetails

enum class DateRangeFilter(val label: String) {
    ALL("全部时间"),
    TODAY("今天"),
    WEEK("近7天"),
    MONTH("近30天")
}

enum class SearchSortOrder(val label: String) {
    UPDATED_DESC("最近修改"),
    CREATED_DESC("最新创建"),
    TITLE_ASC("标题 A-Z")
}

data class SearchFilter(
    val query: String = "",
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val hasChecklist: Boolean? = null,
    val hasImages: Boolean? = null,
    val isPinnedOnly: Boolean = false,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL,
    val sortOrder: SearchSortOrder = SearchSortOrder.UPDATED_DESC
)

enum class MatchType {
    TITLE_EXACT,
    TITLE_PINYIN,
    CONTENT_EXACT,
    CONTENT_PINYIN,
    FUZZY,
    EXACT_FALLBACK
}

data class SearchResult(
    val noteDetails: NoteWithDetails,
    val titleHighlighted: AnnotatedString,
    val snippetHighlighted: AnnotatedString,
    val matchType: MatchType,
    val score: Int
)
