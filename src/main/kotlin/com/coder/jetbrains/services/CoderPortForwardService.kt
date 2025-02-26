package com.coder.jetbrains.services

import com.coder.jetbrains.matcher.PortMatcher
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
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import com.coder.jetbrains.settings.CoderBackendSettings

/**
 * Automatically forward ports that have something listening on them by scanning
 * /proc/net/ipv{,6} at a regular interval.
 *
 * If a process stops listening the port forward is removed.
 */
@Suppress("UnstableApiUsage")
class CoderPortForwardService(
    private val cs: CoroutineScope,
): Disposable {
    private val logger = thisLogger()
    private var poller: Job? = null

    private data class PortRule(
        val matcher: PortMatcher,
        val autoForward: Boolean
    )
    // TODO: I also see 63342, 57675, and 56830 for JetBrains.  Are they static?
    // TODO: If you have multiple IDEs, you will see 5991. 5992, etc.  Can we
    //       detect all of these and exclude them?
    private val rules = mutableListOf(
        PortRule(PortMatcher("22"), false),
        PortRule(PortMatcher("5990"), false),
    )

    private var defaultForward = true

    init {
        logger.info("initializing port forwarding service")
        start()
    }

    override fun dispose() {
        poller?.cancel()
    }

    companion object {
    @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            allowTrailingComma = true
            allowComments = true
        }
    }

    private fun start() {
        val devcontainerFile = CoderBackendSettings.getDevcontainerFile()
        if (devcontainerFile.exists()) {
            try {
                val jsonContent = devcontainerFile.readText()
                val config = json.decodeFromString<DevContainerConfig>(jsonContent)

                // Process port attributes
                config.portsAttributes.forEach { (spec, attrs) ->
                    when (attrs.onAutoForward) {
                        "ignore" -> {
                            logger.info("found ignored port specification $spec in devcontainer.json")
                            rules.add(0, PortRule(PortMatcher(spec), false))
                        }
                        "" -> {}
                        else -> {
                            logger.info("found auto-forward port specification $spec in devcontainer.json")
                            rules.add(0, PortRule(PortMatcher(spec), true))
                        }
                    }
                }

                // Process other ports attributes
                config.otherPortsAttributes?.let {
                    if (it.onAutoForward == "ignore") {
                        logger.info("found ignored setting for otherPortsAttributes in devcontainer.json")
                        defaultForward = false
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse devcontainer.json", e)
            }
        }

        logger.info("starting port scanner")
        poller = cs.launch {
            while (isActive) {
                logger.debug("scanning for ports")
                val listeningPorts = withContext(Dispatchers.IO) {
                    listeningPorts().filter { port ->
                        val matchedRule = rules.firstOrNull { it.matcher.matches(port) }
                        matchedRule?.autoForward ?: defaultForward
                    }.toSet()
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
                        logger.info("removing ports: ${removed.map { it.hostPortNumber }}")
                    }
                    removed.forEach {
                        try {
                            manager.removePort(it)
                        } catch (ex: Exception) {
                            logger.error("failed to remove port ${it.hostPortNumber}", ex)
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
