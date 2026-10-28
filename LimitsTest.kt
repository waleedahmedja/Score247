package com.waleedahmedja.score247.data

import com.waleedahmedja.score247.data.model.Limits
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitsTest {

    @Test fun cleanName_trimsSpaces() {
        assertEquals("Ali", Limits.cleanName("  Ali  "))
    }

    @Test fun cleanName_removesControlCharacters() {
        assertEquals("AliKhan", Limits.cleanName("Ali\n\tKhan"))
    }

    @Test fun cleanName_cutsOverlongNames() {
        val long = "x".repeat(200)
        assertEquals(Limits.NAME_LENGTH, Limits.cleanName(long).length)
    }

    @Test fun cleanName_keepsNonLatinNames() {
        assertEquals("علی", Limits.cleanName("علی"))
    }
}
