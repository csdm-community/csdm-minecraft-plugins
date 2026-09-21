# CSDMAdmin 0.3.6 — candidato

Añade un libro de historial de sanciones en el séptimo espacio de Staff Mode. Abre una GUI de solo lectura con los registros locales de CSDMAdmin, de más reciente a más antiguo, incluyendo jugadores desconectados. Cada registro muestra jugador, tipo, motivo, moderador, fecha UTC, vencimiento, estado e ID.

Incluye páginas de 45 registros, filtro de restricciones activas, actualización y consulta por jugador al pulsar un registro o con /staff sanciones Nombre. Permiso: csdm.sanctions.view, heredado de csdm.staffmode.use. No aplica ni retira sanciones desde el menú. Conserva la brújula con cabezas y las mejoras de Staff Mode de 0.3.4–0.3.5.

Compilado y probado por GitHub Actions con Java 25 / Paper API 26.2.build.121-stable.
Fuente: 8bcd87279663d02d897b9d1d913d2508ec06993b.
Run: https://github.com/csdm-community/csdm-minecraft-plugins/actions/runs/35522529498
SHA-256 del artefacto original: 66cdeecb8ef9057470be540651ef12bfd986d8c72a503b0b9ad09fa7f755332e.
SHA-256 del JAR: 7c5446f2fdfa02a2068490afaf91f9d8a438c59a321d2db48050b274e9589c73.

Pruebas aprobadas: persistencia tras recarga/perdón, registros con fechas inválidas, estados y vencimientos, paginación, filtro por UUID, motivos largos, permisos y sesión al hacer clic, bloqueo de movimientos de objetos. Pendiente de comprobación visual en el VPS.

Ejecuta instalar-staff-mode.sh como root para verificar la descarga, respaldar CSDMAdmin y solicitar el reinicio. Actualiza únicamente CSDMAdmin y conserva los datos existentes. El script no acredita por sí solo que Paper haya completado el arranque. Después entra, activa /sm y usa el libro del séptimo espacio.
