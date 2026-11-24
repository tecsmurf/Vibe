package com.vibe.musicassistant.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantStatusTest {
    @Test
    fun everyStatusHasAUniqueVisibleLabel() {
        val labels = AssistantStatus.entries.map { it.label }
        assertTrue(labels.all { it.isNotBlank() })
        assertEquals(labels.size, labels.toSet().size)
    }
}
