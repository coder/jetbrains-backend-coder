package com.coder.jetbrains.matcher

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows

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
    fun `test port range with whitespace`() {
        val matcher = PortMatcher("20021  - 20024")
        assertFalse(matcher.matches(20000))
        assertTrue(matcher.matches(20022))
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

    @Test
    fun `test invalid port numbers`() {
        assertThrows(IllegalArgumentException::class.java) { PortMatcher("65536") }
        assertThrows(IllegalArgumentException::class.java) { PortMatcher("0-65536") }
        assertThrows(IllegalArgumentException::class.java) { PortMatcher("70000") }
    }

    @Test
    fun `test edge case port numbers`() {
        // These should work
        PortMatcher("0")
        PortMatcher("65535")
        PortMatcher("0-65535")

        // These combinations should work
        val matcher = PortMatcher("0-65535")
        assertTrue(matcher.matches(0))
        assertTrue(matcher.matches(65535))
    }
}
