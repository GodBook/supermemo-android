package com.supermemo.app.domain.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.domain.model.DateRangeFilter
import com.supermemo.app.domain.model.MatchType
import com.supermemo.app.domain.model.SearchFilter
import com.supermemo.app.domain.model.SearchResult
import com.supermemo.app.domain.model.SearchSortOrder

object SearchEngine {

    /**
     * 对候选备忘录列表执行高级检索、多维过滤、模糊打分与高亮摘录生成
     */
    fun search(
        notes: List<NoteWithDetails>,
        filter: SearchFilter,
        highlightColor: Color = Color(0xFF6750A4),
        highlightBg: Color = Color(0x336750A4)
    ): List<SearchResult> {
        val query = filter.query.trim().lowercase()
        val now = System.currentTimeMillis()

        val results = mutableListOf<SearchResult>()

        for (item in notes) {
            val note = item.note

            // 回收站与归档备忘默认不出现在常规搜索（除非已锁定特定上下文）
            if (note.isDeleted || note.isArchived) continue

            // 1. 分类过滤
            if (filter.categoryId != null && note.categoryId != filter.categoryId) {
                continue
            }

            // 2. 标签过滤
            if (filter.tagId != null && item.tags.none { it.id == filter.tagId }) {
                continue
            }

            // 3. 待办过滤
            if (filter.hasChecklist == true) {
                val hasCheck = note.content.contains("- [ ]") || note.content.contains("- [x]")
                if (!hasCheck) continue
            }

            // 4. 图片附件过滤
            if (filter.hasImages == true && item.attachments.isEmpty()) {
                continue
            }

            // 5. 置顶过滤
            if (filter.isPinnedOnly && !note.isPinned) {
                continue
            }

            // 6. 日期范围过滤
            when (filter.dateRange) {
                DateRangeFilter.ALL -> {}
                DateRangeFilter.TODAY -> {
                    val oneDayAgo = now - 24L * 3600L * 1000L
                    if (note.updatedAt < oneDayAgo) continue
                }
                DateRangeFilter.WEEK -> {
                    val oneWeekAgo = now - 7L * 24L * 3600L * 1000L
                    if (note.updatedAt < oneWeekAgo) continue
                }
                DateRangeFilter.MONTH -> {
                    val oneMonthAgo = now - 30L * 24L * 3600L * 1000L
                    if (note.updatedAt < oneMonthAgo) continue
                }
            }

            // 若搜索词为空，则展示满足过滤条件的全部项
            if (query.isEmpty()) {
                results.add(
                    SearchResult(
                        noteDetails = item,
                        titleHighlighted = AnnotatedString(note.title.ifEmpty { "无标题" }),
                        snippetHighlighted = AnnotatedString(getPlainSnippet(note.content)),
                        matchType = MatchType.EXACT_FALLBACK,
                        score = if (note.isPinned) 1000 else 0
                    )
                )
                continue
            }

            // 7. 关键词匹配与拼音打分
            val matchEvaluation = evaluateMatch(item, query)
            if (matchEvaluation != null) {
                val (matchType, score) = matchEvaluation

                // 生成高亮标题
                val titleHigh = highlightText(
                    text = note.title.ifEmpty { "无标题" },
                    query = query,
                    highlightColor = highlightColor,
                    highlightBg = highlightBg
                )

                // 生成高亮摘录
                val snippetHigh = createHighlightedSnippet(
                    content = note.content,
                    query = query,
                    highlightColor = highlightColor,
                    highlightBg = highlightBg
                )

                results.add(
                    SearchResult(
                        noteDetails = item,
                        titleHighlighted = titleHigh,
                        snippetHighlighted = snippetHigh,
                        matchType = matchType,
                        score = score + if (note.isPinned) 50 else 0
                    )
                )
            }
        }

        // 8. 排序
        return when (filter.sortOrder) {
            SearchSortOrder.UPDATED_DESC -> results.sortedWith(
                compareByDescending<SearchResult> { it.noteDetails.note.isPinned }
                    .thenByDescending { it.score }
                    .thenByDescending { it.noteDetails.note.updatedAt }
            )
            SearchSortOrder.CREATED_DESC -> results.sortedWith(
                compareByDescending<SearchResult> { it.noteDetails.note.isPinned }
                    .thenByDescending { it.noteDetails.note.createdAt }
            )
            SearchSortOrder.TITLE_ASC -> results.sortedBy { it.noteDetails.note.title.lowercase() }
        }
    }

