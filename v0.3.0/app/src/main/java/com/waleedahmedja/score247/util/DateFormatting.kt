package com.waleedahmedja.score247.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "11 Oct 2026, 2:16 pm" — in the phone's own language and time zone. */
fun formatMatchDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
