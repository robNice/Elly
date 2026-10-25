## 1. Generelt
Elly er en fjernbetjening til LG-tv med webOS på det lokale netværk.

Udvikleren af denne app indsamler, gemmer eller behandler ingen personoplysninger. Appen kræver ingen brugerkonto.

## 2. Netværkskommunikation
Appen kommunikerer udelukkende inden for dit lokale netværk:

- Søgning efter tv: SSDP-forespørgsel og en kontrol af adresserne i dit lokale netværk for webOS-portene 3000 og 3001.
- Styring: krypteret forbindelse til det parrede tv (ældre tv uden kryptering via port 3000). Appen sender styringskommandoer som knaptryk, markørbevægelser, app-start samt indgangs- og kanalskift og henter de installerede apps, indgangene og app-ikonerne.
- Tænding: Wake-on-LAN-pakke til tv'ets MAC-adresse.
- Programguide (valgfrit, kun for tv med rootadgang, som du selv konfigurerer): SSH-forbindelse til tv'et (port 22). Appen læser tv'ets favoritlister, kanaler, programguide samt planlagte påmindelser og optagelser, opretter eller sletter påmindelser og optagelser på tv'et og læser kanallogoer. Logoer, som tv'et endnu ikke har, henter tv'et selv fra LG's logoserver, ligesom det gør til sin egen programguide; din enhed kontakter ikke denne server.

Denne kommunikation foregår direkte mellem din enhed og tv'et. Der sendes ingen data til eksterne servere.

## 3. Datalagring
Følgende data gemmes lokalt på din enhed:

- Navn, model, IP-adresse og MAC-adresse på parrede tv
- Parringsnøgle og certifikatets fingeraftryk for tv'et
- Appindstillinger (skærme, design, app-knapper, markør, vibration, demotilstand)
- kopier af de billeder, du vælger som skærmbaggrund; appen modtager kun de billeder, du vælger i Androids fotovælger
- App-ikoner hentet fra tv'et
- for programguiden: SSH-bruger, port og fingeraftrykket på tv'ets SSH-nøgle; appens SSH-nøgle, som opbevares i Android Keystore og ikke kan læses ud af enheden; programguiden (kanallister og programmer fra de seneste 24 timer og op til 14 dage frem) og kanallogoer. Tv'ets rootadgangskode bruges kun én gang til at konfigurere adgangen og gemmes ikke.

Disse data forbliver udelukkende på din enhed, er udelukket fra Android-sikkerhedskopier og slettes, når appen afinstalleres.

## 4. Sensorer
I markørens bevægelsestilstand bruger appen din enheds gyroskop. Sensordataene behandles kun, mens markørtilstanden er aktiv og appen er synlig, omsættes direkte til markørbevægelser og gemmes ikke.

## 5. Tilladelser
- Internet: forbindelse til tv'et på det lokale netværk (Android kræver denne tilladelse til enhver netværksforbindelse)
- Netværks- og Wi-Fi-status: bestemmelse af det lokale netværk til søgningen
- Wi-Fi-multicast: modtagelse af svarene ved søgning efter tv
- Vibration: kort feedback, når der trykkes på en knap i en widget på startskærmen
- Forgrundstjeneste: afsendelse af et knaptryk fra en widget på startskærmen til tv'et, også mens appen kører i baggrunden

Der bruges ingen andre tilladelser, især ingen adgang til placering, mikrofon, kamera eller kontakter.

## 6. Tredjeparter
Denne app bruger ingen tjenester fra tredjeparter, analyseværktøjer eller reklamer.

## 7. Ændringer af denne privatlivspolitik
Denne privatlivspolitik kan om nødvendigt blive opdateret for at afspejle ændringer i appen eller lovkrav.

## 8. Kontakt
Hvis du har spørgsmål om privatliv, kan du kontakte udvikleren.

E-mail: elly@robnice.de
