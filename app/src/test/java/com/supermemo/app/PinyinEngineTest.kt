package com.supermemo.app

import com.supermemo.app.domain.engine.PinyinEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinEngineTest {

    @Test
    fun testPinyinInitials() {
        val text = "超级备忘录"
        val initials = PinyinEngine.toInitialLetters(text)
        assertEquals("cjbwl", initials)
    }

    @Test
    fun testPinyinFull() {
        val text = "工作会议"
        val full = PinyinEngine.toFullPinyin(text)
        assertEquals("gongzuohuiyi", full)
    }

    @Test
    fun testFuzzyMatching() {
        val title = "下午超级备忘录项目评审"

        // 包含首字母匹配
        assertTrue("应能通过拼音首字母匹配", PinyinEngine.matchesFuzzy(title, "cjbwl"))
        assertTrue("应能通过全拼匹配", PinyinEngine.matchesFuzzy(title, "chaoji"))
        assertTrue("应能通过中文子串匹配", PinyinEngine.matchesFuzzy(title, "备忘录"))
        assertTrue("应能通过离散模糊匹配", PinyinEngine.matchesFuzzy(title, "cbl"))
    }
}
