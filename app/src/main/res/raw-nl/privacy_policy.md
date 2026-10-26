## 1. Algemeen
Elly is een afstandsbediening voor LG-tv's met webOS in het lokale netwerk.

De ontwikkelaar van deze app verzamelt, bewaart of verwerkt geen persoonsgegevens. De app vereist geen gebruikersaccount.

## 2. Netwerkcommunicatie
De app communiceert uitsluitend binnen je lokale netwerk:

- Tv's zoeken: SSDP-verzoek en een controle van de adressen in je lokale netwerk op de webOS-poorten 3000 en 3001.
- Bediening: versleutelde verbinding met de gekoppelde tv (oudere tv's zonder versleuteling via poort 3000). De app verstuurt bedieningsopdrachten zoals toetsen, aanwijzerbewegingen, het starten van apps en het wisselen van bron, en vraagt de geïnstalleerde apps, de bronnen en de app-pictogrammen op.
- Inschakelen: Wake-on-LAN-pakket naar het MAC-adres van de tv.
- Programmagids (optioneel, alleen voor tv's met roottoegang die je zelf instelt): SSH-verbinding met de tv (poort 22). De app leest favorietenlijsten, zenders, programmagegevens en geplande herinneringen en opnames van de tv, plant of verwijdert herinneringen en opnames op de tv en leest zenderlogo's. Logo's die de tv nog niet heeft, downloadt de tv zelf van de logoserver van LG, zoals voor zijn eigen gids; je apparaat maakt daarbij geen verbinding met die server.

Deze communicatie vindt rechtstreeks plaats tussen je apparaat en de tv. Er worden geen gegevens naar externe servers verzonden.

## 3. Gegevensopslag
De volgende gegevens worden lokaal op je apparaat opgeslagen:

- Naam, model, IP-adres en MAC-adres van gekoppelde tv's
- Koppelingssleutel en certificaatvingerafdruk van de tv
- App-instellingen (schermen, vormgeving, app-toetsen, aanwijzer, trillen, demomodus)
- kopieën van de afbeeldingen die je als schermachtergrond kiest; de app ontvangt alleen de afbeeldingen die je in de Android-fotokiezer selecteert
- App-pictogrammen die van de tv zijn geladen
- voor de programmagids: SSH-gebruiker, poort en vingerafdruk van de SSH-sleutel van de tv; het rootwachtwoord van de tv of de privé-SSH-sleutel die je hebt ingevoerd (met wachtwoordzin), versleuteld met een sleutel die in de Android Keystore ligt en niet uit het apparaat kan worden gelezen; de programmagegevens (zenderlijsten en programma's van de afgelopen 24 uur en tot 14 dagen vooruit) en zenderlogo's.

Deze gegevens blijven uitsluitend op je apparaat, worden uitgesloten van Android-back-ups en worden verwijderd als de app wordt verwijderd.

## 4. Sensoren
In de bewegingsmodus van de aanwijzer gebruikt de app de gyroscoop van je apparaat. De sensorgegevens worden alleen verwerkt terwijl de aanwijzermodus actief is en de app zichtbaar is, direct omgezet in aanwijzerbewegingen en niet opgeslagen.

## 5. Machtigingen
- Internet: verbinding met de tv in het lokale netwerk (Android vereist deze machtiging voor elke netwerkverbinding)
- Netwerk- en wifistatus: het lokale netwerk bepalen voor het zoeken
- Wifi-multicast: de antwoorden ontvangen bij het zoeken naar tv's
- Trillen: korte feedback als er een toets op een widget op het startscherm wordt ingedrukt
- Voorgrondservice: een toets die op een widget op het startscherm is ingedrukt naar de tv sturen, ook als de app op de achtergrond draait

Er worden geen andere machtigingen gebruikt, in het bijzonder geen toegang tot locatie, microfoon, camera of contacten.

## 6. Derden
Deze app gebruikt geen diensten van derden, analysetools of advertenties.

## 7. Wijzigingen in deze privacyverklaring
Deze privacyverklaring kan indien nodig worden bijgewerkt om wijzigingen in de app of in wettelijke vereisten te weerspiegelen.

## 8. Contact
Heb je vragen over privacy, dan kun je contact opnemen met de ontwikkelaar.

E-mail: elly@robnice.de
