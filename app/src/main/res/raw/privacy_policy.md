## 1. General
Elgy is a remote control for LG TVs with webOS on the local network.

The developer of this app does not collect, store or process any personal data. The app does not require a user account.

## 2. Network communication
The app communicates exclusively within your local network:

- Searching for TVs: SSDP request and a check of the addresses in your local network for the webOS ports 3000 and 3001.
- Control: encrypted connection to the paired TV (older TVs without encryption via port 3000). The app transmits control commands such as keys, pointer movements, app launches and input changes, and requests the installed apps, the inputs and the app icons.
- Power on: Wake-on-LAN packet to the TV's MAC address.

This communication takes place directly between your device and the TV. No data is transmitted to external servers.

## 3. Data storage
The following data is stored locally on your device:

- Name, model, IP address and MAC address of paired TVs
- Pairing key and certificate fingerprint of the TV
- App settings (screens, design, app keys, pointer, vibration, demo mode)
- copies of the pictures you choose as screen backgrounds; the app only receives the pictures you select in the Android photo picker
- App icons loaded from the TV

This data remains exclusively on your device, is excluded from Android backups and is deleted when the app is uninstalled.

## 4. Sensors
In the pointer's motion mode, the app uses your device's gyroscope. The sensor data is only processed while the pointer mode is active and the app is visible, converted directly into pointer movements and not stored.

## 5. Permissions
- Internet: connection to the TV on the local network (Android requires this permission for any network connection)
- Network and Wi-Fi state: determining the local network for the search
- Wi-Fi multicast: receiving the responses when searching for TVs
- Vibration: short feedback when a key on a home screen widget is pressed
- Foreground service: sending a key pressed on a home screen widget to the TV, also while the app is in the background

No other permissions are used, in particular no access to location, microphone, camera or contacts.

## 6. Third parties
This app does not use any third-party services, analytics tools or advertising.

## 7. Changes to this privacy policy
This privacy policy may be updated if necessary to reflect changes to the app or legal requirements.

## 8. Contact
If you have any questions about privacy, you can contact the developer.

E-mail: elgy@robnice.de
