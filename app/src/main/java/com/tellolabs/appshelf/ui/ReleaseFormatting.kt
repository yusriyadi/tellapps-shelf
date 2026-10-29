package com.tellolabs.appshelf.ui

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatReleaseDate(value: String?, locale: Locale = Locale.getDefault()): String? {
    val raw = value?.takeIf { it.isNotBlank() } ?: return null
    val date = runCatching { LocalDate.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toLocalDate() }.getOrNull()
        ?: return raw
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "Ukuran belum diketahui"
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    else -> "%.1f GB".format(bytes.toDouble() / (1024L * 1024 * 1024))
}
