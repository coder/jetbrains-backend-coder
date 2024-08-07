# Coder Gateway backend plugin

![Build](https://github.com/coder/jetbrains-backend-coder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/coder-gateway-backend.svg)](https://plugins.jetbrains.com/plugin/coder-gateway-backend)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/coder-gateway-backend.svg)](https://plugins.jetbrains.com/plugin/coder-gateway-backend)

<!-- Plugin description -->
This plugin is meant to be installed on a remote machine and used as a companion
to using Coder through Gateway.
<!-- Plugin description end -->

## Installation

Download the [latest release](https://github.com/coder/jetbrains-backend-coder/releases/latest)
and install it manually using <kbd>Settings/Preferences</kbd> > <kbd>Plugins
<small>Host</small></kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
disk...</kbd>

Alternatively, you can add this plugin to your self-hosted marketplace if you
have one. This plugin is not currently hosted on the public JetBrains
marketplace.

Make sure to install it as a host plugin, not a client plugin.

## Features

- Scan `/proc/net/tcp*` files for listening ports and automatically forward
  them.
