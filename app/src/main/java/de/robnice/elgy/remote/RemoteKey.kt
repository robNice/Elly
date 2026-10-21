package de.robnice.elgy.remote

enum class RemoteKey(val command: RemoteCommand) {
    POWER(RemoteCommand.Power),

    DIGIT_0(RemoteCommand.Button("0")),
    DIGIT_1(RemoteCommand.Button("1")),
    DIGIT_2(RemoteCommand.Button("2")),
    DIGIT_3(RemoteCommand.Button("3")),
    DIGIT_4(RemoteCommand.Button("4")),
    DIGIT_5(RemoteCommand.Button("5")),
    DIGIT_6(RemoteCommand.Button("6")),
    DIGIT_7(RemoteCommand.Button("7")),
    DIGIT_8(RemoteCommand.Button("8")),
    DIGIT_9(RemoteCommand.Button("9")),
    MORE_ACTIONS(RemoteCommand.Button("SCREEN_REMOTE")),

    VOLUME_UP(RemoteCommand.Request("ssap://audio/volumeUp")),
    VOLUME_DOWN(RemoteCommand.Request("ssap://audio/volumeDown")),
    MUTE(RemoteCommand.Button("MUTE")),
    CHANNEL_UP(RemoteCommand.Request("ssap://tv/channelUp")),
    CHANNEL_DOWN(RemoteCommand.Request("ssap://tv/channelDown")),
    LIVE_TV(RemoteCommand.LaunchApp("com.webos.app.livetv")),


    HOME(RemoteCommand.Button("HOME")),
    SETTINGS(RemoteCommand.Button("MENU")),
    UP(RemoteCommand.Button("UP")),
    DOWN(RemoteCommand.Button("DOWN")),
    LEFT(RemoteCommand.Button("LEFT")),
    RIGHT(RemoteCommand.Button("RIGHT")),
    OK(RemoteCommand.Button("ENTER")),
    BACK(RemoteCommand.Button("BACK")),
    GUIDE(RemoteCommand.Button("GUIDE")),

    INPUT(RemoteCommand.InputPicker),

    RED(RemoteCommand.Button("RED")),
    GREEN(RemoteCommand.Button("GREEN")),
    YELLOW(RemoteCommand.Button("YELLOW")),
    BLUE(RemoteCommand.Button("BLUE")),

    PLAY(RemoteCommand.Request("ssap://media.controls/play")),
    PAUSE(RemoteCommand.Request("ssap://media.controls/pause")),
    REWIND(RemoteCommand.Request("ssap://media.controls/rewind")),
    FAST_FORWARD(RemoteCommand.Request("ssap://media.controls/fastForward")),
    RECORD(RemoteCommand.Button("RECORD")),
    LIVE_STOP(RemoteCommand.Button("STOP")),
    LIVE_PLAY(RemoteCommand.Button("PLAY"))
}
