package com.coder.jetbrains.matcher

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class PortMatcherTest {

    @Test
    fun `test single port`() {
        val matcher = PortMatcher("3000")
        assertTrue(matcher.matches(3000))
        assertFalse(matcher.matches(2999))
        assertFalse(matcher.matches(3001))
    }

    @Test
    fun `test host colon port`() {
        val matcher = PortMatcher("localhost:3000")
        assertTrue(matcher.matches(3000))
        assertFalse(matcher.matches(3001))
    }

    @Test
    fun `test port range`() {
        val matcher = PortMatcher("40000-55000")
        assertFalse(matcher.matches(39999))
        assertTrue(matcher.matches(40000))
        assertTrue(matcher.matches(50000))
        assertTrue(matcher.matches(55000))
        assertFalse(matcher.matches(55001))
    }

    @Test
    fun `test regex`() {
        val matcher = PortMatcher("800[1-9]")
        assertFalse(matcher.matches(8000))
        assertTrue(matcher.matches(8001))
        assertTrue(matcher.matches(8005))
        assertTrue(matcher.matches(8009))
        assertFalse(matcher.matches(8010))
    }
}
