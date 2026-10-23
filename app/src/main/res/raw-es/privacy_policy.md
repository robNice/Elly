## 1. General
Elly es un mando a distancia para televisores LG con webOS en la red local.

El desarrollador de esta app no recopila, almacena ni procesa ningún dato personal. La app no requiere una cuenta de usuario.

## 2. Comunicación de red
La app se comunica exclusivamente dentro de tu red local:

- Búsqueda de televisores: solicitud SSDP y comprobación de las direcciones de tu red local en los puertos de webOS 3000 y 3001.
- Control: conexión cifrada con el televisor vinculado (los televisores más antiguos, sin cifrado, a través del puerto 3000). La app transmite comandos de control como teclas, movimientos del puntero, inicios de apps y cambios de entrada, y solicita las apps instaladas, las entradas y los iconos de las apps.
- Encendido: paquete Wake-on-LAN a la dirección MAC del televisor.

Esta comunicación se realiza directamente entre tu dispositivo y el televisor. No se transmite ningún dato a servidores externos.

## 3. Almacenamiento de datos
Los siguientes datos se almacenan localmente en tu dispositivo:

- Nombre, modelo, dirección IP y dirección MAC de los televisores vinculados
- Clave de vinculación y huella digital del certificado del televisor
- Ajustes de la app (pantallas, diseño, teclas de app, puntero, vibración, modo demo)
- copias de las imágenes que eliges como fondo de pantalla; la app solo recibe las imágenes que seleccionas en el selector de fotos de Android
- Iconos de apps cargados desde el televisor

Estos datos permanecen exclusivamente en tu dispositivo, se excluyen de las copias de seguridad de Android y se eliminan al desinstalar la app.

## 4. Sensores
En el modo movimiento del puntero, la app utiliza el giroscopio de tu dispositivo. Los datos del sensor solo se procesan mientras el modo puntero está activo y la app está visible, se convierten directamente en movimientos del puntero y no se almacenan.

## 5. Permisos
- Internet: conexión con el televisor en la red local (Android requiere este permiso para cualquier conexión de red)
- Estado de la red y del wifi: determinar la red local para la búsqueda
- Multidifusión wifi: recibir las respuestas al buscar televisores
- Vibración: respuesta breve al pulsar una tecla de un widget de la pantalla de inicio
- Servicio en primer plano: enviar al televisor una tecla pulsada en un widget de la pantalla de inicio, también mientras la app está en segundo plano

No se utiliza ningún otro permiso; en particular, no hay acceso a la ubicación, el micrófono, la cámara ni los contactos.

## 6. Terceros
Esta app no utiliza servicios de terceros, herramientas de análisis ni publicidad.

## 7. Cambios en esta política de privacidad
Esta política de privacidad puede actualizarse si es necesario para reflejar cambios en la app o en los requisitos legales.

## 8. Contacto
Si tienes alguna pregunta sobre privacidad, puedes ponerte en contacto con el desarrollador.

Correo electrónico: elly@robnice.de
