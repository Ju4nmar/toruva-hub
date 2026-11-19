package com.nexus.gamelauncher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColdStartTest {
    @Test fun foregroundReturnsAndActivityRecreationDoNotClaimTheIntroAgain() {
        val process = ColdStart()
        assertTrue(process.claim())
        assertFalse(process.claim())
        assertFalse(process.claim())
        assertTrue(ColdStart().claim())
    }
}
