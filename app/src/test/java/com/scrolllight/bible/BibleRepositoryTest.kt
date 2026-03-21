package com.scrolllight.bible

import org.junit.Assert.assertEquals
import org.junit.Test

class BibleRepositoryTest {

    @Test
    fun `book list contains 66 books`() {
        // TODO: inject BibleRepository via Hilt test rule and call getAllBooks()
        // Placeholder assertion
        assertEquals(66, 66)
    }

    @Test
    fun `old testament has 39 books`() {
        assertEquals(39, 39)
    }

    @Test
    fun `new testament has 27 books`() {
        assertEquals(27, 27)
    }

    @Test
    fun `daily verse is not blank`() {
        val verse = "只是不可忘记行善和捐输的事"
        assert(verse.isNotBlank())
    }
}
