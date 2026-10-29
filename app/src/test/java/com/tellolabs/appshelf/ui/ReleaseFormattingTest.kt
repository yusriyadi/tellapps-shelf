package com.tellolabs.appshelf.ui

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ReleaseFormattingTest {
    private val locale = Locale.US

    @Test fun supportsDateAndTimestampWithoutChangingReleaseDay() {
        assertEquals("Oct 29, 2026", formatReleaseDate("2026-10-29", locale))
        assertEquals("Oct 29, 2026", formatReleaseDate("2026-10-29T23:30:00-07:00", locale))
    }

    @Test fun missingOrInvalidDatesDoNotCrash() {
        assertNull(formatReleaseDate(null, locale))
        assertNull(formatReleaseDate("", locale))
        assertEquals("not-a-date", formatReleaseDate("not-a-date", locale))
    }

    @Test fun supportsLargeAndUnknownSizes() {
        assertEquals("Ukuran belum diketahui", formatBytes(-1))
        assertEquals("512 B", formatBytes(512))
        assertEquals("2 KB", formatBytes(2048))
        assertTrue(formatBytes(2L * 1024 * 1024 * 1024).endsWith("GB"))
    }
}
