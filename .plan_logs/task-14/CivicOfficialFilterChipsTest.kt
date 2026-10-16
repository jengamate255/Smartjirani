package com.smartjirani.feature.civic.official

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CivicOfficialFilterChipsTest {
    @Test
    fun `filter enum has five values`() {
        val filters = CivicOfficialFilter.values()
        assertEquals(5, filters.size)
    }

    @Test
    fun `filter enum values match RPC contract`() {
        assertEquals("all", CivicOfficialFilter.ALL.value)
        assertEquals("awaiting_verification", CivicOfficialFilter.AWAITING_VERIFICATION.value)
        assertEquals("overdue", CivicOfficialFilter.OVERDUE.value)
        assertEquals("mine", CivicOfficialFilter.MINE.value)
        assertEquals("new_today", CivicOfficialFilter.NEW_TODAY.value)
    }

    @Test
    fun `each filter has a label resource`() {
        CivicOfficialFilter.values().forEach { filter ->
            val labelRes = filterLabelRes(filter)
            assertNotNull("Missing label resource for $filter", labelRes)
            assert(labelRes != 0) { "Label resource for $filter is 0" }
        }
    }

    @Test
    fun `filter values are unique`() {
        val values = CivicOfficialFilter.values().map { it.value }
        assertEquals(values.size, values.toSet().size)
    }
}
