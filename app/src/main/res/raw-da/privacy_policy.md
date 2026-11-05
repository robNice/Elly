## 1. Generelt
Elly er en fjernbetjening til LG-tv med webOS på det lokale netværk, på telefonen og på et Wear OS-ur.

Udvikleren af denne app indsamler, gemmer eller behandler ingen personoplysninger. Appen kræver ingen brugerkonto.

## 2. Netværkskommunikation
Appen kommunikerer med tv'et udelukkende inden for dit lokale netværk:

- Søgning efter tv: SSDP-forespørgsel og en kontrol af adresserne i dit lokale netværk for webOS-portene 3000 og 3001.
- Styring: krypteret forbindelse til det parrede tv (ældre tv uden kryptering via port 3000). Appen sender styringskommandoer som knaptryk, markørbevægelser, app-start samt indgangs- og kanalskift og henter de installerede apps, indgangene og app-ikonerne.
- Tænding: Wake-on-LAN-pakke til tv'ets MAC-adresse.
- Programguide (valgfrit, kun for tv med rootadgang, som du selv konfigurerer): SSH-forbindelse til tv'et (port 22). Appen læser tv'ets favoritlister, kanaler, programguide samt planlagte påmindelser og optagelser, opretter eller sletter påmindelser og optagelser på tv'et og læser kanallogoer. Logoer, som tv'et endnu ikke har, henter tv'et selv fra LG's logoserver, ligesom det gør til sin egen programguide; din enhed kontakter ikke denne server. Det samme gælder programbilleder fra LG's onlineprogramdata: Når du åbner detaljerne for et program, henter tv'et billedet fra LG's server og sender det videre; din enhed gemmer det kun i hukommelsen, mens appen kører.

Denne kommunikation foregår direkte mellem din enhed og tv'et. Der sendes ingen data til eksterne servere.

## 3. Ur (Wear OS)
Ligesom telefonen styrer ur-appen tv'et direkte via dit wi-fi (betjeningskommandoer, markørbevægelser, tænd via Wake-on-LAN, åbning af tv'ets kildevalg).

For at du ikke skal parre tv'et igen på uret, overfører telefonen følgende data til det parrede ur, så længe Elly er installeret der:

- navn, model, IP-adresse og MAC-adresse på parrede tv, deres parringsnøgle og certifikatfingeraftryk
- hvilket tv der er aktivt, og om demotilstanden er slået til
- sider og knaplayout i ur-appen og ikonerne for de apps, som urets knapper åbner

Overførslen bruger Wearable-grænsefladen i Google Play-tjenester, som forbinder telefonen og uret. Hvis telefonen og uret ikke er forbundet direkte via Bluetooth eller wi-fi, kan Google Play-tjenester overføre dataene krypteret mellem dine enheder via Googles servere. Så snart der ikke længere kendes et ur med Elly, sletter telefonen dataene fra denne grænseflade. Loginoplysninger til root-adgangen og programguiden overføres ikke til uret.

## 4. Datalagring
Følgende data gemmes lokalt på din enhed:

- Navn, model, IP-adresse og MAC-adresse på parrede tv
- Parringsnøgle og certifikatets fingeraftryk for tv'et
- Appindstillinger (skærme, design, app-knapper, markør, vibration, demotilstand, urets sider og knapper)
- kopier af de billeder, du vælger som skærmbaggrund; appen modtager kun de billeder, du vælger i Androids fotovælger
- App-ikoner hentet fra tv'et
- for programguiden: SSH-bruger, port og fingeraftrykket på tv'ets SSH-nøgle; tv'ets rootadgangskode eller den private SSH-nøgle, du har indtastet (med adgangsfrase), krypteret med en nøgle, der opbevares i Android Keystore og ikke kan læses ud af enheden; programguiden (kanallister og programmer fra de seneste 24 timer og op til 14 dage frem) og kanallogoer.

På uret gemmer Elly de data, der er overført fra telefonen (se afsnit 3), og urets indstillinger (vibration, cirkelbevægelse, lyseffekt, markør, valgt tv).

Disse data forbliver på dine enheder, er udelukket fra Android-sikkerhedskopier og slettes, når appen afinstalleres.

## 5. Sensorer
I markørens bevægelsestilstand bruger appen din enheds gyroskop. På uret bruger markørsiden urets gyroskop, mens en finger hviler på skærmen. Sensordataene behandles kun da, omsættes direkte til markørbevægelser og gemmes ikke.

## 6. Tilladelser
På telefonen:

- Internet: forbindelse til tv'et på det lokale netværk (Android kræver denne tilladelse til enhver netværksforbindelse)
- Netværks- og Wi-Fi-status: bestemmelse af det lokale netværk til søgningen
- Wi-Fi-multicast: modtagelse af svarene ved søgning efter tv
- Vibration: kort feedback, når der trykkes på en knap i en widget på startskærmen
- Forgrundstjeneste: afsendelse af et knaptryk fra en widget på startskærmen til tv'et, også mens appen kører i baggrunden

På uret:

- Internet og netværksstatus: forbindelse til tv'et på det lokale netværk
- Skift netværk: uret forbinder sig til dit wi-fi for at styre i stedet for at sende forbindelsen gennem telefonen
- Vibration: feedback på knapper og mens lydstyrken ændres
- Hold vågen: holder wi-fi hurtigt reagerende på markørsiden

Der bruges ingen andre tilladelser, især ingen adgang til placering, mikrofon, kamera eller kontakter.

## 7. Tredjeparter
Denne app bruger ingen analyseværktøjer eller reklamer. For at holde telefonen og uret synkroniseret bruger den Wearable-grænsefladen i Google Play-tjenester (se afsnit 3); Googles privatlivspolitik gælder også for den.

## 8. Ændringer af denne privatlivspolitik
Denne privatlivspolitik kan om nødvendigt blive opdateret for at afspejle ændringer i appen eller lovkrav.

## 9. Kontakt
Hvis du har spørgsmål om privatliv, kan du kontakte udvikleren.

E-mail: elly@robnice.de
