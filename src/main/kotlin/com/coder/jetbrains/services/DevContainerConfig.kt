package com.coder.jetbrains.services

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevContainerConfig(
    @SerialName("portsAttributes")
    val portsAttributes: Map<String, PortAttributes> = mapOf(),
    @SerialName("otherPortsAttributes")
    val otherPortsAttributes: OtherPortsAttributes? = null
)

@Serializable
data class PortAttributes(
    @SerialName("onAutoForward")
    val onAutoForward: String = ""
)

@Serializable
data class OtherPortsAttributes(
    @SerialName("onAutoForward")
    val onAutoForward: String = ""
)
