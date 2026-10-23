## 1. Généralités
Elly est une télécommande pour les TV LG sous webOS sur le réseau local.

Le développeur de cette appli ne collecte, n'enregistre ni ne traite aucune donnée personnelle. L'appli ne nécessite pas de compte utilisateur.

## 2. Communication réseau
L'appli communique exclusivement au sein de ton réseau local :

- Recherche de TV : requête SSDP et vérification des adresses de ton réseau local pour les ports webOS 3000 et 3001.
- Commande : connexion chiffrée avec la TV associée (TV plus anciennes sans chiffrement via le port 3000). L'appli transmet des commandes telles que les touches, les mouvements du pointeur, le lancement d'applis et le changement de source, et demande les applis installées, les sources et les icônes des applis.
- Allumage : paquet Wake-on-LAN envoyé à l'adresse MAC de la TV.

Cette communication a lieu directement entre ton appareil et la TV. Aucune donnée n'est transmise à des serveurs externes.

## 3. Stockage des données
Les données suivantes sont enregistrées localement sur ton appareil :

- Nom, modèle, adresse IP et adresse MAC des TV associées
- Clé d'association et empreinte du certificat de la TV
- Paramètres de l'appli (écrans, design, touches appli, pointeur, vibration, mode démo)
- copies des images que tu choisis comme arrière-plans d'écran ; l'appli ne reçoit que les images que tu sélectionnes dans le sélecteur de photos d'Android
- Icônes d'applis chargées depuis la TV

Ces données restent exclusivement sur ton appareil, sont exclues des sauvegardes Android et sont supprimées lors de la désinstallation de l'appli.

## 4. Capteurs
En mode mouvement du pointeur, l'appli utilise le gyroscope de ton appareil. Les données du capteur ne sont traitées que lorsque le mode pointeur est actif et que l'appli est visible ; elles sont converties directement en mouvements du pointeur et ne sont pas enregistrées.

## 5. Autorisations
- Internet : connexion à la TV sur le réseau local (Android exige cette autorisation pour toute connexion réseau)
- État du réseau et du Wi-Fi : identification du réseau local pour la recherche
- Multicast Wi-Fi : réception des réponses lors de la recherche de TV
- Vibration : bref retour lors de l'appui sur une touche d'un widget de l'écran d'accueil
- Service de premier plan : envoi à la TV d'une touche appuyée sur un widget de l'écran d'accueil, même lorsque l'appli est en arrière-plan

Aucune autre autorisation n'est utilisée, en particulier aucun accès à la localisation, au micro, à l'appareil photo ou aux contacts.

## 6. Tiers
Cette appli n'utilise aucun service tiers, aucun outil d'analyse ni aucune publicité.

## 7. Modifications de cette politique de confidentialité
Cette politique de confidentialité peut être mise à jour si nécessaire pour tenir compte de modifications de l'appli ou d'exigences légales.

## 8. Contact
Pour toute question concernant la confidentialité, tu peux contacter le développeur.

E-mail : elly@robnice.de
