# Elgy

Elgy macht dein Android-Handy zur Fernbedienung für LG-Fernseher mit webOS. Die App bildet die Tasten und Funktionen der originalen LG Magic Remote nach – einschließlich des Zeigers, der sich über die Bewegungssensoren des Handys oder eine Touch-Fläche steuern lässt.

> **Status:** frühe Entwicklung – Suchen und Koppeln von Fernsehern, die Fernbedienungstasten, der Zeiger und die App-Tasten funktionieren; einzelne Tastenbelegungen müssen noch an weiteren Fernsehern geprüft werden.

[English version](README.md)

<p>
  <img src="docs/de/remote_navigation.png" width="200" alt="Screen Navigation">
  &nbsp;
  <img src="docs/de/remote_numbers.png" width="200" alt="Screen Zahlen">
  &nbsp;
  <img src="docs/de/remote_media.png" width="200" alt="Screen Media &amp; Apps">
</p>

## Inhalt

1. [Funktionen im Überblick](#1-funktionen-im-ueberblick)
2. [Voraussetzungen](#2-voraussetzungen)
3. [Fernseher verbinden](#3-fernseher-verbinden)
4. [Die Screens](#4-die-screens)
   - [4.1 Kopfleiste](#41-kopfleiste)
   - [4.2 Navigation](#42-navigation)
   - [4.3 Zahlen](#43-zahlen)
   - [4.4 Media und Apps](#44-media-und-apps)
5. [Zeiger](#5-zeiger)
   - [5.1 Bewegungsmodus](#51-bewegungsmodus)
   - [5.2 Touchpad-Modus](#52-touchpad-modus)
6. [Einstellungen](#6-einstellungen)
   - [6.1 Fernseher](#61-fernseher)
   - [6.2 Screens](#62-screens)
   - [6.3 Zeiger](#63-zeiger)
   - [6.4 App-Tasten](#64-app-tasten)
   - [6.5 Vibration und Demo-Modus](#65-vibration-und-demo-modus)
7. [Bauen](#7-bauen)
8. [Releases](#8-releases)

## 1 Funktionen im Ueberblick

- Tasten der LG Magic Remote auf bis zu drei Screens, die ohne Scrollen auskommen – gewechselt wird per Wischen.
- Dunkles Design mit dreidimensionalen Tasten und optionalem Vibrationsfeedback.
- Zeiger wie bei der Magic Remote: Steuerung durch Bewegen des Handys oder über eine Touch-Fläche (Touchpad oder analoger Joystick).
- Bis zu neun frei belegbare App-Tasten pro Fernseher, mit dem App-Icon vom Fernseher.
- Eingangsauswahl mit den Eingängen, die der Fernseher meldet.
- Screens lassen sich pro Fernseher ausblenden und umsortieren.
- Fernseher im Netzwerk suchen, koppeln, per Wake-on-LAN einschalten.
- Demo-Modus zum Ausprobieren ohne LG-Fernseher.
- Verfügbar auf Deutsch und Englisch.

## 2 Voraussetzungen

- Android 9 (API 28) oder neuer.
- LG-Fernseher mit webOS im selben Netzwerk wie das Handy.
- Zum Einschalten des Fernsehers: Am Fernseher muss die Option „Über WLAN einschalten“ (bei manchen Modellen „Mobile TV On“) aktiviert sein.

## 3 Fernseher verbinden

Beim ersten Start öffnet Elgy den Verbindungs-Screen:

1. **Suchen** – Elgy sucht LG-Fernseher im lokalen Netzwerk. Gefundene Fernseher erscheinen mit Name, Modell und IP-Adresse.
2. **IP-Adresse eingeben** – findet die Suche nichts, lässt sich die IP-Adresse des Fernsehers manuell eingeben.
3. **Koppeln** – Fernseher auswählen und die Verbindungsanfrage am Fernseher bestätigen. Elgy merkt sich den Fernseher, das ist also nur einmal nötig.

Es lassen sich mehrere Fernseher koppeln; der aktive Fernseher wird in der Liste ausgewählt. Elgy merkt sich außerdem die Netzwerkadresse (MAC) des Fernsehers, um ihn später einschalten zu können.

## 4 Die Screens

### 4.1 Kopfleiste

Die Kopfleiste ist auf jedem Screen sichtbar:

- **Name des Fernsehers** – öffnet den Verbindungs-Screen (suchen, koppeln, Fernseher wechseln).
- **Zeiger-Taste** (Maus-Symbol) – öffnet den Zeiger, siehe [Zeiger](#5-zeiger).
- **Zahnrad** – öffnet die Einstellungen der App.

Die Punkte unten zeigen den aktuellen Screen. Gewechselt wird durch Wischen nach links oder rechts. Tasten lösen erst beim Loslassen aus – ein Wisch, der auf einer Taste beginnt, sendet also nichts. Lautstärke, Kanal und das Steuerkreuz wiederholen beim Gedrückthalten.

### 4.2 Navigation

<img src="docs/de/remote_navigation.png" width="270" alt="Screen Navigation">

| Taste | Funktion |
|---|---|
| Ein/Aus (rot) | Schaltet den Fernseher aus; ist er nicht erreichbar, wird er per Wake-on-LAN eingeschaltet. |
| Home | Startseite von webOS. |
| Zahnrad | Einstellungen des Fernsehers. |
| Live-TV (Fernseher mit Antenne) | Wechselt zum Fernsehempfang (Antenne, Kabel, Satellit). |
| Lautstärke-Wippe | Lauter / leiser. |
| Stumm | Ton aus / an. |
| CH-Wippe | Kanal hoch / runter. |
| Steuerkreuz und OK | Navigation und Bestätigen. |
| Zurück | Einen Schritt zurück. |
| Guide | Programmführer. |

### 4.3 Zahlen

<img src="docs/de/remote_numbers.png" width="270" alt="Screen Zahlen">

| Taste | Funktion |
|---|---|
| Eingang | Öffnet eine Liste der Eingänge des Fernsehers (z. B. HDMI 1, HDMI 2) und schaltet auf den gewählten um. Eingänge ohne angeschlossenes Gerät sind gekennzeichnet. |
| Live-TV | Wechselt zum Fernsehempfang. |
| 0–9 | Zifferntasten, z. B. für Kanalnummern. |
| GUIDE | Programmführer. |
| ••• | Öffnet das Menü „Weitere Aktionen“ des Fernsehers. |
| Zurück, OK | Einen Schritt zurück / Bestätigen. |

<img src="docs/de/input_picker.png" width="270" alt="Eingangsauswahl">

### 4.4 Media und Apps

<img src="docs/de/remote_media.png" width="270" alt="Screen Media &amp; Apps">

| Taste | Funktion |
|---|---|
| Aufnahme (roter Punkt) | Startet eine Aufnahme (sofern vom Fernseher unterstützt). |
| Live Playback stoppen / starten | Beendet oder startet Live Playback (LGs Bezeichnung für zeitversetztes Fernsehen). |
| Zurückspulen, Pause, Play, Vorspulen | Steuerung der Wiedergabe. |
| Steuerkreuz, OK, Zurück | Navigation wie auf dem ersten Screen. |
| Farbtasten | Rot, Grün, Gelb, Blau. |
| App-Tasten | Bis zu neun Tasten, die eine App am Fernseher öffnen, siehe [App-Tasten](#64-app-tasten). |

## 5 Zeiger

Die Zeiger-Taste in der Kopfleiste öffnet ein Overlay über dem aktuellen Screen. In beiden Modi gilt:

- **OK** klickt an der Zeigerposition – mehrere Klicks hintereinander sind möglich.
- Der **Beenden-Button** (×) oben rechts oder die Zurück-Geste beendet den Zeiger-Modus; der Cursor am Fernseher verschwindet.
- Nach 5 Sekunden ohne Aktivität endet der Zeiger-Modus automatisch.

### 5.1 Bewegungsmodus

<img src="docs/de/pointer_motion.png" width="270" alt="Zeiger im Bewegungsmodus">

Der Zeiger folgt den Bewegungen des Handys – wie beim Zielen mit der Magic Remote. Zwei Haltungen werden unterstützt und automatisch erkannt:

- **Wie eine Fernbedienung** (Bildschirm nach oben, Oberkante zeigt zum Fernseher): links/rechts schwenken, Oberkante hoch/runter kippen.
- **Hochkant vor dem Körper** (Rückseite zeigt zum Fernseher): um die senkrechte Achse nach links/rechts drehen, nach vorne/hinten kippen.

Ein Verdrehen des Handys um die Zeigerichtung wird bewusst ignoriert, weil es beim Zielen unbewusst passiert. Die Sensordaten werden geglättet; schnelle Schwenks bewegen den Zeiger weiter als langsame Bewegungen. Auf der **Scroll-Fläche** wird durch Wischen nach oben oder unten gescrollt.

### 5.2 Touchpad-Modus

<p>
  <img src="docs/de/pointer_touch_swipe.png" width="270" alt="Zeiger im Touchpad-Modus (Wischen)">
  &nbsp;
  <img src="docs/de/pointer_touch_joystick.png" width="270" alt="Zeiger im Touchpad-Modus (Joystick)">
</p>

Der Zeiger wird über die Touch-Fläche gesteuert. Ein Tippen auf die Fläche ohne Wischen klickt wie OK. Zwei Eingabearten stehen zur Wahl:

- **Wischen** – der Zeiger folgt dem Finger wie auf dem Touchpad eines Laptops.
- **Joystick** – wie ein analoger Stick: Ausgehend vom ersten Berührungspunkt bewegt sich der Zeiger umso schneller, je weiter man in eine Richtung zieht.

Der OK-Button lässt sich unten (Standard) oder oben platzieren.

## 6 Einstellungen

### 6.1 Fernseher

<img src="docs/de/settings_top.png" width="270" alt="Einstellungen: Fernseher und Screens">

Zeigt den aktiven Fernseher. **Anderen Fernseher verbinden** öffnet den Verbindungs-Screen, **Verbindung aufheben** entfernt den aktiven Fernseher nach einer Rückfrage.

### 6.2 Screens

Die drei Screens werden als vereinfachte Vorschaubilder angezeigt:

- **Antippen** blendet einen Screen ein oder aus.
- **Gedrückt halten und ziehen** ändert die Reihenfolge.
- Mindestens ein Screen bleibt immer aktiv. Ist nur ein Screen aktiv, verschwinden die Seitenpunkte.

Die Auswahl wird pro Fernseher gespeichert.

### 6.3 Zeiger

<img src="docs/de/settings_pointer.png" width="270" alt="Einstellungen: Zeiger">

- **Bewegung** – mit den Unteroptionen *Empfindlichkeit* (0,3× bis 3,0×) und *Beschleunigung* (0× bis 4×; 0 = keine Beschleunigung). Nur verfügbar, wenn das Handy ein Gyroskop hat.
- **Touchpad** – mit den Unteroptionen *Touch-Eingabe* (Wischen oder Joystick) und *OK-Button* (unten oder oben).

### 6.4 App-Tasten

<p>
  <img src="docs/de/settings_app_keys.png" width="270" alt="Einstellungen: App-Tasten">
  &nbsp;
  <img src="docs/de/settings_app_picker.png" width="270" alt="App auswählen">
  &nbsp;
  <img src="docs/de/settings_app_style.png" width="270" alt="Darstellung einer App-Taste">
</p>

Bis zu neun Tasten auf dem Media-Screen, gespeichert pro Fernseher. Für jede Taste gibt es:

- **Schalter** – blendet die Taste ein oder aus.
- **Wählen** – lädt die auf dem Fernseher installierten Apps und weist eine davon zu. Das App-Icon wird vom Fernseher geladen und auf dem Handy gespeichert, damit es auch ohne Verbindung angezeigt wird. *App entfernen* leert die Taste.
- **Darstellung** – App-Icon und/oder Text anzeigen, eigenen Text eingeben (mit dem App-Namen vorbelegt) und eine Textfarbe wählen. Mit Icon und Text steht das Icon links, der Text folgt dahinter. Der Text verkleinert sich automatisch, damit er immer in die Taste passt.

Vorbelegung: Netflix, Prime Video, Rakuten TV.

### 6.5 Vibration und Demo-Modus

<img src="docs/de/settings_bottom.png" width="270" alt="Einstellungen: Vibration und Demo-Modus">

- **Vibration bei Tastendruck** – kurzes Vibrieren beim Drücken einer Taste (auch beim Klicken im Touchpad-Modus).
- **Demo-Modus** – simuliert einen verbundenen Fernseher („Demo-TV“). Alle Screens sind ohne LG-Fernseher nutzbar, es werden keine Befehle gesendet. Der Demo-Modus hat eigene App-Tasten und eine eigene Screen-Auswahl.

## 7 Bauen

```
./gradlew :app:assembleDebug
```

Benötigt JDK 17 oder neuer und das Android SDK.

## 8 Releases

APKs werden automatisch auf GitHub gebaut:

- Tag `v*` → signiertes Release-APK und App Bundle, veröffentlicht als GitHub-Release.
- Tag `devbuild` → signiertes APK als Build-Artefakt.
