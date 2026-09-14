package com.supermemo.app.domain.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

data class ChecklistProgress(
    val completed: Int,
    val total: Int
) {
    val progress: Float
        get() = if (total > 0) completed.toFloat() / total.toFloat() else 0f

    val isAllCompleted: Boolean
        get() = total > 0 && completed == total
}

data class ChecklistItem(
    val lineIndex: Int,
    val isCompleted: Boolean,
    val text: String
)

object MarkdownParser {

    private val CHECKLIST_REGEX = Regex("""^(\s*[-*+]\s+\[([ xX])\]\s+)(.*)$""")

    /**
     * 提取正文中的待办清单统计数据
     */
    fun extractChecklistProgress(content: String): ChecklistProgress? {
        val lines = content.lines()
        var total = 0
        var completed = 0

        for (line in lines) {
            val match = CHECKLIST_REGEX.find(line)
            if (match != null) {
                total++
                val checkMark = match.groupValues[2]
                if (checkMark.equals("x", ignoreCase = true)) {
                    completed++
                }
            }
        }

        return if (total > 0) ChecklistProgress(completed, total) else null
    }

    /**
     * 提取所有的待办条目列表
     */
    fun extractChecklistItems(content: String): List<ChecklistItem> {
        val list = mutableListOf<ChecklistItem>()
        val lines = content.lines()
        lines.forEachIndexed { index, line ->
            val match = CHECKLIST_REGEX.find(line)
            if (match != null) {
                val isCompleted = match.groupValues[2].equals("x", ignoreCase = true)
                val text = match.groupValues[3]
                list.add(ChecklistItem(lineIndex = index, isCompleted = isCompleted, text = text))
            }
        }
        return list
    }

    /**
     * 切换指定行待办项的完成状态，并返回更新后的全文
     */
    fun toggleChecklistItem(content: String, targetLineIndex: Int): String {
        val lines = content.lines().toMutableList()
        if (targetLineIndex !in lines.indices) return content

        val line = lines[targetLineIndex]
        val match = CHECKLIST_REGEX.find(line)
        if (match != null) {
            val prefix = match.groupValues[1]
            val isCompleted = match.groupValues[2].equals("x", ignoreCase = true)
            val text = match.groupValues[3]

            val newPrefix = if (isCompleted) {
                prefix.replace(Regex("""\[[xX]\]"""), "[ ]")
            } else {
                prefix.replace(Regex("""\[ \]"""), "[x]")
            }
            lines[targetLineIndex] = "$newPrefix$text"
        }

        return lines.joinToString("\n")
    }

    /**
     * 显式设置指定行待办项的完成状态（true: [x], false: [ ]）
     */
    fun setChecklistItemStatus(content: String, targetLineIndex: Int, isCompleted: Boolean): String {
        val lines = content.lines().toMutableList()
        if (targetLineIndex !in lines.indices) return content

        val line = lines[targetLineIndex]
        val match = CHECKLIST_REGEX.find(line)
        if (match != null) {
            val prefix = match.groupValues[1]
            val text = match.groupValues[3]
            val newPrefix = if (isCompleted) {
                prefix.replace(Regex("""\[[ xX]\]"""), "[x]")
            } else {
                prefix.replace(Regex("""\[[ xX]\]"""), "[ ]")
            }
            lines[targetLineIndex] = "$newPrefix$text"
        }

        return lines.joinToString("\n")
    }

    /**
     * 删除指定行的待办事项，并重新拼接正文
     */
    fun deleteChecklistItem(content: String, targetLineIndex: Int): String {
        val lines = content.lines().toMutableList()
        if (targetLineIndex !in lines.indices) return content
        lines.removeAt(targetLineIndex)
        return lines.joinToString("\n")
    }

