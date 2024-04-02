package com.coder.jetbrains.scanner

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Path

internal class PortScannerTest {
    @Test
    fun testNoFile() {
        assertEquals(emptySet<Int>(), readTcpFile(File("/does/not/exist")))
    }

    @Test
    fun testScanner() {
        val tests = listOf(
            Pair("tcp", setOf(59574, 13337, 59738, 13339, 2112, 2113, 5990, 6060, 63342)),
            Pair("tcp6", setOf(8000, 6901, 22)))
        tests.forEach {
            assertEquals(it.second,
                readTcpFile(Path.of("src/test/testData").resolve(it.first).toFile()))
        }
    }
}
