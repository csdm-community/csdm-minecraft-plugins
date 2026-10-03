# CSDMAdmin 0.3.5 — candidato

Añade la brújula con un cofre doble de cabezas: staff y permiso csdm.staffmode.priority en las dos filas superiores, jugadores en las filas 4–5 y controles en la última. Incluye paginación, actualización, teletransporte por UUID y comprobación de permisos, visibilidad y conexión al hacer clic. Conserva los títulos, mensajes editables, TP aleatorio e inspección ampliada de 0.3.4.

Compilado y probado por GitHub Actions con Java 25 / Paper API 26.2.build.121-stable.
Fuente: fca555cbd626503701f757b9a20087b195c84580.
Run: https://github.com/csdm-community/csdm-minecraft-plugins/actions/runs/35521227682
SHA-256 del artefacto original: 65c3593d45d0921be8ddfb8e255dae06c45d297e43c6a107ee357fc8a6cfab58.
SHA-256 del JAR: 7bc12a57294bf72698b570c90d0ef0a5cd3e04299060042f126b270bbeacea3a.

Pruebas: clasificación por permisos, exclusión de jugadores ocultos/desconectados y del propio moderador, paginación sin omisiones/duplicados, bloqueo de movimientos de objetos y revalidación del destino, permisos y sesión antes del teletransporte. Pendiente de comprobación visual en el VPS.

Este paquete actualiza únicamente CSDMAdmin; conserva CSDMVerify y CSDMCommunity instalados. Ejecuta instalar-staff-mode.sh como root para verificar la descarga, respaldar el plugin anterior y solicitar el reinicio. El script no acredita por sí solo que Paper haya completado el arranque.

Después de arrancar, activa /sm y usa la brújula o /staff tp. Para priorizar a una persona sin otorgarle moderación:

```text
/lp user NOMBRE permission set csdm.staffmode.priority true
```
