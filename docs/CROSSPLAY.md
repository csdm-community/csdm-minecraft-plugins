# Crossplay Java y Bedrock

CSDM 0.3.11 prepara la identidad Bedrock para el mismo servidor Paper 26.3 build
135 beta / JDK 25. **No está instalado ni probado con clientes reales todavía.**
La compilación y los tests no demuestran compatibilidad completa de protocolo.

## Instalación coordinada

1. Preparar el backend de `csdm-web-beta` siguiendo `docs/bedrock-crossplay.md`:
   migración de identidades Bedrock y tres Edge Functions. No activar el panel
   público hasta terminar las pruebas.
2. Respaldar mundos, playerdata, permisos y plugins. Detener el servidor antes de
   sustituir JAR; no usar `/reload`. Instalar CSDMVerify 0.3.11, CSDMAdmin,
   CSDMParkour y CSDMVisuals de esta rama, además de sus dependencias existentes.
3. Descargar Geyser-Spigot y Floodgate-Spigot de
   <https://geysermc.org/download/>. Instalar ambos en `plugins/`. No basta con
   agregar una dependencia de Gradle: Floodgate es un plugin de ejecución.
4. Conservar ViaVersion y ViaBackwards en versiones compatibles con Paper 26.3.
   A fecha de revisión (2026-10-03), la documentación de Geyser declara un cliente
   Java emulado 26.2. Esta combinación exige traducción 26.2 → 26.3 mediante
   ViaBackwards; **comprobar esa ruta con los JAR concretos antes de activar**.
   Revisar de nuevo las versiones soportadas al instalar; cambian con frecuencia.
5. Arrancar para generar las configuraciones y volver a detener. Aplicar solo
   estos ajustes a los archivos generados, conservando el resto de opciones.

`server.properties`:

```properties
online-mode=true
enforce-secure-profile=false
```

Geyser `plugins/Geyser-Spigot/config.yml` (fragmentos; usar la sección `java` en
la configuración actual, o `remote` si la versión instalada conserva ese nombre):

```yaml
bedrock:
  address: 0.0.0.0
  port: 19132
  clone-remote-port: false
java:
  auth-type: floodgate
```

Floodgate `plugins/floodgate/config.yml`:

```yaml
username-prefix: "."
replace-spaces: true
player-link:
  enabled: false
  require-link: false
  enable-global-linking: false
```

Conservar `key.pem` privado en el servidor. No subirlo a Git ni distribuirlo.
No cambiar el prefijo después de comenzar a usarlo sin revisar plugins externos.
CSDM verifica el XUID/UUID de la API, no ese prefijo.

En la configuración existente de CSDMVerify (los archivos antiguos no se
sobrescriben automáticamente), después de habilitar el backend:

```yaml
crossplay:
  bedrock-enabled: true
client-versions:
  max-protocol: 777
```

El límite 777 corresponde a Java 26.3. Bedrock no pasa por la validación de
protocolo Java: Geyser controla las versiones Bedrock admitidas. Se envía su
versión real y `clientProtocol: null` al backend.

6. Permitir **19132/UDP** en el firewall del VPS y en cualquier firewall externo
   de Hostinger. No reutilizar un puerto UDP ocupado por query o voz. Revisar
   que `verify.csdm.tv` resuelva directamente a la IP del servidor; un proxy web
   HTTP no transporta el juego. Java conserva 25565/TCP si esa es su configuración.
7. Reiniciar y revisar que Geyser, Floodgate, ViaVersion, ViaBackwards y CSDMVerify
   carguen. Ejecutar `geyser connectiontest verify.csdm.tv 19132` desde la consola
   y conectar un cliente Bedrock desde otra red. No basta con probar TCP.

## Cuentas, parkour y escala visual

- Java sigue usando sus UUID y verificación. Bedrock se autentica con Xbox mediante
  Floodgate, sin requerir una licencia Java adicional.
- Una cuenta web admite las dos identidades, verificadas por separado. No se
  mezclan inventarios, permisos, sanciones ni playerdata. Los permisos de staff
  o Personalidad deben asignarse al UUID correcto de cada edición.
- Se desactiva el linking de Floodgate porque cambia el UUID por el Java. CSDM
  rechaza sesiones enlazadas y UUID que no coincidan con el XUID autenticado.
- Parkour ya opera por UUID y posiciones Paper. Probar en Bedrock los botones de
  la hotbar y controles táctiles; también hay `/parkour checkpoint`,
  `/parkour reiniciar` y `/parkour salir`.
- `CSDMVisuals` deja `bedrock-viewers-enabled: false`. Los observadores Bedrock
  reciben la escala normal, mientras Java puede ver a los demás en 0.3. No activar
  escalado para observadores Bedrock hasta comprobar cámara, hitbox y paquetes.
  `enabled: false` continúa siendo el valor inicial del plugin completo.

## Prueba de aceptación pendiente

1. Java existente: entrar al museo; Java nuevo: generar código y verificar.
2. Bedrock nuevo: panel Bedrock → código → conexión → `/verificar` → reconexión
   al museo. Verificar un gamertag con espacios y el prefijo en moderación.
3. Misma cuenta CSDM con ambas ediciones: conserva el vínculo y la apariencia Java.
   Probar código usado, caducado y Discord revocado antes de consumirlo.
4. Java y Bedrock juntos: inicio, checkpoints ordenados, caída, reinicio, ocultar,
   salir, reconectar y restaurar inventario. No prometer tiempos competitivos.
5. Moderación: advertir, expulsar, sancionar y perdonar un jugador Bedrock en el
   entorno de prueba; confirmar persistencia por UUID y entrega de aviso al bot.
6. Visuals: observar desde ambas ediciones, cambios de mundo y permisos, sin
   alterar el tamaño real en el servidor. Mantener escalado Bedrock apagado si
   Geyser no traduce el comportamiento correctamente.

Para desactivar: apagar el panel web y el flag de Edge Functions, poner
`crossplay.bedrock-enabled: false` y retirar Geyser con el servidor detenido si
se necesita cerrar el puerto. Conservar datos y respaldos; no borrar identidades.

## Fuentes oficiales revisadas

- <https://geysermc.org/wiki/geyser/setup/self/paper-spigot/>
- <https://geysermc.org/wiki/floodgate/setup/paper-spigot/>
- <https://geysermc.org/wiki/geyser/supported-versions/>
- <https://geysermc.org/wiki/geyser/secure-chat/>
- <https://geysermc.org/wiki/floodgate/linking/>
- <https://geysermc.org/wiki/floodgate/api/>
- <https://github.com/ViaVersion/ViaVersion/blob/master/api/src/main/java/com/viaversion/viaversion/api/protocol/version/ProtocolVersion.java>