    private fun evaluateMatch(item: NoteWithDetails, query: String): Pair<MatchType, Int>? {
        val title = item.note.title.lowercase()
        val content = item.note.content.lowercase()

        // 标题精确包含
        if (title.contains(query)) {
            return MatchType.TITLE_EXACT to 100
        }

        // 标题拼音首字母或全拼匹配
        val titleInitials = PinyinEngine.toInitialLetters(title)
        val titlePinyin = PinyinEngine.toFullPinyin(title)
        if (titleInitials.contains(query) || titlePinyin.contains(query)) {
            return MatchType.TITLE_PINYIN to 80
        }

        // 正文精确包含
        if (content.contains(query)) {
            return MatchType.CONTENT_EXACT to 60
        }

        // 正文拼音模糊匹配
        val contentInitials = PinyinEngine.toInitialLetters(content)
        val contentPinyin = PinyinEngine.toFullPinyin(content)
        if (contentInitials.contains(query) || contentPinyin.contains(query)) {
            return MatchType.CONTENT_PINYIN to 40
        }

        // 标题或正文离散子序列模糊匹配
        if (PinyinEngine.matchesFuzzy(title, query)) {
            return MatchType.FUZZY to 30
        }
        if (PinyinEngine.matchesFuzzy(content, query)) {
            return MatchType.FUZZY to 20
        }

        return null
    }

    /**
     * 将包含搜索词的文本生成带有高亮样式的 AnnotatedString
     */
    fun highlightText(
        text: String,
        query: String,
        highlightColor: Color,
        highlightBg: Color
    ): AnnotatedString {
        if (query.isEmpty()) return AnnotatedString(text)

        return buildAnnotatedString {
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()

            var cursor = 0
            while (cursor < text.length) {
                val matchIdx = lowerText.indexOf(lowerQuery, cursor)
                if (matchIdx == -1) {
                    append(text.substring(cursor))
                    break
                }
                if (matchIdx > cursor) {
                    append(text.substring(cursor, matchIdx))
                }
                val startStyle = length
                append(text.substring(matchIdx, matchIdx + query.length))
                addStyle(
                    SpanStyle(
                        color = highlightColor,
                        background = highlightBg,
                        fontWeight = FontWeight.Bold
                    ),
                    startStyle,
                    length
                )
                cursor = matchIdx + query.length
            }
        }
    }

    /**
     * 截取包含匹配词的上下文片段（前后各 25 个字符），并加上高亮
     */
    fun createHighlightedSnippet(
        content: String,
        query: String,
        highlightColor: Color,
        highlightBg: Color
    ): AnnotatedString {
        if (content.isEmpty()) return AnnotatedString("")
        if (query.isEmpty()) return AnnotatedString(getPlainSnippet(content))

        val lowerContent = content.lowercase()
        val lowerQuery = query.lowercase()

        val matchIndex = lowerContent.indexOf(lowerQuery)
        if (matchIndex == -1) {
            return AnnotatedString(getPlainSnippet(content))
        }

        val windowBefore = 20
        val windowAfter = 40

        val start = (matchIndex - windowBefore).coerceAtLeast(0)
        val end = (matchIndex + query.length + windowAfter).coerceAtMost(content.length)

        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""
        val snippetRaw = prefix + content.substring(start, end).replace('\n', ' ') + suffix

        return highlightText(snippetRaw, query, highlightColor, highlightBg)
    }

    private fun getPlainSnippet(content: String): String {
        val singleLine = content.lines().firstOrNull { it.isNotBlank() } ?: ""
        return if (singleLine.length > 50) singleLine.take(50) + "..." else singleLine
    }
}
