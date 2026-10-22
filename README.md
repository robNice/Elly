# Elgy

Elgy turns your Android phone into a remote control for LG TVs running webOS. It recreates the keys and functions of the original LG Magic Remote, including the pointer, which can be controlled with your phone's motion sensors or a touch area.

> **Status:** early development – searching for and pairing TVs, the remote keys, the pointer, freely configurable screens and home screen widgets work; some key assignments still need to be verified on more TVs.

[Deutsche Version](README.de.md)

<p>
  <img src="docs/en/remote_navigation.png" width="200" alt="Navigation screen">
  &nbsp;
  <img src="docs/en/remote_numbers.png" width="200" alt="Numbers screen">
  &nbsp;
  <img src="docs/en/remote_media.png" width="200" alt="Media &amp; Apps screen">
</p>

## Contents

1. [Features at a glance](#1-features-at-a-glance)
2. [Requirements](#2-requirements)
3. [Connecting a TV](#3-connecting-a-tv)
4. [The screens](#4-the-screens)
   - [4.1 Header](#41-header)
   - [4.2 Navigation](#42-navigation)
   - [4.3 Numbers](#43-numbers)
   - [4.4 Media and apps](#44-media-and-apps)
   - [4.5 Custom screen](#45-custom-screen)
5. [Pointer](#5-pointer)
   - [5.1 Motion mode](#51-motion-mode)
   - [5.2 Touchpad mode](#52-touchpad-mode)
6. [Editing screens](#6-editing-screens)
   - [6.1 Editor](#61-editor)
   - [6.2 App keys](#62-app-keys)
7. [Widgets](#7-widgets)
8. [Settings](#8-settings)
   - [8.1 TV](#81-tv)
   - [8.2 Screens](#82-screens)
   - [8.3 Pointer](#83-pointer)
   - [8.4 Vibration and demo mode](#84-vibration-and-demo-mode)
   - [8.5 Version and privacy](#85-version-and-privacy)
9. [Building](#9-building)
10. [Releases](#10-releases)

## 1 Features at a glance

- Keys of the LG Magic Remote on up to four screens that fit without scrolling – swipe to switch between them.
- All screens freely configurable: place, move and resize keys, rockers, the directional pad and app keys in the editor; every screen can be reset to its default layout.
- Dark design with three-dimensional keys and optional vibration feedback.
- Status light in the header that shows whether the TV is reachable.
- Pointer like the Magic Remote: controlled by moving the phone or via a touch area (touchpad or analog joystick).
- App keys that open an app on the TV, with the app's icon loaded from the TV.
- Input picker that lists the inputs reported by the TV.
- Switch the TV screen off and on again while the sound keeps playing.
- Six small home screen widgets that can be combined freely.
- Searching for TVs in the network, pairing, switching on via Wake-on-LAN.
- Demo mode to try the app without an LG TV.
- Available in English and German.

## 2 Requirements

- Android 9 (API 28) or newer. From Android 12, the widgets take on the colours of the wallpaper (Material You).
- LG TV with webOS in the same network as the phone.
- For switching the TV on: the option "Turn on via Wi-Fi" (on some models "Mobile TV On") must be enabled on the TV.

## 3 Connecting a TV

On first start, Elgy opens the connection screen:

1. **Search** – Elgy looks for LG TVs in the local network. Found TVs are listed with name, model and IP address.
2. **Enter IP address** – if the search finds nothing, enter the TV's IP address manually.
3. **Pair** – select a TV and accept the connection request on the TV. Elgy remembers the TV, so this is only needed once.

Several TVs can be paired; the active TV is selected in the list. Elgy also remembers the TV's network address (MAC) to be able to switch it on later.

## 4 The screens

The following sections describe the default layout. Every screen can be rearranged freely in the [editor](#61-editor).

### 4.1 Header

The header is visible on every screen:

- **Status light and TV name** – the light shows the connection state: green connected, red not reachable, grey checking, yellow demo mode. While the app is visible, the connection is checked every 10 seconds and after every command. Tapping the name opens the connection screen (search, pair, switch TV).
- **Pointer key** (mouse symbol) – opens the pointer, see [Pointer](#5-pointer). The symbol is tinted while the pointer is active.
- **Gear** – opens the app settings.

The dots at the bottom show the current screen. Swipe left or right to switch. Keys only fire when the finger is lifted, so a swipe that starts on a key does not send anything. Volume, channel and the directional pad repeat while held.

### 4.2 Navigation

<img src="docs/en/remote_navigation.png" width="270" alt="Navigation screen">

| Key | Function |
|---|---|
| Power (red) | Switches the TV off; if the TV is not reachable, it is switched on via Wake-on-LAN. |
| Home | webOS home screen. |
| Gear | TV settings. |
| Live TV (TV with antenna) | Switches to live TV (antenna, cable, satellite). |
| Volume rocker | Volume up / down. |
| Mute | Mutes / unmutes the sound. |
| CH rocker | Channel up / down. |
| Directional pad and OK | Navigation and confirm. |
| Back | One step back. |
| Screen off / on | Switches only the TV screen off or on again; the sound keeps playing. |
| Guide | Programme guide. |

### 4.3 Numbers

<img src="docs/en/remote_numbers.png" width="270" alt="Numbers screen">

| Key | Function |
|---|---|
| Input | Opens a list of the TV's inputs (e.g. HDMI 1, HDMI 2) and switches to the selected one. Inputs without a connected device are marked. |
| Live TV | Switches to live TV. |
| 0–9 | Number keys, e.g. for channel numbers. |
| GUIDE | Programme guide. |
| ••• | Opens the TV menu "More actions". |
| Back, OK | One step back / confirm. |

<img src="docs/en/input_picker.png" width="270" alt="Input picker">

### 4.4 Media and apps

<img src="docs/en/remote_media.png" width="270" alt="Media &amp; Apps screen">

| Key | Function |
|---|---|
| Record (red dot) | Starts a recording (if supported by the TV). |
| Stop | Stops playback. |
| Rewind, Pause, Play, Fast forward | Playback control. |
| Directional pad, OK, Back | Navigation as on the first screen. |
| Colour keys | Red, green, yellow, blue. |
| App keys | Open an app on the TV, preset with Netflix, Prime Video and Rakuten TV, see [App keys](#62-app-keys). |

### 4.5 Custom screen

The fourth screen starts empty and hidden. Show it in the settings under [Screens](#82-screens) and fill it in the [editor](#61-editor).

## 5 Pointer

The pointer key in the header opens an overlay above the current screen. In both modes:

- **OK** clicks at the pointer position – several clicks in a row are possible.
- The **close button** (×) at the top right or the back gesture ends pointer mode; the cursor on the TV disappears.
- After 5 seconds without activity, pointer mode ends automatically.

### 5.1 Motion mode

<img src="docs/en/pointer_motion.png" width="270" alt="Pointer in motion mode">

The pointer follows the movements of your phone – like aiming with the Magic Remote. Two ways of holding the phone are supported and recognised automatically:

- **Like a remote** (screen facing up, top edge pointing at the TV): swing left/right, tilt the top edge up/down.
- **Upright in front of you** (back facing the TV): turn left/right around the vertical axis, tilt forwards/backwards.

Twisting the phone around the aiming axis is ignored on purpose because it happens unintentionally while aiming. Sensor data is smoothed; quick swings move the pointer further than slow movements. Swipe up or down on the **scroll area** to scroll.

### 5.2 Touchpad mode

<p>
  <img src="docs/en/pointer_touch_swipe.png" width="270" alt="Pointer in touchpad mode (swipe)">
  &nbsp;
  <img src="docs/en/pointer_touch_joystick.png" width="270" alt="Pointer in touchpad mode (joystick)">
</p>

The pointer is controlled with the touch area. A tap on the area without swiping clicks like OK. Two input styles are available:

- **Swipe** – the pointer follows your finger like on a laptop touchpad.
- **Joystick** – like an analog stick: the further you pull in a direction, the faster the pointer moves. *Free*: the stick starts at the first touch point. *Fixed*: the stick is always visible in the middle of the area. In both cases the dead zone is drawn as an inner circle; inside it, the pointer does not move.

The OK button can be placed at the bottom (default) or at the top.

## 6 Editing screens

### 6.1 Editor

The editor is opened in the settings under [Screens](#82-screens) with a screen's pencil. It shows the screen's grid (24 × 48 cells) in the proportions of the remote screen.

- **Element** (at the bottom) adds an element: volume rocker, channel rocker, directional pad, app key or any key of the remote. The element is placed on the first free spot.
- **Drag** to move an element.
- **Tap** to select an element; then drag the corner handle to resize it.
- **Drag onto the trash bin** to remove an element.
- **Reset** restores the screen's default layout after a confirmation.
- **Done** saves and closes the editor.

The layout is saved per TV.

### 6.2 App keys

<p>
  <img src="docs/en/settings_app_picker.png" width="270" alt="Choose an app">
  &nbsp;
  <img src="docs/en/settings_app_style.png" width="270" alt="Style of an app key">
</p>

App keys are placed in the editor. **Holding an app key for 2 seconds** opens the "App key" window with a preview:

- **Choose app** – loads the apps installed on the TV and assigns one of them. The app icon is loaded from the TV and stored on the phone so it is shown even without a connection. *Remove app* clears the key.
- **Style** – show the app icon and/or text, enter your own text (prefilled with the app name) and pick a text colour. With icon and text, the icon is on the left and the text follows. The text shrinks automatically so it always fits the key.

Changes are applied with OK. If an app key has no icon yet, Elgy loads it as soon as the TV is connected – also after a screen has been reset.

## 7 Widgets

Elgy comes with six small home screen widgets that can be combined freely:

| Widget | Size | Keys |
|---|---|---|
| Elgy volume | 1 × 2, height adjustable | Volume up / down |
| Elgy channel | 1 × 2, height adjustable | Channel up / down |
| Elgy playback | 3 × 1, width adjustable | Rewind, Play, Pause, Fast forward, Stop |
| Elgy power | 1 × 1 | Power |
| Elgy mute | 1 × 1 | Mute |
| Elgy screen | 2 × 1, width adjustable | Screen off / on |

- A widget controls the TV that is active in the app. Via **widget settings** (long-press the widget) it can be bound to a specific TV.
- The app does not have to be open. A key press vibrates if [Vibrate on key press](#84-vibration-and-demo-mode) is on; if the TV is not reachable, a message is shown.
- From Android 12, the widgets take on the colours of the wallpaper and follow the light and dark theme.

## 8 Settings

### 8.1 TV

<img src="docs/en/settings_top.png" width="270" alt="Settings: TV and screens">

Shows the active TV. **Connect another TV** opens the connection screen, **Disconnect** removes the active TV after confirmation.

### 8.2 Screens

The four screens are shown as previews of their current layout:

- **Tap** to show or hide a screen.
- **Long-press and drag** to change the order.
- **Pencil** opens the [editor](#61-editor).
- **Arrow** resets the screen to its default layout after a confirmation.
- At least one screen always stays active. If only one screen is active, the page dots disappear.

The selection is saved per TV.

### 8.3 Pointer

<img src="docs/en/settings_pointer.png" width="270" alt="Settings: pointer">

- **Motion** – with the sub-options *Sensitivity* (0.3× to 3.0×) and *Acceleration* (0× to 4×; 0 = no acceleration). Only available if the phone has a gyroscope.
- **Touchpad** – with the sub-options *Touch input* (swipe or joystick, for the joystick with *Joystick position* free or fixed) and *OK button* (bottom or top).

### 8.4 Vibration and demo mode

<img src="docs/en/settings_bottom.png" width="270" alt="Settings: vibration and demo mode">

- **Vibrate on key press** – short vibration when a key is pressed (also when clicking in touchpad mode and on the widgets).
- **Demo mode** – simulates a connected TV ("Demo TV", yellow status light). All screens can be used without an LG TV, no commands are sent. Demo mode has its own screen layouts and its own screen selection.

### 8.5 Version and privacy

The bottom of the settings always shows the version number (left) and the **Privacy** link (right), which opens the privacy policy.

## 9 Building

```
./gradlew :app:assembleDebug
```

Requires JDK 17 or newer and the Android SDK.

## 10 Releases

APKs are built automatically on GitHub:

- Tag `v*` → signed release APK and app bundle, published as a GitHub release.
- Tag `devbuild` → signed APK as a build artifact.
