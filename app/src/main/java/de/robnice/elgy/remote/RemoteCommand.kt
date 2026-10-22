package de.robnice.elgy.remote

sealed interface RemoteCommand {
    data class Button(val name: String) : RemoteCommand
    data class Request(val uri: String, val fallbackUri: String? = null) : RemoteCommand
    data class LaunchApp(val appId: String) : RemoteCommand
    data object Power : RemoteCommand
    data object InputPicker : RemoteCommand
    data object TextInput : RemoteCommand
    data object Unsupported : RemoteCommand
}
