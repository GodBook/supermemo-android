package com.supermemo.app.ui.editor.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownToolbar(
    isPreviewMode: Boolean,
    onTogglePreview: () -> Unit,
    onInsertText: (prefix: String, suffix: String) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // H1
            IconButton(onClick = { onInsertText("\n# ", "") }) {
                Text("H1", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // H2
            IconButton(onClick = { onInsertText("\n## ", "") }) {
                Text("H2", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // H3
            IconButton(onClick = { onInsertText("\n### ", "") }) {
                Text("H3", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 待办清单
            IconButton(onClick = { onInsertText("\n- [ ] ", "") }) {
                Icon(Icons.Filled.CheckBox, contentDescription = "待办清单", tint = MaterialTheme.colorScheme.primary)
            }

            // 粗体
            IconButton(onClick = { onInsertText("**", "**") }) {
                Icon(Icons.Filled.FormatBold, contentDescription = "粗体")
            }

            // 斜体
            IconButton(onClick = { onInsertText("*", "*") }) {
                Icon(Icons.Filled.FormatItalic, contentDescription = "斜体")
            }

            // 删除线
            IconButton(onClick = { onInsertText("~~", "~~") }) {
                Icon(Icons.Filled.FormatStrikethrough, contentDescription = "删除线")
            }

            // 无序列表
            IconButton(onClick = { onInsertText("\n- ", "") }) {
                Icon(Icons.Filled.FormatListBulleted, contentDescription = "无序列表")
            }

            // 有序列表
            IconButton(onClick = { onInsertText("\n1. ", "") }) {
                Icon(Icons.Filled.FormatListNumbered, contentDescription = "有序列表")
            }

            // 引用
            IconButton(onClick = { onInsertText("\n> ", "") }) {
                Icon(Icons.Filled.FormatQuote, contentDescription = "引用")
            }

            // 代码
            IconButton(onClick = { onInsertText("`", "`") }) {
                Icon(Icons.Filled.Code, contentDescription = "行内代码")
            }

            // 插入图片
            IconButton(onClick = onPickImage) {
                Icon(Icons.Filled.Image, contentDescription = "插入图片", tint = MaterialTheme.colorScheme.secondary)
            }

            // 预览切换
            IconButton(onClick = onTogglePreview) {
                Icon(
                    Icons.Filled.Preview,
                    contentDescription = "实时渲染预览",
                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
