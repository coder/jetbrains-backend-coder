package com.coder.jetbrains.settings

import java.io.File

object CoderBackendSettings {
    fun getDevcontainerFile(): File {
        // TODO: make path configurable?
        return File(System.getProperty("user.home"), ".cache/JetBrains/devcontainer.json")
    }
}
