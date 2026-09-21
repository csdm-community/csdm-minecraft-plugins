# CSDMAdmin 0.3.7 — candidato

Corrige el aviso al intentar entrar durante mantenimiento: los rechazos por lista blanca muestran maintenance.kick-message de CSDMAdmin. El MOTD ya anunciaba mantenimiento, pero el aviso de entrada solo se comprobaba en PlayerJoinEvent, al que nunca llegaba un jugador rechazado por whitelist.

El listener cambia exclusivamente el texto de KICK_WHITELIST mientras maintenance.enabled es true. No permite entrar, no cambia whitelist.json ni server.properties y no sustituye los mensajes de baneos u otros rechazos. Las sanciones locales conservan prioridad. Con mantenimiento desactivado se conserva el mensaje habitual de whitelist. Incluye las mejoras de Staff Mode, brújula y libro de sanciones de 0.3.4–0.3.6.

Compilado y probado con Java 25 / Paper API 26.2.build.121-stable.
Fuente: 7ce7f26922e7e4b815794285cfcf423f08632bab.
Run: https://github.com/csdm-community/csdm-minecraft-plugins/actions/runs/35546454394
SHA-256 del artefacto original: e523e6b38c63caa0694eb7e1cde4d01a596c422a8aad383c3400ad2db5e47a3a.
SHA-256 del JAR: 4d682b0f6a35192f6cddd4754d481e6d2379e38149062aa1e41ec7e9999b388a.

Pruebas aprobadas: mensaje personalizado sin permitir acceso, mantenimiento desactivado, preservación de otros rechazos, prioridad de sanciones y aviso en join para jugadores admitidos por whitelist sin bypass. Pendiente de comprobación dentro del juego en el VPS.

Ejecuta instalar-staff-mode.sh como root para verificar la descarga, respaldar CSDMAdmin y solicitar el reinicio. Actualiza únicamente CSDMAdmin y conserva la configuración. El script no acredita por sí solo que Paper haya completado el arranque.
