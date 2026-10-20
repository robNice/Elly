# Elgy

Elgy turns your Android phone into a remote control for LG TVs running webOS. It recreates every key and function of the original LG Magic Remote, including the pointer, which can be controlled with your phone's motion sensors.

> **Status:** early development – the project skeleton is in place, remote functions are not implemented yet.

[Deutsche Version](README.de.md)

## Contents

1. [Features](#1-features)
2. [Requirements](#2-requirements)
3. [Connection to the TV](#3-connection-to-the-tv)
4. [Building](#4-building)
5. [Releases](#5-releases)

## 1 Features

- All keys of the LG Magic Remote: power, numbers, volume, channels, navigation pad with OK, home, settings, back, guide, input, Netflix, Prime Video, Movies, colour keys, play/pause and more.
- Keys are arranged on several screens so that everything fits without scrolling. Swipe to switch between screens.
- Pointer control via the phone's gyroscope: toggled by a dedicated key, switches off automatically after 5 seconds without movement.
- Search for LG TVs in your network (or enter the IP address manually) and pair them with the app.
- Dark design with realistic, three-dimensional keys.
- Home screen widgets (planned).
- Available in English and German.

## 2 Requirements

- Android 9 (API 28) or newer.
- LG TV with webOS in the same network as the phone.

## 3 Connection to the TV

Elgy communicates with the TV over the local network (Wi-Fi/LAN). On first connection, the TV asks you to allow access. To switch the TV on, Elgy uses Wake-on-LAN – this requires the corresponding option to be enabled on the TV (e.g. "Turn on via Wi-Fi" / "Mobile TV On").

## 4 Building

```
./gradlew :app:assembleDebug
```

Requires JDK 17 or newer and the Android SDK.

## 5 Releases

APKs are built automatically on GitHub:

- Tag `v*` → signed release APK and app bundle, published as a GitHub release.
- Tag `devbuild` → signed APK as a build artifact.
