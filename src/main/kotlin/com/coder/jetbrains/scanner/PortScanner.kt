package com.coder.jetbrains.scanner

import java.io.File

private val sources = listOf(
    "/proc/net/tcp",
    "/proc/net/tcp6"
)

private val whitespaceRe = "\\s+".toRegex()

/**
 * Parse a TCP socket /proc interface.
 */
fun readTcpFile(file: File): Set<Int> {
    /*
     * A typical entry of /proc/net/tcp would look like this (only the first
     * four parts since that is all we use):
     *
     * 46: 010310AC:9C4C 030310AC:1770 01
     * |      |      |      |      |   |--> connection state
     * |      |      |      |      |------> remote TCP port number
     * |      |      |      |-------------> remote IPv4 address
     * |      |      |--------------------> local TCP port number
     * |      |---------------------------> local IPv4 address
     * |----------------------------------> number of entry
     *
     * connection states:
     *   TCP_ESTABLISHED,
     *   TCP_SYN_SENT,
     *   TCP_SYN_RECV,
     *   TCP_FIN_WAIT1,
     *   TCP_FIN_WAIT2,
     *   TCP_TIME_WAIT,
     *   TCP_CLOSE,
     *   TCP_CLOSE_WAIT,
     *   TCP_LAST_ACK,
     *   TCP_LISTEN,
     *   TCP_CLOSING,
     *   TCP_NEW_SYN_RECV,
     */
    return try {
        file.readLines()
            .asSequence()
            // Fields are separated by spaces.
            .map { it.trim().split(whitespaceRe) }
            // Only TCP_LISTEN.
            .filter { it.size >= 4 && it[3] == "0A" }
            // Local address.
            .map { it[1].split(":") }
            .filter { it.size == 2 }
            // Decode port from hex.
            .map { it[1].toIntOrNull(16) }
            .filterNotNull()
            .toSet()
    } catch (ex: Exception) {
        // TODO: Log this exception?
        emptySet()
    }
}

/**
 * Return a list of ports with listening TCP sockets.  It does not differentiate
 * between ipv4 and ipv6, and it does not return addresses since the forwarding
 * API does not appear to let us give it an address anyway.
 *
 * It only supports Linux, as that is the only place remote IDEs can run anyway.
 */
fun listeningPorts(): Set<Int> {
    // REVIEW: We could instead query /workspaceagents/me/listening-ports (after
    // adding it to coderd) which would let us share the port scanning code with
    // the agent?
    return sources.flatMap { readTcpFile(File(it)) }.toSet()
}
