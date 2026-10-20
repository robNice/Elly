# Elgy

Elgy macht dein Android-Handy zur Fernbedienung für LG-Fernseher mit webOS. Die App bildet alle Tasten und Funktionen der originalen LG Magic Remote nach – einschließlich des Zeigers, der sich über die Bewegungssensoren des Handys steuern lässt.

> **Status:** frühe Entwicklung – das Projektgerüst steht, die Fernbedienungsfunktionen sind noch nicht umgesetzt.

[English version](README.md)

## Inhalt

1. [Funktionen](#1-funktionen)
2. [Voraussetzungen](#2-voraussetzungen)
3. [Verbindung zum TV](#3-verbindung-zum-tv)
4. [Bauen](#4-bauen)
5. [Releases](#5-releases)

## 1 Funktionen

- Alle Tasten der LG Magic Remote: Ein/Aus, Zahlen, Lautstärke, Kanäle, Steuerkreuz mit OK, Home, Einstellungen, Zurück, Guide, Input, Netflix, Prime Video, Movies, Farbtasten, Play/Pause und mehr.
- Die Tasten sind auf mehrere Screens verteilt, sodass alles ohne Scrollen sichtbar ist. Gewechselt wird per Swipe.
- Zeigersteuerung über das Gyroskop des Handys: per eigener Taste ein- und ausschaltbar, schaltet sich nach 5 Sekunden ohne Bewegung automatisch ab.
- Dunkles Design mit realistisch wirkenden, dreidimensionalen Tasten.
- Verfügbar auf Deutsch und Englisch.

## 2 Voraussetzungen

- Android 9 (API 28) oder neuer.
- LG-Fernseher mit webOS im selben Netzwerk wie das Handy.

## 3 Verbindung zum TV

Elgy kommuniziert mit dem Fernseher über das lokale Netzwerk (WLAN/LAN). Beim ersten Verbinden fragt der TV, ob der Zugriff erlaubt werden soll. Zum Einschalten nutzt Elgy Wake-on-LAN – dafür muss am TV die entsprechende Option aktiviert sein (z. B. „Über WLAN einschalten“ / „Mobile TV On“).

## 4 Bauen

```
./gradlew :app:assembleDebug
```

Benötigt JDK 17 oder neuer und das Android SDK.

## 5 Releases

APKs werden automatisch auf GitHub gebaut:

- Tag `v*` → signiertes Release-APK und App Bundle, veröffentlicht als GitHub-Release.
- Tag `devbuild` → signiertes APK als Build-Artefakt.
