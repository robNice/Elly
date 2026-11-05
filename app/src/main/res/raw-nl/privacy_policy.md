## 1. Algemeen
Elly is een afstandsbediening voor LG-tv's met webOS in het lokale netwerk, op de telefoon en op een Wear OS-horloge.

De ontwikkelaar van deze app verzamelt, bewaart of verwerkt geen persoonsgegevens. De app vereist geen gebruikersaccount.

## 2. Netwerkcommunicatie
De app communiceert met de tv uitsluitend binnen je lokale netwerk:

- Tv's zoeken: SSDP-verzoek en een controle van de adressen in je lokale netwerk op de webOS-poorten 3000 en 3001.
- Bediening: versleutelde verbinding met de gekoppelde tv (oudere tv's zonder versleuteling via poort 3000). De app verstuurt bedieningsopdrachten zoals toetsen, aanwijzerbewegingen, het starten van apps en het wisselen van bron, en vraagt de geïnstalleerde apps, de bronnen en de app-pictogrammen op.
- Inschakelen: Wake-on-LAN-pakket naar het MAC-adres van de tv.
- Programmagids (optioneel, alleen voor tv's met roottoegang die je zelf instelt): SSH-verbinding met de tv (poort 22). De app leest favorietenlijsten, zenders, programmagegevens en geplande herinneringen en opnames van de tv, plant of verwijdert herinneringen en opnames op de tv en leest zenderlogo's. Logo's die de tv nog niet heeft, downloadt de tv zelf van de logoserver van LG, zoals voor zijn eigen gids; je apparaat maakt daarbij geen verbinding met die server. Hetzelfde geldt voor programma-afbeeldingen uit de online gidsgegevens van LG: als je de details van een programma opent, downloadt de tv de afbeelding van de server van LG en geeft die door; je apparaat bewaart haar alleen in het geheugen zolang de app draait.

Deze communicatie vindt rechtstreeks plaats tussen je apparaat en de tv. Er worden geen gegevens naar externe servers verzonden.

## 3. Horloge (Wear OS)
Net als de telefoon bedient de horloge-app de tv rechtstreeks via je wifi (bedieningsopdrachten, aanwijzerbewegingen, inschakelen via Wake-on-LAN, openen van de bronkeuze van de tv).

Zodat je de tv niet opnieuw hoeft te koppelen op het horloge, draagt de telefoon de volgende gegevens over aan het gekoppelde horloge zolang Elly daarop is geïnstalleerd:

- naam, model, IP-adres en MAC-adres van gekoppelde tv's, hun koppelsleutel en certificaatvingerafdruk
- welke tv actief is en of de demomodus aan staat
- de pagina's en toetsindeling van de horloge-app en de pictogrammen van de apps die horlogetoetsen openen

Voor de overdracht wordt de Wearable-interface van Google Play-services gebruikt, die de telefoon en het horloge verbindt. Als de telefoon en het horloge niet rechtstreeks via bluetooth of wifi verbonden zijn, kunnen Google Play-services de gegevens versleuteld via de servers van Google tussen je apparaten overdragen. Zodra er geen horloge met Elly meer bekend is, verwijdert de telefoon de gegevens uit deze interface. Inloggegevens voor de root-toegang en de programmagids worden niet naar het horloge overgedragen.

## 4. Gegevensopslag
De volgende gegevens worden lokaal op je apparaat opgeslagen:

- Naam, model, IP-adres en MAC-adres van gekoppelde tv's
- Koppelingssleutel en certificaatvingerafdruk van de tv
- App-instellingen (schermen, vormgeving, app-toetsen, aanwijzer, trillen, demomodus, pagina's en toetsen van het horloge)
- kopieën van de afbeeldingen die je als schermachtergrond kiest; de app ontvangt alleen de afbeeldingen die je in de Android-fotokiezer selecteert
- App-pictogrammen die van de tv zijn geladen
- voor de programmagids: SSH-gebruiker, poort en vingerafdruk van de SSH-sleutel van de tv; het rootwachtwoord van de tv of de privé-SSH-sleutel die je hebt ingevoerd (met wachtwoordzin), versleuteld met een sleutel die in de Android Keystore ligt en niet uit het apparaat kan worden gelezen; de programmagegevens (zenderlijsten en programma's van de afgelopen 24 uur en tot 14 dagen vooruit) en zenderlogo's.

Op het horloge slaat Elly de van de telefoon overgedragen gegevens op (zie paragraaf 3) en de instellingen van het horloge (trillen, cirkelgebaar, lichteffect, aanwijzer, gekozen tv).

Deze gegevens blijven op je apparaten, worden uitgesloten van Android-back-ups en worden verwijderd als de app wordt verwijderd.

## 5. Sensoren
In de bewegingsmodus van de aanwijzer gebruikt de app de gyroscoop van je apparaat. Op het horloge gebruikt de aanwijzerpagina de gyroscoop van het horloge zolang er een vinger op het scherm ligt. De sensorgegevens worden alleen dan verwerkt, direct omgezet in aanwijzerbewegingen en niet opgeslagen.

## 6. Machtigingen
Op de telefoon:

- Internet: verbinding met de tv in het lokale netwerk (Android vereist deze machtiging voor elke netwerkverbinding)
- Netwerk- en wifistatus: het lokale netwerk bepalen voor het zoeken
- Wifi-multicast: de antwoorden ontvangen bij het zoeken naar tv's
- Trillen: korte feedback als er een toets op een widget op het startscherm wordt ingedrukt
- Voorgrondservice: een toets die op een widget op het startscherm is ingedrukt naar de tv sturen, ook als de app op de achtergrond draait

Op het horloge:

- Internet en netwerkstatus: verbinding met de tv in het lokale netwerk
- Netwerk wijzigen: het horloge maakt voor de bediening verbinding met je wifi in plaats van de verbinding via de telefoon te laten lopen
- Trillen: feedback bij toetsen en terwijl het volume verandert
- Wakker houden: houdt wifi snel reagerend op de aanwijzerpagina

Er worden geen andere machtigingen gebruikt, in het bijzonder geen toegang tot locatie, microfoon, camera of contacten.

## 7. Derden
Deze app gebruikt geen analysetools of advertenties. Om de telefoon en het horloge te synchroniseren gebruikt hij de Wearable-interface van Google Play-services (zie paragraaf 3); daarvoor geldt ook het privacybeleid van Google.

## 8. Wijzigingen in deze privacyverklaring
Deze privacyverklaring kan indien nodig worden bijgewerkt om wijzigingen in de app of in wettelijke vereisten te weerspiegelen.

## 9. Contact
Heb je vragen over privacy, dan kun je contact opnemen met de ontwikkelaar.

E-mail: elly@robnice.de
