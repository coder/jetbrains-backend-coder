package com.coder.jetbrains.services

import com.coder.jetbrains.scanner.listeningPorts
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.application
import com.jetbrains.rd.platform.codeWithMe.portForwarding.GlobalPortForwardingManager
import com.jetbrains.rd.platform.codeWithMe.portForwarding.PortAlreadyForwardedException
import com.jetbrains.rd.platform.codeWithMe.portForwarding.PortType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val logger = thisLogger()
    private var poller: Job? = null

    // TODO: Make customizable.
    // TODO: I also see 63342, 57675, and 56830 for JetBrains.  Are they static?
    private val ignoreList = setOf(
        22,   // SSH
        5990, // JetBrains Gateway port.
    )

    init {
        logger.info("initializing port forwarding service")
        start()
    }

    override fun dispose() {
        poller?.cancel()
    }

    private fun start() {
        logger.info("starting port scanner")
        poller = cs.launch {
            while (isActive) {
                logger.debug("scanning for ports")
                val listeningPorts = withContext(Dispatchers.IO) {
                    listeningPorts().subtract(ignoreList)
                }
                application.invokeLater {
                    val manager = serviceOrNull<GlobalPortForwardingManager>()
                    if (manager == null) {
                        logger.warn("port forwarding manager is not available")
                        return@invokeLater
                    }

                    val ports = manager.getPorts()

                    // Remove ports that are no longer listening.
                    val removed = ports.filterNot { it.hostPortNumber in listeningPorts }
                    if (removed.isNotEmpty()) {
                        logger.info("removing ports: $removed")
                    }
                    removed.forEach {
                        try {
                            manager.removePort(it)
                        } catch (ex: Exception) {
                            logger.error("failed to remove port $it", ex)
                        }
                    }

                    // Add ports that are not yet listening.
                    val added = listeningPorts.subtract(ports.map { it.hostPortNumber }.toSet())
                    if (added.isNotEmpty()) {
                        logger.info("forwarding ports: $added")
                    }
                    added.forEach {
                        try {
                            // TODO: If privileged use a different port?
                            manager.forwardPort(it, PortType.TCP, setOf("coder"))
                        } catch (ex: PortAlreadyForwardedException) {
                            // All good.
                        } catch (ex: Exception) {
                            // TODO: Surface this to the user.
                            logger.error("failed to forward port $it", ex)
                        }
                    }
                }
                // TODO: Customizable interval.
                delay(5000)
            }
        }
    }
}
