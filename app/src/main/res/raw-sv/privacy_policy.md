## 1. Allmänt
Elly är en fjärrkontroll för LG-tv-apparater med webOS i det lokala nätverket.

Utvecklaren av den här appen samlar inte in, lagrar eller behandlar några personuppgifter. Appen kräver inget användarkonto.

## 2. Nätverkskommunikation
Appen kommunicerar uteslutande inom ditt lokala nätverk:

- Sökning efter tv-apparater: SSDP-förfrågan och en kontroll av adresserna i ditt lokala nätverk efter webOS-portarna 3000 och 3001.
- Styrning: krypterad anslutning till den parkopplade tv:n (äldre tv-apparater utan kryptering via port 3000). Appen skickar styrkommandon som knapptryck, pekarrörelser, appstarter och byte av ingång, och hämtar de installerade apparna, ingångarna och appikonerna.
- Påslagning: Wake-on-LAN-paket till tv:ns MAC-adress.

Kommunikationen sker direkt mellan din enhet och tv:n. Inga data överförs till externa servrar.

## 3. Datalagring
Följande data lagras lokalt på din enhet:

- Namn, modell, IP-adress och MAC-adress för parkopplade tv-apparater
- Parkopplingsnyckel och tv:ns certifikatfingeravtryck
- Appinställningar (skärmar, utseende, appknappar, pekare, vibration, demoläge)
- kopior av bilderna du väljer som skärmbakgrund; appen får bara de bilder du väljer i Androids bildväljare
- Appikoner som hämtats från tv:n

Dessa data stannar uteslutande på din enhet, undantas från Android-säkerhetskopior och raderas när appen avinstalleras.

## 4. Sensorer
I pekarens rörelseläge använder appen enhetens gyroskop. Sensordata behandlas bara medan pekarläget är aktivt och appen syns, omvandlas direkt till pekarrörelser och lagras inte.

## 5. Behörigheter
- Internet: anslutning till tv:n i det lokala nätverket (Android kräver den här behörigheten för alla nätverksanslutningar)
- Nätverks- och wifi-status: fastställa det lokala nätverket för sökningen
- Wifi-multicast: ta emot svaren vid sökning efter tv-apparater
- Vibration: kort återkoppling när en knapp på en hemskärmswidget trycks
- Förgrundstjänst: skicka en knapp som tryckts på en hemskärmswidget till tv:n, även när appen är i bakgrunden

Inga andra behörigheter används, i synnerhet ingen åtkomst till plats, mikrofon, kamera eller kontakter.

## 6. Tredje part
Appen använder inga tjänster från tredje part, inga analysverktyg och ingen reklam.

## 7. Ändringar i den här integritetspolicyn
Integritetspolicyn kan vid behov uppdateras för att återspegla ändringar i appen eller i lagkrav.

## 8. Kontakt
Om du har frågor om integritet kan du kontakta utvecklaren.

E-post: elly@robnice.de