    /**
     * 将备忘录正文整体转为待办事项清单（普通文本加 `- [ ]`，已有待办重置为未完成待办）
     */
    fun convertContentToTodoList(content: String, fallbackTitle: String = ""): String {
        val lines = content.lines()
        val nonBlankLines = lines.filter { it.isNotBlank() }
        if (nonBlankLines.isEmpty()) {
            val fallback = if (fallbackTitle.isNotBlank()) fallbackTitle else "待办事项"
            return "- [ ] $fallback"
        }
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                line
            } else if (CHECKLIST_REGEX.containsMatchIn(line)) {
                // 已有待办项重置为未完成待办
                line.replace(Regex("""\[[xX]\]"""), "[ ]")
            } else {
                // 普通文本转为未完成待办项
                "- [ ] $trimmed"
            }
        }
    }

    /**
     * 将备忘录中所有待办项设置为指定状态（true: 全部完成, false: 全部设为待办未完成）
     * 若不是待办清单，则整体转为对应状态的待办清单
     */
    fun setAllChecklistStatus(content: String, isCompleted: Boolean, fallbackTitle: String = ""): String {
        val targetTag = if (isCompleted) "[x]" else "[ ]"
        val existingItems = extractChecklistItems(content)
        if (existingItems.isEmpty()) {
            // 普通文本，转为对应状态的待办项
            val lines = content.lines()
            val nonBlankLines = lines.filter { it.isNotBlank() }
            if (nonBlankLines.isEmpty()) {
                val fallback = if (fallbackTitle.isNotBlank()) fallbackTitle else "待办事项"
                return "- $targetTag $fallback"
            }
            return lines.joinToString("\n") { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) line else "- $targetTag $trimmed"
            }
        }

        // 原本包含待办项，统一批量修改
        val lines = content.lines().toMutableList()
        for (i in lines.indices) {
            val line = lines[i]
            if (CHECKLIST_REGEX.containsMatchIn(line)) {
                lines[i] = line.replace(Regex("""\[[ xX]\]"""), targetTag)
            }
        }
        return lines.joinToString("\n")
    }

    /**
     * 快速追加一条待办事项
     */
    fun appendChecklistItem(content: String, text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return content
        val newItem = "- [ ] $trimmed"
        return if (content.isBlank()) {
            newItem
        } else {
            content.trimEnd() + "\n" + newItem
        }
    }

    /**
     * 将 Markdown 文本解析转换为富文本 AnnotatedString
     */
    fun renderMarkdown(
        content: String,
        primaryColor: Color = Color(0xFF6750A4),
        codeColor: Color = Color(0xFF006495)
    ): AnnotatedString {
        return buildAnnotatedString {
            val lines = content.lines()
            lines.forEachIndexed { index, line ->
                val lineStart = length

                when {
                    // H1
                    line.startsWith("# ") -> {
                        append(line.removePrefix("# "))
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp), lineStart, length)
                    }
                    // H2
                    line.startsWith("## ") -> {
                        append(line.removePrefix("## "))
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp), lineStart, length)
                    }
                    // H3
                    line.startsWith("### ") -> {
                        append(line.removePrefix("### "))
                        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp), lineStart, length)
                    }
                    // 引用块
                    line.startsWith("> ") -> {
                        append("▍ ")
                        val quoteContent = line.removePrefix("> ")
                        val startIdx = length
                        append(quoteContent)
                        addStyle(SpanStyle(color = primaryColor, fontStyle = FontStyle.Italic), lineStart, length)
                    }
                    // 待办事项
                    CHECKLIST_REGEX.matches(line) -> {
                        val match = CHECKLIST_REGEX.find(line)!!
                        val isDone = match.groupValues[2].equals("x", ignoreCase = true)
                        val text = match.groupValues[3]
                        append(if (isDone) "☑ " else "☐ ")
                        val textStart = length
                        append(text)
                        if (isDone) {
                            addStyle(
                                SpanStyle(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = Color.Gray
                                ),
                                textStart,
                                length
                            )
                        }
                    }
                    else -> {
                        // 解析行内样式：**粗体**、*斜体*、`代码`
                        renderInlineStyles(line, primaryColor, codeColor)
                    }
                }

                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.renderInlineStyles(
        line: String,
        primaryColor: Color,
        codeColor: Color
    ) {
        var cursor = 0
        while (cursor < line.length) {
            val boldStart = line.indexOf("**", cursor)
            val italicStart = line.indexOf("*", cursor)
            val codeStart = line.indexOf("`", cursor)

            // 优先处理 **粗体**
            if (boldStart != -1 && (italicStart == -1 || boldStart <= italicStart)) {
                val boldEnd = line.indexOf("**", boldStart + 2)
                if (boldEnd != -1) {
                    append(line.substring(cursor, boldStart))
                    val styledStart = length
                    append(line.substring(boldStart + 2, boldEnd))
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), styledStart, length)
                    cursor = boldEnd + 2
                    continue
                }
            }

            // 处理 `代码`
            if (codeStart != -1) {
                val codeEnd = line.indexOf("`", codeStart + 1)
                if (codeEnd != -1) {
                    append(line.substring(cursor, codeStart))
                    val styledStart = length
                    append(line.substring(codeStart + 1, codeEnd))
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = codeColor,
                            background = Color(0x15000000)
                        ),
                        styledStart,
                        length
                    )
                    cursor = codeEnd + 1
                    continue
                }
            }

            // 没有其他行内标记，追加剩余文本
            append(line.substring(cursor))
            break
        }
    }
}
