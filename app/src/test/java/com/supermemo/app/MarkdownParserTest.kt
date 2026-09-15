package com.supermemo.app

import com.supermemo.app.domain.engine.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testChecklistProgress() {
        val markdown = """
            # 待办清单
            - [x] 第一项任务
            - [ ] 第二项任务
            - [x] 第三项任务
            - [ ] 第四项任务
        """.trimIndent()

        val progress = MarkdownParser.extractChecklistProgress(markdown)
        assertNotNull(progress)
        assertEquals(4, progress!!.total)
        assertEquals(2, progress.completed)
        assertEquals(0.5f, progress.progress, 0.01f)
    }

    @Test
    fun testToggleChecklistItem() {
        val markdown = "- [ ] 买牛奶\n- [x] 买面包"
        val toggled = MarkdownParser.toggleChecklistItem(markdown, 0)
        assertTrue("第一行应被勾选", toggled.startsWith("- [x] 买牛奶"))

        val untoggled = MarkdownParser.toggleChecklistItem(toggled, 1)
        assertTrue("第二行应被取消勾选", untoggled.contains("- [ ] 买面包"))
    }

    @Test
    fun testSetChecklistItemStatus() {
        val markdown = "- [ ] 任务A\n- [x] 任务B"
        // 显式将任务A设为完成
        val res1 = MarkdownParser.setChecklistItemStatus(markdown, 0, true)
        assertTrue("任务A应被显式标记完成", res1.startsWith("- [x] 任务A"))

        // 显式将任务B设为未完成待办
        val res2 = MarkdownParser.setChecklistItemStatus(res1, 1, false)
        assertTrue("任务B应被显式设为未完成待办", res2.contains("- [ ] 任务B"))
    }

    @Test
    fun testDeleteChecklistItem() {
        val markdown = "- [ ] 任务A\n- [ ] 待删除任务\n- [ ] 任务C"
        val result = MarkdownParser.deleteChecklistItem(markdown, 1)
        assertTrue("不应再包含待删除任务", !result.contains("待删除任务"))
        assertEquals("- [ ] 任务A\n- [ ] 任务C", result)
    }

    @Test
    fun testUpdateChecklistItemContent() {
        val markdown = "- [ ] 买牛奶\n- [x] 买面包"
        val updated = MarkdownParser.updateChecklistItemContent(markdown, 0, "买脱脂牛奶")
        assertEquals("- [ ] 买脱脂牛奶\n- [x] 买面包", updated)

        val updatedCompleted = MarkdownParser.updateChecklistItemContent(markdown, 1, "全麦吐司")
        assertEquals("- [ ] 买牛奶\n- [x] 全麦吐司", updatedCompleted)
    }

    @Test
    fun testSinkCompletedChecklistItems() {
        val markdown = """
            # 今日计划
            - [x] 任务一 (已完成)
            - [ ] 任务二 (待办)
            - [x] 任务三 (已完成)
            - [ ] 任务四 (待办)
        """.trimIndent()

        val sunk = MarkdownParser.sinkCompletedChecklistItems(markdown)
        val lines = sunk.lines()
        assertEquals("# 今日计划", lines[0])
        assertTrue("未完成任务应排在前", lines[1].startsWith("- [ ] 任务二"))
        assertTrue("未完成任务应排在前", lines[2].startsWith("- [ ] 任务四"))
        assertTrue("已完成任务应沉底", lines[3].startsWith("- [x] 任务一"))
        assertTrue("已完成任务应沉底", lines[4].startsWith("- [x] 任务三"))
    }

    @Test
    fun testSmartEnter() {
        // 场景1：在有内容的待办后回车，自动追加新待办
        val oldText = "- [ ] 买牛奶"
        val enterText = "- [ ] 买牛奶\n"
        val res1 = MarkdownParser.handleSmartEnter(oldText, enterText, enterText.length)
        assertNotNull(res1)
        assertEquals("- [ ] 买牛奶\n- [ ] ", res1!!.newText)
        assertEquals(enterText.length + 6, res1.newCursorPosition)

        // 场景2：在空白待办行回车，自动清除待办退出
        val oldEmpty = "- [ ] 买牛奶\n- [ ] "
        val enterEmpty = "- [ ] 买牛奶\n- [ ] \n"
        val res2 = MarkdownParser.handleSmartEnter(oldEmpty, enterEmpty, enterEmpty.length)
        assertNotNull(res2)
        assertEquals("- [ ] 买牛奶\n", res2!!.newText)
    }
}

