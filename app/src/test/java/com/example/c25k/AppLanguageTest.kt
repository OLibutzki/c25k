package com.example.c25k

import com.example.c25k.domain.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `fromTag parses known tags and defaults to english`() {
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.DE, AppLanguage.fromTag("de"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("unknown"))
    }
}
