## 1. Informazioni generali
Elly è un telecomando per TV LG con webOS nella rete locale, sul telefono e su un orologio Wear OS.

Lo sviluppatore di questa app non raccoglie, memorizza né elabora alcun dato personale. L'app non richiede un account utente.

## 2. Comunicazione di rete
L'app comunica con la TV esclusivamente all'interno della tua rete locale:

- Ricerca delle TV: richiesta SSDP e verifica degli indirizzi della tua rete locale sulle porte webOS 3000 e 3001.
- Controllo: connessione cifrata con la TV associata (le TV più vecchie senza cifratura tramite la porta 3000). L'app trasmette comandi di controllo come tasti, movimenti del puntatore, avvii di app e cambi di ingresso e di canale, e richiede le app installate, gli ingressi e le icone delle app.
- Accensione: pacchetto Wake-on-LAN all'indirizzo MAC della TV.
- Guida programmi (facoltativa, solo per TV con accesso root configurato da te): connessione SSH alla TV (porta 22). L'app legge gli elenchi dei preferiti, i canali, la guida programmi e i promemoria e le registrazioni pianificati sulla TV, pianifica o elimina promemoria e registrazioni sulla TV e legge i loghi dei canali. I loghi che la TV non possiede ancora vengono scaricati dalla TV stessa dal server dei loghi di LG, come avviene per la sua guida; il tuo dispositivo non contatta tale server. Lo stesso vale per le immagini dei programmi provenienti dai dati online della guida di LG: quando apri i dettagli di un programma, la TV scarica la sua immagine dal server di LG e la inoltra; il tuo dispositivo la conserva solo in memoria finché l'app è in esecuzione.

Questa comunicazione avviene direttamente tra il tuo dispositivo e la TV. Nessun dato viene trasmesso a server esterni.

## 3. Orologio (Wear OS)
Come il telefono, l'app dell'orologio controlla la TV direttamente tramite il tuo Wi-Fi (comandi, movimenti del puntatore, accensione tramite Wake-on-LAN, apertura della scelta della sorgente della TV).

Per evitare di dover associare di nuovo la TV sull'orologio, il telefono trasferisce i seguenti dati all'orologio associato finché Elly vi è installata:

- nome, modello, indirizzo IP e indirizzo MAC delle TV associate, la loro chiave di associazione e l'impronta del certificato
- quale TV è attiva e se la modalità demo è attiva
- le pagine e la disposizione dei tasti dell'app dell'orologio e le icone delle app che i tasti dell'orologio aprono

Il trasferimento utilizza l'interfaccia Wearable dei servizi Google Play, che collega il telefono e l'orologio. Se il telefono e l'orologio non sono collegati direttamente tramite Bluetooth o Wi-Fi, i servizi Google Play possono trasferire i dati tra i tuoi dispositivi in forma crittografata tramite i server di Google. Non appena non è più noto alcun orologio con Elly, il telefono elimina i dati da questa interfaccia. Le credenziali per l'accesso root e la guida TV non vengono trasferite all'orologio.

## 4. Memorizzazione dei dati
I seguenti dati vengono memorizzati localmente sul tuo dispositivo:

- Nome, modello, indirizzo IP e indirizzo MAC delle TV associate
- Chiave di associazione e impronta digitale del certificato della TV
- Impostazioni dell'app (schermate, design, tasti app, puntatore, vibrazione, modalità demo, pagine e tasti dell'orologio)
- copie delle immagini che scegli come sfondo delle schermate; l'app riceve solo le immagini che selezioni nel selettore di foto di Android
- Icone delle app caricate dalla TV
- per la guida programmi: utente SSH, porta e impronta della chiave SSH della TV; la password di root della TV o la chiave SSH privata che hai inserito (con la relativa passphrase), crittografate con una chiave conservata in Android Keystore e non estraibile dal dispositivo; la guida programmi (elenchi dei canali e programmi delle ultime 24 ore e fino a 14 giorni successivi) e i loghi dei canali.

Sull'orologio, Elly memorizza i dati trasferiti dal telefono (vedi la sezione 3) e le impostazioni dell'orologio (vibrazione, gesto circolare, effetto luce, puntatore, TV scelta).

Questi dati restano sui tuoi dispositivi, sono esclusi dai backup di Android e vengono eliminati quando l'app viene disinstallata.

## 5. Sensori
Nella modalità movimento del puntatore, l'app utilizza il giroscopio del tuo dispositivo. Sull'orologio, la pagina del puntatore utilizza il giroscopio dell'orologio mentre un dito è appoggiato sul display. I dati del sensore vengono elaborati solo in quel momento, convertiti direttamente in movimenti del puntatore e non memorizzati.

## 6. Autorizzazioni
Sul telefono:

- Internet: connessione alla TV nella rete locale (Android richiede questa autorizzazione per qualsiasi connessione di rete)
- Stato della rete e del Wi-Fi: individuazione della rete locale per la ricerca
- Multicast Wi-Fi: ricezione delle risposte durante la ricerca delle TV
- Vibrazione: breve riscontro quando viene premuto un tasto di un widget della schermata home
- Servizio in primo piano: invio alla TV di un tasto premuto su un widget della schermata home, anche quando l'app è in background

Sull'orologio:

- Internet e stato della rete: connessione alla TV nella rete locale
- Modifica della rete: l'orologio si collega al tuo Wi-Fi per il controllo invece di far passare la connessione dal telefono
- Vibrazione: riscontro sui tasti e mentre il volume cambia
- Mantenere attivo: mantiene il Wi-Fi reattivo nella pagina del puntatore

Non vengono utilizzate altre autorizzazioni, in particolare nessun accesso a posizione, microfono, fotocamera o contatti.

## 7. Terze parti
Questa app non utilizza strumenti di analisi né pubblicità. Per sincronizzare il telefono e l'orologio utilizza l'interfaccia Wearable dei servizi Google Play (vedi la sezione 3); anche per essa si applica l'informativa sulla privacy di Google.

## 8. Modifiche a questa informativa sulla privacy
Questa informativa sulla privacy può essere aggiornata, se necessario, per riflettere modifiche all'app o ai requisiti di legge.

## 9. Contatti
Per qualsiasi domanda sulla privacy, puoi contattare lo sviluppatore.

E-mail: elly@robnice.de
