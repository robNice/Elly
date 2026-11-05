## 1. Allmänt
Elly är en fjärrkontroll för LG-tv-apparater med webOS i det lokala nätverket, på telefonen och på en Wear OS-klocka.

Utvecklaren av den här appen samlar inte in, lagrar eller behandlar några personuppgifter. Appen kräver inget användarkonto.

## 2. Nätverkskommunikation
Appen kommunicerar med tv:n uteslutande inom ditt lokala nätverk:

- Sökning efter tv-apparater: SSDP-förfrågan och en kontroll av adresserna i ditt lokala nätverk efter webOS-portarna 3000 och 3001.
- Styrning: krypterad anslutning till den parkopplade tv:n (äldre tv-apparater utan kryptering via port 3000). Appen skickar styrkommandon som knapptryck, pekarrörelser, appstarter och byte av ingång, och hämtar de installerade apparna, ingångarna och appikonerna.
- Påslagning: Wake-on-LAN-paket till tv:ns MAC-adress.
- Programguide (valfritt, bara för tv-apparater med root-åtkomst som du själv konfigurerar): SSH-anslutning till tv:n (port 22). Appen läser tv:ns favoritlistor, kanaler, programdata och planerade påminnelser och inspelningar, planerar eller tar bort påminnelser och inspelningar på tv:n och läser kanallogotyper. Logotyper som tv:n ännu inte har laddar tv:n själv ned från LG:s logotypserver, precis som för sin egen guide; din enhet ansluter inte till den servern. Detsamma gäller programbilder från LG:s onlineprogramdata: när du öppnar detaljerna för ett program laddar tv:n ned bilden från LG:s server och skickar den vidare; din enhet sparar den bara i minnet medan appen körs.

Kommunikationen sker direkt mellan din enhet och tv:n. Inga data överförs till externa servrar.

## 3. Klocka (Wear OS)
Precis som telefonen styr klockappen tv:n direkt via ditt wifi (styrkommandon, pekarrörelser, påslagning via Wake-on-LAN, öppning av tv:ns källval).

För att du inte ska behöva parkoppla tv:n igen på klockan överför telefonen följande data till den parkopplade klockan så länge Elly är installerad där:

- namn, modell, IP-adress och MAC-adress för parkopplade tv-apparater, deras parkopplingsnyckel och certifikatets fingeravtryck
- vilken tv som är aktiv och om demoläget är på
- sidor och knapplayout i klockappen och ikonerna för de appar som klockans knappar öppnar

Överföringen använder Wearable-gränssnittet i Google Play-tjänster, som förbinder telefonen och klockan. Om telefonen och klockan inte är direkt anslutna via Bluetooth eller wifi kan Google Play-tjänster överföra data krypterat mellan dina enheter via Googles servrar. Så snart ingen klocka med Elly längre är känd raderar telefonen data från detta gränssnitt. Inloggningsuppgifter för root-åtkomsten och programguiden överförs inte till klockan.

## 4. Datalagring
Följande data lagras lokalt på din enhet:

- Namn, modell, IP-adress och MAC-adress för parkopplade tv-apparater
- Parkopplingsnyckel och tv:ns certifikatfingeravtryck
- Appinställningar (skärmar, utseende, appknappar, pekare, vibration, demoläge, klockans sidor och knappar)
- kopior av bilderna du väljer som skärmbakgrund; appen får bara de bilder du väljer i Androids bildväljare
- Appikoner som hämtats från tv:n
- för programguiden: SSH-användare, port och fingeravtryck för tv:ns SSH-nyckel; tv:ns root-lösenord eller den privata SSH-nyckel du har angett (med lösenfras), krypterade med en nyckel som ligger i Android Keystore och inte kan läsas ut ur enheten; programdata (kanallistor och program från de senaste 24 timmarna och upp till 14 dagar framåt) och kanallogotyper.

På klockan sparar Elly de data som överförts från telefonen (se avsnitt 3) och klockans inställningar (vibration, cirkelgest, ljuseffekt, pekare, vald tv).

Dessa data stannar på dina enheter, undantas från Android-säkerhetskopior och raderas när appen avinstalleras.

## 5. Sensorer
I pekarens rörelseläge använder appen enhetens gyroskop. På klockan använder pekarsidan klockans gyroskop medan ett finger vilar på skärmen. Sensordata behandlas bara då, omvandlas direkt till pekarrörelser och sparas inte.

## 6. Behörigheter
På telefonen:

- Internet: anslutning till tv:n i det lokala nätverket (Android kräver den här behörigheten för alla nätverksanslutningar)
- Nätverks- och wifi-status: fastställa det lokala nätverket för sökningen
- Wifi-multicast: ta emot svaren vid sökning efter tv-apparater
- Vibration: kort återkoppling när en knapp på en hemskärmswidget trycks
- Förgrundstjänst: skicka en knapp som tryckts på en hemskärmswidget till tv:n, även när appen är i bakgrunden

På klockan:

- Internet och nätverksstatus: anslutning till tv:n i det lokala nätverket
- Ändra nätverk: klockan ansluter till ditt wifi för styrningen i stället för att leda anslutningen via telefonen
- Vibration: återkoppling vid knappar och medan volymen ändras
- Håll vaken: håller wifi snabbt på pekarsidan

Inga andra behörigheter används, i synnerhet ingen åtkomst till plats, mikrofon, kamera eller kontakter.

## 7. Tredje part
Appen använder inga analysverktyg och ingen reklam. För att hålla telefonen och klockan synkroniserade använder den Wearable-gränssnittet i Google Play-tjänster (se avsnitt 3); Googles integritetspolicy gäller även för det.

## 8. Ändringar i den här integritetspolicyn
Integritetspolicyn kan vid behov uppdateras för att återspegla ändringar i appen eller i lagkrav.

## 9. Kontakt
Om du har frågor om integritet kan du kontakta utvecklaren.

E-post: elly@robnice.de
