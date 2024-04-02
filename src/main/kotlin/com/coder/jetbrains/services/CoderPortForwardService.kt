package com.coder.jetbrains.services

import com.coder.jetbrains.scanner.listeningPorts
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.application
import com.jetbrains.rd.platform.codeWithMe.portForwarding.ClientPortAttributes
import com.jetbrains.rd.platform.codeWithMe.portForwarding.ClientPortPickingStrategy
import com.jetbrains.rd.platform.codeWithMe.portForwarding.PerClientPortForwardingManager
import com.jetbrains.rd.platform.codeWithMe.portForwarding.PortAlreadyForwardedException
import com.jetbrains.rd.platform.codeWithMe.portForwarding.PortType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Automatically forward ports that have something listening on them by scanning
 * /proc/net/ipv{,6} at a regular interval.
 *
 * If a process stops listening the port forward is removed.
 *
 * If the user manually removes a port, it will be added back at the next
 * interval.
 */
@Suppress("UnstableApiUsage")
class CoderPortForwardService(
    private val cs: CoroutineScope,
): Disposable {
    private var poller: Job? = null

    // TODO: Make customizable.
    private val ignoreList = setOf(
        22,   // SSH
        5990, // Default JetBrains remote port.
    )

    init {
        thisLogger().info("initializing port forwarding service")
        application.invokeLater {
            start()
        }
    }

    override fun dispose() {
        poller?.cancel()
    }

    private fun start() {
        thisLogger().info("starting port scanner")
        poller = cs.launch {
            while (isActive) {
                thisLogger().debug("scanning for ports")
                val listeningPorts = listeningPorts().subtract(ignoreList)
                val manager = service<PerClientPortForwardingManager>()
                val ports = manager.getPorts()
                // Remove ports that are no longer listening.
                // TODO: Only remove ones we added?
                ports.forEach { old ->
                    if (!listeningPorts.contains(old.hostPortNumber)) {
                        try {
                            thisLogger().info("removing port ${old.hostPortNumber}")
                            manager.removePort(old)
                        } catch (ex: Exception) {
                            thisLogger().error("failed to remove port $old", ex)
                        }
                    }
                }
                // Add ports that are not yet listening.
                // TODO: Avoid adding if the user removed it previously?
                listeningPorts.forEach {
                    try {
                        thisLogger().info("forwarding port $it")
                        forwardPort(it)
                    } catch (ex: PortAlreadyForwardedException) {
                        // All good.
                    } catch (ex: Exception) {
                        thisLogger().error("failed to forward port $it", ex)
                    }
                }
                // TODO: Customizable interval.
                delay(5000)
            }
        }
    }

    /**
     * Add a port forward to the provided port on the host.
     *
     * TODO: If privileged, use a different port.
     */
    private fun forwardPort(port: Int) {
        val manager = service<PerClientPortForwardingManager>()
        manager.forwardPort(port, PortType.TCP, setOf("coder"), ClientPortAttributes(
            preferredPortNumber = port,
            strategy = ClientPortPickingStrategy.REASSIGN_WHEN_BUSY,
        ))
    }
}
