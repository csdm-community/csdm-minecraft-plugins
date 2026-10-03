# CSDM Minecraft Plugins

Plugins Paper para la infraestructura Minecraft de CSDM.

## Módulos

- `csdm-verify`: vinculación segura de una cuenta CSDM con el UUID autenticado por Minecraft.
- `csdm-community`: rangos de staff delegados a LuckPerms y medallas de CSDM.
- `csdm-admin`: políticas del lobby, noche permanente, spawn y modo mantenimiento.

El módulo de comunidad también personaliza los mensajes de entrada/salida. El
módulo de administración publica un MOTD de dos líneas y mantiene la noche y el
clima sin depender de EssentialsX.

Los rangos funcionales aparecen antes del nick sobre la cabeza de cada jugador
y los rangos de prestigio después. El formato se actualiza también cuando el
cambio se hace directamente con LuckPerms.
En el chat, el nombre se muestra sin corchetes angulares: `RANGO • Nick: mensaje`.

## Requisitos

- Paper 26.2 build 121 o posterior compatible.
- Java 25.
- LuckPerms para `CSDMCommunity`.
- ViaVersion y ViaBackwards para el rango de clientes admitido por `CSDMVerify`.

## Compilación

```bash
./gradlew clean build
```

Los JAR quedan en el directorio `build/libs` de cada módulo.

La guía de despliegue está en [`docs/VPS_INSTALL.md`](docs/VPS_INSTALL.md).

## Plugins públicos fijados

El script `scripts/install-public-plugins.sh` instala y verifica por SHA-512:

- LuckPerms 5.5.71 para permisos y rangos.
- FancyNpcs 2.11.0 para NPCs con skins y escala.
- FancyHolograms 2.11.0 para nombres, puestos y estadísticas flotantes.

La telemetría bStats se instala desactivada mediante
`scripts/bStats-config.yml`.

## Seguridad

- Paper debe conservar `online-mode=true`.
- El secreto interno de `CSDMVerify` se lee preferentemente desde `CSDM_INTERNAL_SECRET`.
- Ningún plugin contiene ni debe recibir una clave `service_role` de Supabase.
- Los permisos administrativos pertenecen a LuckPerms; los componentes visuales nunca conceden permisos.

## Avisos de Staff Mode

`/sm` muestra títulos al activar y desactivar el modo. Vanish, congelación y
liberación también tienen avisos en español. El jugador congelado recibe el mismo
aviso al usar `/staff congelar <jugador>` o la herramienta de hielo. Una interacción
con la mano secundaria no repite la acción. No se añaden sanciones automáticas.

Edita `plugins/CSDMAdmin/staff-messages.yml` y ejecuta `/csdmadmin recargar`.
Los mensajes admiten MiniMessage y `<player>` para el nombre del jugador; puedes
cambiar cada lista `chat`, título, subtítulo y los tiempos en milisegundos.
`chat: []` silencia el chat de ese aviso y `title.enabled: false` oculta su título.
Las claves nuevas se incorporan al archivo sin sustituir tus personalizaciones.
Recargar los textos conserva las sesiones de Staff Mode, vanish e inventarios.

La vara de blaze (sexto espacio) y `/staff aleatorio` llevan a un jugador visible
del mismo mundo; se excluyen el propio moderador y quienes estén en Staff Mode.
La inspección añade la mano principal, efectos, nivel de experiencia, vida, hambre,
ping y tiempo jugado. Es una captura al abrir, de solo lectura: vuelve a abrirla
para actualizar los datos. Al salir de Staff Mode se cierra la inspección; sus
copias de objetos siguen protegidas aunque cambie el estado del moderador.

## Brújula de Staff Mode

La brújula y `/staff tp` abren un cofre doble (54 espacios) con las cabezas de
los jugadores conectados y visibles para quien lo abre. El propio moderador no
aparece. Haz clic en una cabeza para teletransportarte.

- Filas 1–2: jugadores con `csdm.staffmode.use` o `csdm.staffmode.priority`.
- Fila 3: separador.
- Filas 4–5: los demás jugadores.
- Fila 6: página anterior/siguiente, recuentos, actualizar y cerrar.

Ambos grupos se ordenan por nombre y tienen paginación independiente de 18
jugadores por página, controlada con los mismos botones. El permiso de prioridad
solo cambia la ubicación en el menú; no concede herramientas de moderación:

```text
/lp user NOMBRE permission set csdm.staffmode.priority true
```

La lista se reconstruye al actualizar o cambiar de página. Antes de teletransportar
se comprueban de nuevo los permisos del moderador, la sesión del menú y que el
destino siga conectado y visible. El menú es de solo lectura y se cierra al salir
de Staff Mode. `/staff tp Nombre` conserva el teletransporte directo.

## Libro de sanciones

El séptimo espacio de Staff Mode contiene un libro de **Historial de sanciones**.
Abre un cofre doble de solo lectura con 45 registros por página, del más reciente
al más antiguo. Cada registro muestra jugador, tipo, motivo, moderador, fecha UTC,
vencimiento cuando corresponde, estado e ID. Incluye registros de desconectados.

Haz clic en un registro para filtrar por ese jugador (UUID, incluso si cambió de
nombre); usa «Ver todos los jugadores» para volver. El embudo alterna todo el
historial/restricciones activas, y el girasol actualiza los datos. Una suspensión
vencida aparece como finalizada aunque su marca guardada siga activa. Advertencias,
expulsiones y perdones se muestran como acciones registradas, no como bloqueos.

También puedes usar `/staff sanciones [jugador]` con Staff Mode activo. El permiso
`csdm.sanctions.view` se hereda de `csdm.staffmode.use`; se puede denegar expresamente
con LuckPerms para restringir la consulta. El libro solo consulta `sanctions.yml`
de CSDMAdmin; no aplica ni retira sanciones y no importa historiales de otros plugins.

## Mensaje de mantenimiento y lista blanca

Con `/csdmadmin mantenimiento on`, los rechazos por lista blanca muestran
`maintenance.kick-message` de CSDMAdmin, en lugar del texto genérico de Minecraft.
La comprobación se realiza antes de entrar al mundo. Solo cambia el mensaje: la
lista blanca sigue rechazando el acceso, incluso si el jugador tiene permiso para
omitir mantenimiento. Los baneos y otros motivos de rechazo conservan su mensaje;
las sanciones locales siguen teniendo prioridad. Con mantenimiento desactivado,
la lista blanca vuelve a usar su mensaje habitual. No se modifica `whitelist.json`
ni `server.properties`. Activar únicamente la whitelist no activa mantenimiento.
