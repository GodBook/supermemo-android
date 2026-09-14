package com.supermemo.app.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.IconButton
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.domain.engine.ChecklistItem
import com.supermemo.app.domain.engine.MarkdownParser
import com.supermemo.app.ui.theme.parseHexColor
import com.supermemo.app.util.ImageStorageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    noteDetails: NoteWithDetails,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isBlinkEnabled: Boolean = true,
    blinkColorHex: String = "#FF9800",
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleChecklistItem: (lineIndex: Int) -> Unit,
    onChecklistItemLongClick: ((noteId: Long, item: ChecklistItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val note = noteDetails.note
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val cardBg = parseHexColor(note.colorHex, MaterialTheme.colorScheme.surfaceVariant)

    // 待办事项动态呼吸闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "ChecklistBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )
    val blinkColor = parseHexColor(blinkColorHex, Color(0xFFFF9800))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 顶部信息栏：分类色点、置顶图钉、私密锁、多选复选框
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (noteDetails.category != null) {
                        val catColor = parseHexColor(noteDetails.category.colorHex, MaterialTheme.colorScheme.primary)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(catColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = noteDetails.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "置顶",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (note.isLocked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "私密锁",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标题
            Text(
                text = if (note.isLocked) "私密备忘录（已加锁）" else note.title.ifEmpty { "未命名备忘录" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (note.isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 正文或已加锁遮罩
            if (note.isLocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "指纹或密码验证后可查看内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 待办清单进度与快捷交互
                val checklistProgress = MarkdownParser.extractChecklistProgress(note.content)
                if (checklistProgress != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { checklistProgress.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${checklistProgress.completed}/${checklistProgress.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 预览前 3 个待办清单项：点击切换打勾，长按或点⋯弹出设置待办/完成/删除快捷菜单
                    val items = MarkdownParser.extractChecklistItems(note.content).take(3)
                    items.forEach { item ->
                        val isItemBlinking = isBlinkEnabled && !item.isCompleted
                        val itemRowBg = if (isItemBlinking) {
                            blinkColor.copy(alpha = blinkAlpha * 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemRowBg)
                                .pointerInput(item.lineIndex, note.id, item.isCompleted) {
                                    detectTapGestures(
                                        onTap = {
                                            onToggleChecklistItem(item.lineIndex)
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onChecklistItemLongClick?.invoke(note.id, item)
                                        }
                                    )
                                }
                                .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            // 未完成呼吸指示小圆点
                            if (isItemBlinking) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(blinkColor.copy(alpha = blinkAlpha), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Icon(
                                imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (item.isCompleted) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isItemBlinking) {
                                    blinkColor.copy(alpha = (blinkAlpha * 0.6f + 0.4f))
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = if (isItemBlinking) FontWeight.Medium else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (item.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            // 专属快捷操作图标按钮（点击直达快捷弹窗！）
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onChecklistItemLongClick?.invoke(note.id, item)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "待办操作",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // 常规正文预览
                    val plainSnippet = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
                    if (plainSnippet.isNotEmpty()) {
                        Text(
                            text = plainSnippet,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 图片附件缩略图预览
                if (noteDetails.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val firstAtt = noteDetails.attachments.first()
                    val imgFile = ImageStorageHelper.getAttachmentFile(context, firstAtt.relativePath)
                    if (imgFile.exists()) {
                        AsyncImage(
                            model = imgFile,
                            contentDescription = "图片附件",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // 底部标签云与更新时间
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (noteDetails.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        noteDetails.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "#${tag.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
