## 1. Allgemeines
Elgy dient als Fernbedienung für LG-Fernseher mit webOS im lokalen Netzwerk.

Der Entwickler dieser App erhebt, speichert oder verarbeitet keine personenbezogenen Daten. Die App benötigt kein Benutzerkonto.

## 2. Netzwerkkommunikation
Die App kommuniziert ausschließlich innerhalb Ihres lokalen Netzwerks:

- Suche nach Fernsehern: Anfrage per SSDP und Prüfung der Adressen Ihres lokalen Netzes auf die webOS-Ports 3000 und 3001.
- Steuerung: verschlüsselte Verbindung zum gekoppelten Fernseher (ältere Fernseher ohne Verschlüsselung über Port 3000). Übertragen werden Steuerbefehle wie Tasten, Zeigerbewegungen, App-Starts und Eingangswechsel sowie Abfragen der installierten Apps, der Eingänge und der App-Symbole.
- Einschalten: Wake-on-LAN-Paket an die MAC-Adresse des Fernsehers.

Diese Kommunikation erfolgt direkt zwischen Ihrem Gerät und dem Fernseher. Es werden keine Daten an externe Server übertragen.

## 3. Datenspeicherung
Folgende Daten werden lokal auf Ihrem Gerät gespeichert:

- Name, Modell, IP-Adresse und MAC-Adresse gekoppelter Fernseher
- Pairing-Schlüssel und Zertifikats-Fingerabdruck des Fernsehers
- Einstellungen der App (Screens, Design, App-Tasten, Zeiger, Vibration, Demo-Modus)
- Kopien der Bilder, die Sie als Screen-Hintergrund wählen; die App erhält nur die Bilder, die Sie in der Android-Fotoauswahl auswählen
- vom Fernseher geladene App-Symbole

Diese Daten verbleiben ausschließlich auf Ihrem Gerät, sind von der Android-Datensicherung ausgeschlossen und werden beim Deinstallieren der App gelöscht.

## 4. Sensoren
Im Bewegungsmodus des Zeigers nutzt die App das Gyroskop Ihres Geräts. Die Sensordaten werden nur verarbeitet, solange der Zeiger-Modus aktiv und die App sichtbar ist, direkt in Zeigerbewegungen umgerechnet und nicht gespeichert.

## 5. Berechtigungen
- Internet: Verbindung zum Fernseher im lokalen Netzwerk (Android verlangt diese Berechtigung für jede Netzwerkverbindung)
- Netzwerk- und WLAN-Status: Ermitteln des lokalen Netzes für die Suche
- WLAN-Multicast: Empfang der Antworten bei der Suche nach Fernsehern
- Vibration: kurze Rückmeldung beim Drücken einer Taste in einem Homescreen-Widget
- Vordergrunddienst: Senden einer Taste, die in einem Homescreen-Widget gedrückt wurde, an den TV, auch wenn die App im Hintergrund ist

Es werden keine weiteren Berechtigungen verwendet, insbesondere kein Zugriff auf Standort, Mikrofon, Kamera oder Kontakte.

## 6. Drittanbieter
Diese App verwendet keine Drittanbieter-Dienste, keine Analyse-Tools und keine Werbung.

## 7. Änderungen dieser Datenschutzerklärung
Diese Datenschutzerklärung kann bei Bedarf aktualisiert werden, um Änderungen der App oder rechtliche Anforderungen abzubilden.

## 8. Kontakt
Bei Fragen zum Datenschutz können Sie den Entwickler kontaktieren.

E-Mail: elgy@robnice.de
