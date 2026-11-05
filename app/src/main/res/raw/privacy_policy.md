## 1. General
Elly is a remote control for LG TVs with webOS on the local network, on the phone and on a Wear OS watch.

The developer of this app does not collect, store or process any personal data. The app does not require a user account.

## 2. Network communication
The app communicates with the TV exclusively within your local network:

- Searching for TVs: SSDP request and a check of the addresses in your local network for the webOS ports 3000 and 3001.
- Control: encrypted connection to the paired TV (older TVs without encryption via port 3000). The app transmits control commands such as keys, pointer movements, app launches, input and channel changes, and requests the installed apps, the inputs and the app icons.
- Power on: Wake-on-LAN packet to the TV's MAC address.
- Program guide (optional, only for TVs with root access that you set up yourself): SSH connection to the TV (port 22). The app reads the TV's favorite lists, channels, program guide and planned reminders and recordings, plans or deletes reminders and recordings on the TV and reads channel logos. Logos the TV does not have yet are downloaded by the TV itself from LG's logo server, as it does for its own guide; your device does not contact that server. The same applies to program pictures from LG's online guide data: when you open a program's details, the TV downloads its picture from LG's server and passes it on; your device keeps it only in memory while the app is running.

This communication takes place directly between your device and the TV. No data is transmitted to external servers.

## 3. Watch (Wear OS)
Like the phone, the watch app controls the TV directly over your Wi-Fi (control commands, pointer movements, power on via Wake-on-LAN, opening the TV's input picker).

So that you do not have to pair the TV again on the watch, the phone transfers the following data to the paired watch while Elly is installed there:

- Name, model, IP address and MAC address of paired TVs, their pairing key and certificate fingerprint
- which TV is active and whether the demo mode is on
- the pages and key layout of the watch app and the icons of the apps that watch keys open

The transfer uses the Wearable interface of Google Play services, which connects the phone and the watch. If the phone and the watch are not directly connected via Bluetooth or Wi-Fi, Google Play services may transfer the data between your devices in encrypted form through Google's servers. Once no watch with Elly is known any more, the phone deletes the data from this interface. Credentials for root access and the program guide are not transferred to the watch.

## 4. Data storage
The following data is stored locally on your device:

- Name, model, IP address and MAC address of paired TVs
- Pairing key and certificate fingerprint of the TV
- App settings (screens, design, app keys, pointer, vibration, demo mode, watch pages and keys)
- copies of the pictures you choose as screen backgrounds; the app only receives the pictures you select in the Android photo picker
- App icons loaded from the TV
- for the program guide: SSH user, port and the fingerprint of the TV's SSH key; the TV's root password or the private SSH key you entered (with its passphrase), encrypted with a key that is kept in the Android Keystore and cannot be read out of the device; the program guide (channel lists and programs of the last 24 hours and up to 14 days ahead) and channel logos.

On the watch, Elly stores the data transferred from the phone (see section 3) and the watch settings (vibration, circling gesture, light effect, pointer, chosen TV).

This data remains on your devices, is excluded from Android backups and is deleted when the app is uninstalled.

## 5. Sensors
In the pointer's motion mode, the app uses your device's gyroscope. On the watch, the pointer page uses the watch's gyroscope while a finger rests on the display. The sensor data is only processed then, converted directly into pointer movements and not stored.

## 6. Permissions
On the phone:

- Internet: connection to the TV on the local network (Android requires this permission for any network connection)
- Network and Wi-Fi state: determining the local network for the search
- Wi-Fi multicast: receiving the responses when searching for TVs
- Vibration: short feedback when a key on a home screen widget is pressed
- Foreground service: sending a key pressed on a home screen widget to the TV, also while the app is in the background

On the watch:

- Internet and network state: connection to the TV on the local network
- Change network: the watch connects to your Wi-Fi for the control instead of routing the connection through the phone
- Vibration: feedback on keys and while the volume changes
- Keep awake: keeps Wi-Fi responsive on the pointer page

No other permissions are used, in particular no access to location, microphone, camera or contacts.

## 7. Third parties
This app does not use any analytics tools or advertising. To keep the phone and the watch in sync it uses the Wearable interface of Google Play services (see section 3); Google's privacy policy applies to it as well.

## 8. Changes to this privacy policy
This privacy policy may be updated if necessary to reflect changes to the app or legal requirements.

## 9. Contact
If you have any questions about privacy, you can contact the developer.

E-mail: elly@robnice.de
