## 1. Généralités
Elly est une télécommande pour les TV LG sous webOS sur le réseau local, sur le téléphone et sur une montre Wear OS.

Le développeur de cette appli ne collecte, n'enregistre ni ne traite aucune donnée personnelle. L'appli ne nécessite pas de compte utilisateur.

## 2. Communication réseau
L'appli communique avec la TV exclusivement au sein de ton réseau local :

- Recherche de TV : requête SSDP et vérification des adresses de ton réseau local pour les ports webOS 3000 et 3001.
- Commande : connexion chiffrée avec la TV associée (TV plus anciennes sans chiffrement via le port 3000). L'appli transmet des commandes telles que les touches, les mouvements du pointeur, le lancement d'applis et les changements de source et de chaîne, et demande les applis installées, les sources et les icônes des applis.
- Allumage : paquet Wake-on-LAN envoyé à l'adresse MAC de la TV.
- Guide des programmes (facultatif, uniquement pour les TV avec un accès root que tu configures toi-même) : connexion SSH à la TV (port 22). L'appli lit les listes de favoris, les chaînes, le guide des programmes ainsi que les rappels et enregistrements programmés sur la TV, programme ou supprime des rappels et des enregistrements sur la TV et lit les logos des chaînes. Les logos que la TV ne possède pas encore sont téléchargés par la TV elle-même depuis le serveur de logos de LG, comme elle le fait pour son propre guide ; ton appareil ne contacte pas ce serveur. Il en va de même pour les images des programmes issues des données en ligne du guide de LG : lorsque tu ouvres les détails d'un programme, la TV télécharge son image depuis le serveur de LG et la transmet ; ton appareil ne la conserve qu'en mémoire tant que l'appli est ouverte.

Cette communication a lieu directement entre ton appareil et la TV. Aucune donnée n'est transmise à des serveurs externes.

## 3. Montre (Wear OS)
Comme le téléphone, l'appli de la montre commande la TV directement via ton Wi-Fi (commandes, mouvements du pointeur, allumage par Wake-on-LAN, ouverture du choix de la source de la TV).

Pour que tu n'aies pas à associer la TV une seconde fois sur la montre, le téléphone transmet les données suivantes à la montre associée tant qu'Elly y est installée :

- nom, modèle, adresse IP et adresse MAC des TV associées, leur clé d'association et l'empreinte de leur certificat
- quelle TV est active et si le mode démo est activé
- les pages et la disposition des touches de l'appli de la montre ainsi que les icônes des applis que les touches de la montre ouvrent

La transmission utilise l'interface Wearable des services Google Play, qui relie le téléphone et la montre. Si le téléphone et la montre ne sont pas directement connectés par Bluetooth ou Wi-Fi, les services Google Play peuvent transmettre les données entre tes appareils sous forme chiffrée via les serveurs de Google. Dès qu'aucune montre avec Elly n'est plus connue, le téléphone supprime les données de cette interface. Les identifiants de l'accès root et le guide des programmes ne sont pas transmis à la montre.

## 4. Stockage des données
Les données suivantes sont enregistrées localement sur ton appareil :

- Nom, modèle, adresse IP et adresse MAC des TV associées
- Clé d'association et empreinte du certificat de la TV
- Paramètres de l'appli (écrans, design, touches appli, pointeur, vibration, mode démo, pages et touches de la montre)
- copies des images que tu choisis comme arrière-plans d'écran ; l'appli ne reçoit que les images que tu sélectionnes dans le sélecteur de photos d'Android
- Icônes d'applis chargées depuis la TV
- pour le guide des programmes : utilisateur SSH, port et empreinte de la clé SSH de la TV ; mot de passe root de la TV ou clé SSH privée que tu as saisie (avec sa phrase secrète), chiffrés avec une clé conservée dans Android Keystore et impossible à extraire de l'appareil ; guide des programmes (listes de chaînes et programmes des dernières 24 heures et jusqu'à 14 jours à l'avance) et logos des chaînes.

Sur la montre, Elly enregistre les données transmises par le téléphone (voir la section 3) et les réglages de la montre (vibration, geste circulaire, effet de lumière, pointeur, TV choisie).

Ces données restent sur tes appareils, sont exclues des sauvegardes Android et sont supprimées lors de la désinstallation de l'appli.

## 5. Capteurs
En mode mouvement du pointeur, l'appli utilise le gyroscope de ton appareil. Sur la montre, la page du pointeur utilise le gyroscope de la montre tant qu'un doigt est posé sur l'écran. Les données du capteur ne sont traitées qu'à ce moment-là, converties directement en mouvements du pointeur et ne sont pas enregistrées.

## 6. Autorisations
Sur le téléphone :

- Internet : connexion à la TV sur le réseau local (Android exige cette autorisation pour toute connexion réseau)
- État du réseau et du Wi-Fi : identification du réseau local pour la recherche
- Multicast Wi-Fi : réception des réponses lors de la recherche de TV
- Vibration : bref retour lors de l'appui sur une touche d'un widget de l'écran d'accueil
- Service de premier plan : envoi à la TV d'une touche appuyée sur un widget de l'écran d'accueil, même lorsque l'appli est en arrière-plan

Sur la montre :

- Internet et état du réseau : connexion à la TV sur le réseau local
- Modifier le réseau : la montre se connecte à ton Wi-Fi pour la commande au lieu de faire passer la connexion par le téléphone
- Vibration : retour sur les touches et pendant que le volume change
- Maintenir l'appareil éveillé : garde le Wi-Fi réactif sur la page du pointeur

Aucune autre autorisation n'est utilisée, en particulier aucun accès à la localisation, au micro, à l'appareil photo ou aux contacts.

## 7. Tiers
Cette appli n'utilise ni outils d'analyse ni publicité. Pour synchroniser le téléphone et la montre, elle utilise l'interface Wearable des services Google Play (voir la section 3) ; la politique de confidentialité de Google s'applique également à celle-ci.

## 8. Modifications de cette politique de confidentialité
Cette politique de confidentialité peut être mise à jour si nécessaire pour tenir compte de modifications de l'appli ou d'exigences légales.

## 9. Contact
Pour toute question concernant la confidentialité, tu peux contacter le développeur.

E-mail : elly@robnice.de
