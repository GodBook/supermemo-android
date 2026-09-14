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
}
