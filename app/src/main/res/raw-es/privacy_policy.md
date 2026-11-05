## 1. General
Elly es un mando a distancia para televisores LG con webOS en la red local, en el móvil y en un reloj Wear OS.

El desarrollador de esta app no recopila, almacena ni procesa ningún dato personal. La app no requiere una cuenta de usuario.

## 2. Comunicación de red
La app se comunica con la TV exclusivamente dentro de tu red local:

- Búsqueda de televisores: solicitud SSDP y comprobación de las direcciones de tu red local en los puertos de webOS 3000 y 3001.
- Control: conexión cifrada con el televisor vinculado (los televisores más antiguos, sin cifrado, a través del puerto 3000). La app transmite comandos de control como teclas, movimientos del puntero, inicios de apps y cambios de entrada y de canal, y solicita las apps instaladas, las entradas y los iconos de las apps.
- Encendido: paquete Wake-on-LAN a la dirección MAC del televisor.
- Guía de programación (opcional, solo para TV con acceso root que configuras tú): conexión SSH con la TV (puerto 22). La app lee las listas de favoritos, los canales, la guía de programación y los recordatorios y grabaciones programados de la TV, programa o elimina recordatorios y grabaciones en la TV y lee los logotipos de los canales. Los logotipos que la TV aún no tiene los descarga la propia TV del servidor de logotipos de LG, como hace para su propia guía; tu dispositivo no se conecta a ese servidor. Lo mismo se aplica a las imágenes de los programas procedentes de los datos de programación en línea de LG: cuando abres los detalles de un programa, la TV descarga su imagen del servidor de LG y la transmite; tu dispositivo solo la guarda en memoria mientras la app está abierta.

Esta comunicación se realiza directamente entre tu dispositivo y el televisor. No se transmite ningún dato a servidores externos.

## 3. Reloj (Wear OS)
Igual que el móvil, la app del reloj controla la TV directamente a través de tu wifi (órdenes de control, movimientos del puntero, encendido mediante Wake-on-LAN, apertura del selector de entradas de la TV).

Para que no tengas que vincular la TV otra vez en el reloj, el móvil transfiere los siguientes datos al reloj vinculado mientras Elly esté instalada en él:

- nombre, modelo, dirección IP y dirección MAC de las TV vinculadas, su clave de vinculación y la huella de su certificado
- qué TV está activa y si el modo demo está activado
- las páginas y la distribución de teclas de la app del reloj y los iconos de las apps que abren las teclas del reloj

La transferencia utiliza la interfaz Wearable de los servicios de Google Play, que conecta el móvil y el reloj. Si el móvil y el reloj no están conectados directamente por Bluetooth o wifi, los servicios de Google Play pueden transferir los datos entre tus dispositivos de forma cifrada a través de los servidores de Google. En cuanto ya no se conoce ningún reloj con Elly, el móvil elimina los datos de esta interfaz. Las credenciales del acceso root y la guía de programación no se transfieren al reloj.

## 4. Almacenamiento de datos
Los siguientes datos se almacenan localmente en tu dispositivo:

- Nombre, modelo, dirección IP y dirección MAC de los televisores vinculados
- Clave de vinculación y huella digital del certificado del televisor
- Ajustes de la app (pantallas, diseño, teclas de app, puntero, vibración, modo demo, páginas y teclas del reloj)
- copias de las imágenes que eliges como fondo de pantalla; la app solo recibe las imágenes que seleccionas en el selector de fotos de Android
- Iconos de apps cargados desde el televisor
- para la guía de programación: usuario SSH, puerto y huella digital de la clave SSH de la TV; la contraseña root de la TV o la clave SSH privada que hayas introducido (con su frase de contraseña), cifradas con una clave que se guarda en Android Keystore y no puede extraerse del dispositivo; la guía de programación (listas de canales y programas de las últimas 24 horas y de hasta 14 días en el futuro) y los logotipos de los canales.

En el reloj, Elly guarda los datos transferidos desde el móvil (ver la sección 3) y los ajustes del reloj (vibración, gesto circular, efecto de luz, puntero, TV elegida).

Estos datos permanecen en tus dispositivos, se excluyen de las copias de seguridad de Android y se eliminan al desinstalar la app.

## 5. Sensores
En el modo de movimiento del puntero, la app utiliza el giroscopio de tu dispositivo. En el reloj, la página del puntero utiliza el giroscopio del reloj mientras un dedo está apoyado en la pantalla. Los datos del sensor solo se procesan en ese momento, se convierten directamente en movimientos del puntero y no se guardan.

## 6. Permisos
En el móvil:

- Internet: conexión con el televisor en la red local (Android requiere este permiso para cualquier conexión de red)
- Estado de la red y del wifi: determinar la red local para la búsqueda
- Multidifusión wifi: recibir las respuestas al buscar televisores
- Vibración: respuesta breve al pulsar una tecla de un widget de la pantalla de inicio
- Servicio en primer plano: enviar al televisor una tecla pulsada en un widget de la pantalla de inicio, también mientras la app está en segundo plano

En el reloj:

- Internet y estado de la red: conexión con la TV en la red local
- Cambiar la red: el reloj se conecta a tu wifi para el control en lugar de pasar la conexión por el móvil
- Vibración: respuesta en las teclas y mientras cambia el volumen
- Mantener activo: mantiene el wifi con respuesta rápida en la página del puntero

No se utiliza ningún otro permiso; en particular, no hay acceso a la ubicación, el micrófono, la cámara ni los contactos.

## 7. Terceros
Esta app no utiliza herramientas de análisis ni publicidad. Para sincronizar el móvil y el reloj utiliza la interfaz Wearable de los servicios de Google Play (ver la sección 3); también se aplica a ella la política de privacidad de Google.

## 8. Cambios en esta política de privacidad
Esta política de privacidad puede actualizarse si es necesario para reflejar cambios en la app o en los requisitos legales.

## 9. Contacto
Si tienes alguna pregunta sobre privacidad, puedes ponerte en contacto con el desarrollador.

Correo electrónico: elly@robnice.de
