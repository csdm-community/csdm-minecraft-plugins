# Rescate y reaparición en verificación — 0.3.8

Actualiza CSDMVerify y CSDMAdmin juntos. El instalador verifica SHA-256, respalda
ambos JAR, detiene el servicio `csdm-verify` y solicita el arranque tras copiarlos.
No cambia configuraciones, mundos, hologramas ni datos de jugadores. Si falla la
copia o el arranque inmediato, intenta restaurar ambos JAR. El mensaje final no
certifica que Paper haya terminado de iniciar; comprueba sus logs y la partida.

- En el mundo de verificación, caer más de 10 bloques por debajo del spawn
  devuelve al jugador a esa plataforma, eliminando velocidad, caída y fuego.
- El daño queda cancelado en verificación, también para staff con permiso de
  construcción. El daño de vacío incluye un rescate de respaldo.
- Morir en verificación fija la reaparición en su plataforma, no en el museo.
- CSDMAdmin limita su rescate, reaparición y teletransporte automático de entrada
  al mundo configurado como lobby. Los demás mundos conservan sus rutas.

`/csdmadmin spawn` usa el destino configurado de CSDMAdmin (actualmente museo).
`/csdmadmin setspawn` cambia ese destino a la ubicación del administrador; no
configura la plataforma de CSDMVerify. La verificación usa su propio spawn.

Validación: siete pruebas nuevas de regresión, además de la suite existente,
en GitHub Actions con Java 25 y Paper API 26.2.build.121-stable.
Fuente: e9b19089265ab270a9956f539249e8eca7a63d2c.
Build: https://github.com/csdm-community/csdm-minecraft-plugins/actions/runs/35550755802

Comprobación en VPS: esperar a `Done`, revisar `/version CSDMVerify` y
`/version CSDMAdmin`, saltar desde la plataforma y confirmar retorno sin daño;
comprobar una reaparición en verificación y otra en museo.
