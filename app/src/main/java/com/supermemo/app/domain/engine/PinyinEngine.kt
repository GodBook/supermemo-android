package com.supermemo.app.domain.engine

/**
 * 高性能轻量拼音与模糊匹配引擎
 * 支持中文汉字转全拼、首字母缩写、以及子序列模糊匹配算法。
 */
object PinyinEngine {

    // 常用汉字拼音首字母对照区间（基于国家标准汉字GB2312一级字库内码拼音首字母边界）
    private val SEC_POS_VALUE_LIST = intArrayOf(
        1601, 1637, 1833, 2078, 2274, 2302, 2433, 2594, 2787,
        3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027, 4086,
        4390, 4558, 4684, 4925, 5249, 5600
    )
    private val FIRST_LETTER = charArrayOf(
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
        't', 'w', 'x', 'y', 'z'
    )

    // 常用多频字全拼与首字母速查精简词典（覆盖常用备忘录高频字）
    private val COMMON_PINYIN_MAP = mapOf(
        '超' to "chao", '级' to "ji", '备' to "bei", '忘' to "wang", '录' to "lu",
        '会' to "hui", '议' to "yi", '工' to "gong", '作' to "zuo", '计' to "ji",
        '划' to "hua", '生' to "sheng", '活' to "huo", '购' to "gou", '物' to "wu",
        '清' to "qing", '单' to "dan", '学' to "xue", '习' to "xi", '总' to "zong",
        '结' to "jie", '灵' to "ling", '感' to "gan", '随' to "sui", '笔' to "bi",
        '重' to "zhong", '要' to "yao", '紧' to "jin", '急' to "ji", '日' to "ri",
        '记' to "ji", '账' to "zhang", '密' to "mi", '码' to "ma", '私' to "si",
        '人' to "ren", '待' to "dai", '办' to "ban", '项' to "xiang", '目' to "mu",
        '开' to "kai", '发' to "fa", '测' to "ce", '试' to "shi", '提' to "ti",
        '醒' to "xing", '旅' to "lv", '行' to "xing", '阅' to "yue", '读' to "du",
        '书' to "shu", '电' to "dian", '话' to "hua", '邮' to "you", '件' to "jian",
        '文' to "wen", '档' to "dang", '设' to "she", '计' to "ji", '代' to "dai",
        '码' to "ma", '修' to "xiu", '复' to "fu", '发' to "fa", '布' to "bu",
        '钱' to "qian", '银' to "yin", '行' to "hang", '卡' to "ka", '车' to "che",
        '房' to "fang", '家' to "jia", '友' to "you", '朋' to "peng", '亲' to "qin",
        '爱' to "ai", '健' to "jian", '康' to "kang", '医' to "yi", '药' to "yao",
        '吃' to "chi", '饭' to "fan", '买' to "mai", '卖' to "mai", '跑' to "pao",
        '步' to "bu", '健' to "jian", '身' to "shen", '电' to "dian", '影' to "ying",
        '音' to "yin", '乐' to "yue", '游' to "you", '戏' to "xi", '旅' to "lv"
    )

    /**
     * 获取单个中文字符的首字母
     */
    fun getInitialLetter(ch: Char): Char {
        if (ch.code in 0..127) {
            return ch.lowercaseChar()
        }
        val mapped = COMMON_PINYIN_MAP[ch]
        if (mapped != null && mapped.isNotEmpty()) {
            return mapped[0]
        }
        return try {
            val bytes = ch.toString().toByteArray(charset("GB2312"))
            if (bytes.size < 2) return ch
            val secPosValue = (bytes[0].toInt() + 256) * 100 + (bytes[1].toInt() + 256)
            for (i in 0 until 23) {
                if (secPosValue >= SEC_POS_VALUE_LIST[i] && secPosValue < SEC_POS_VALUE_LIST[i + 1]) {
                    return FIRST_LETTER[i]
                }
            }
            ch
        } catch (_: Exception) {
            ch
        }
    }

    /**
     * 将一段文本提取为纯首字母简拼缩写字符串
     * 例如："超级备忘录" -> "cjbwl"
     */
    fun toInitialLetters(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch.isLetterOrDigit()) {
                sb.append(getInitialLetter(ch))
            }
        }
        return sb.toString()
    }

    /**
     * 将一段文本转换为全拼连续字符串
     * 例如："项目会议" -> "xiangmuhuiyi"
     */
    fun toFullPinyin(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            val pinyin = COMMON_PINYIN_MAP[ch]
            if (pinyin != null) {
                sb.append(pinyin)
            } else if (ch.code in 0..127) {
                if (ch.isLetterOrDigit()) sb.append(ch.lowercaseChar())
            } else {
                sb.append(getInitialLetter(ch))
            }
        }
        return sb.toString()
    }

    /**
     * 综合多重模糊匹配：
     * 1. 文本直接子串包含
     * 2. 首字母缩写包含（如 cjbw 命中 超级备忘录）
     * 3. 全拼模糊包含（如 chaoji 命中 超级备忘录）
     * 4. 离散子序列模糊匹配（如 cbl 匹配 c...b...l...）
     */
    fun matchesFuzzy(source: String, query: String): Boolean {
        if (query.isBlank()) return true
        if (source.isBlank()) return false

        val cleanQuery = query.trim().lowercase()
        val cleanSource = source.lowercase()

        // 1. 直接包含
        if (cleanSource.contains(cleanQuery)) return true

        // 2. 首字母缩写包含
        val initials = toInitialLetters(source)
        if (initials.contains(cleanQuery)) return true

        // 3. 全拼包含
        val fullPinyin = toFullPinyin(source)
        if (fullPinyin.contains(cleanQuery)) return true

        // 4. 离散子序列模糊匹配 (Subsequence fuzzy matching)
        return isSubsequence(cleanQuery, cleanSource) || isSubsequence(cleanQuery, initials)
    }

    private fun isSubsequence(pattern: String, text: String): Boolean {
        var pIdx = 0
        var tIdx = 0
        while (pIdx < pattern.length && tIdx < text.length) {
            if (pattern[pIdx] == text[tIdx]) {
                pIdx++
            }
            tIdx++
        }
        return pIdx == pattern.length
    }
}
