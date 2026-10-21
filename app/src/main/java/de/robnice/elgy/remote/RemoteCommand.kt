package de.robnice.elgy.remote

sealed interface RemoteCommand {
    data class Button(val name: String) : RemoteCommand
    data class Request(val uri: String) : RemoteCommand
    data class LaunchApp(val appId: String) : RemoteCommand
    data object Power : RemoteCommand
    data object InputPicker : RemoteCommand
    data object Unsupported : RemoteCommand
}
