package com.supermemo.app

import com.supermemo.app.domain.engine.UpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testVersionComparison() {
        assertTrue("1.2.0 应高于 1.1.0", UpdateManager.isNewerVersion("1.1.0", "v1.2.0"))
        assertTrue("1.1.1 应高于 1.1.0", UpdateManager.isNewerVersion("1.1.0", "1.1.1"))
        assertTrue("2.0.0 应高于 1.1.0", UpdateManager.isNewerVersion("1.1.0", "v2.0.0"))

        assertFalse("相同版本应返回 false", UpdateManager.isNewerVersion("1.1.0", "v1.1.0"))
        assertFalse("旧版本应返回 false", UpdateManager.isNewerVersion("1.1.0", "v1.0.0"))
        assertFalse("旧版本应返回 false", UpdateManager.isNewerVersion("1.1.0", "0.9.5"))
    }
}
