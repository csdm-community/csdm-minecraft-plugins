# Parkour y escala visual del museo

Primera implementación para Paper **26.3 build 135 beta**, JDK 25 y paquete CSDM
0.3.10. Está basada en `chore/paper-26.3`, no en `main` (que aún tiene una versión
anterior). No se ha instalado en el VPS ni validado con clientes reales.

## Qué instalar

Compilar con `./gradlew clean build`. Instalar los JAR normales, nunca `-sources.jar`:

- `csdm-parkour/build/libs/csdm-parkour-0.3.10.jar`
- `csdm-visuals/build/libs/csdm-visuals-0.3.10.jar`
- `csdm-admin/build/libs/csdm-admin-0.3.10.jar` (incluye coordinación con parkour)
- PacketEvents **2.14.0**, distribución Spigot oficial de
  <https://github.com/retrooper/packetevents/releases/tag/v2.14.0>.

Con el servidor detenido, respaldar los mundos, playerdata, configuraciones y
JAR actuales. Sustituir el JAR de CSDMAdmin sin dejar otra versión duplicada.
No hace falta sustituir Community o Verify para estas funciones. Conservar
`plugins/CSDMParkour/recovery/` en todos los respaldos y actualizaciones.
**No usar `/reload` ni gestores de descarga/carga en caliente** para instalar.

El parkour arranca sin recorridos. La escala arranca con `enabled: false`.
No se crean bloques ni se altera la construcción del museo.

## Crear un recorrido

Requiere `csdm.parkour.admin` (operadores por defecto). ID de ejemplo: `museo`.
Los comandos de puntos guardan tu posición centrada sobre el bloque; usa bloques
sólidos completos, no losas, escaleras ni placas. Guarda también la orientación.

1. `/parkour crear museo`
2. Pararse sobre el bloque de inicio: `/parkour inicio museo`
3. Pararse sobre cada checkpoint, en orden: `/parkour cp museo`
4. Pararse sobre el bloque de meta: `/parkour meta museo`
5. Pararse en una zona segura fuera de los bloques anteriores: `/parkour salida museo`
6. `/parkour caida museo <Y>`: cota inferior a todos los puntos del recorrido.
7. `/parkour activar museo`

El mundo debe estar en `worlds` (por defecto, `lobby`). Todos los puntos y la salida
pertenecen al mismo mundo. No se permiten puntos superpuestos, incluso entre
recorridos. Un recorrido incompleto queda inactivo y explica el error en el log.
Editar puntos desactiva el recorrido hasta volver a activarlo. La configuración
no se puede modificar por comandos mientras haya sesiones activas.
Para corregir/eliminar un checkpoint, editar `config.yml`, renumerar consecutivamente
`checkpoints` desde 1 y actualizar `checkpoint-count`; después `/parkour recargar`.

Se inicia al pisar el bloque de inicio. La hotbar contiene:

| Slot | Función |
| --- | --- |
| 1 | Volver al último checkpoint alcanzado (al inicio si aún no hay ninguno) |
| 2 | Reiniciar recorrido y cronómetro |
| 5 | Consultar tiempo; el tiempo y progreso también aparecen en la barra de acción |
| 8 | Ocultar/mostrar jugadores durante esta sesión |
| 9 | Salir y recuperar inventario |

También existen `/parkour salir`, `/parkour reiniciar`, `/parkour checkpoint` y
`/parkour lista`. Regresar al checkpoint **no** pone el cronómetro a cero.
Los checkpoints son obligatorios y ordenados; no basta con llegar a la meta.
Al caer bajo la cota se regresa automáticamente al último checkpoint.

## Recuperación y convivencia

Antes de vaciar el inventario se escribe un archivo por UUID mediante reemplazo
atómico, con sincronización a disco. Contiene almacenamiento, armadura, mano
secundaria, slot seleccionado, modo de juego, vuelo y colisión. Si no se puede
respaldar, la sesión no comienza. Si existe un respaldo pendiente, no se sobrescribe.
La restauración guarda el playerdata antes de retirar el archivo de recuperación.
Un error de lectura o restauración conserva el respaldo y desconecta al jugador.

Al terminar/abandonar se vuelve a la salida configurada; al desconectarse o apagar
el plugin se restaura el inventario. Tras un cierre inesperado se restaura al
entrar, sin teletransportar ni saltarse el flujo existente de verificación.
Un archivo inválido no se trata como un inventario vacío.

Durante la sesión se bloquean movimientos de inventario, drops, recogida,
interacciones, daño, hambre, comandos ajenos y teletransportes externos.
Los objetos se identifican por PersistentDataContainer, no por su nombre.
La visibilidad usa la API por plugin y respeta el vanish de otros plugins.
`CSDMAdmin` omite su rescate al spawn mientras parkour controla al jugador;
Staff Mode y parkour se excluyen mutuamente mediante metadata de sesión.

No hay ranking, récords persistentes, recompensas ni medallas automáticas todavía.
Se emite `ParkourCompletedEvent` tras completar todos los checkpoints y restaurar
el inventario, para integraciones posteriores. Los tiempos de esta versión no
constituyen resultados competitivos: aún no hay control completo de efectos,
velocidad, ayuda externa o clientes modificados.

## Escala visual

En `plugins/CSDMVisuals/config.yml`, configurar los mundos y después `enabled: true`.
Ejecutar `/csdmvisuals` para recargar (requiere `csdm.visual.admin`).

- Cada observador se ve en 1.0.
- Los demás jugadores se ven en 0.3, configurable.
- Quien tenga `csdm.visual.fullsize` se ve en 1.0 para todos.

Asignar ese permiso al grupo real de Personalidad en LuckPerms. El plugin no
adivina ni crea nombres de grupos. El permiso tiene `default: false`, por lo que
ser operador por sí solo no concede la excepción; revisar permisos comodín.

Se alteran paquetes de atributos enviados al observador. Nunca se modifica
`Attribute.SCALE` del jugador en el servidor. El listener de paquetes consulta
una instantánea inmutable; no consulta Bukkit/LuckPerms desde el hilo de red.
Solo afecta a jugadores conectados, no a NPCs de FancyNpcs. Se refresca al aparecer,
reaparecer, cambiar de mundo y periódicamente para cambios de permisos. Al salir
del ámbito, desactivar la función o apagar el plugin se envía la escala real.
Las actualizaciones provocadas por aparición de entidades se agrupan por tick.

La hitbox que representa el cliente puede diferir de la del servidor; no se debe
usar este efecto para PvP. Esta versión no cambia equipos del scoreboard ni las
colisiones globales del lobby, para respetar los nametags existentes.

## Validación pendiente en servidor de pruebas

- Dos jugadores, perspectiva propia F5, observación mutua, cámara y selección de
  entidades. Probar clientes 26.3 y las versiones antiguas aceptadas vía ViaBackwards.
- Asignar/quitar `csdm.visual.fullsize`; ocultar y mostrar; salir del alcance y
  volver; reaparecer y cambiar de mundo. Confirmar NPCs intactos.
- Inventario con objetos nombrados/encantados, armadura y mano secundaria: terminar,
  abandonar, desconectar y reiniciar. Comparar todo el inventario y estado de vuelo.
- Interrumpir un servidor de prueba con sesión activa y volver a entrar. Confirmar
  recuperación; nunca hacer esta prueba destructiva en producción.
- Comprobar checkpoint al caer y que CSDMAdmin no devuelva al spawn; probar `/sm`
  durante parkour y comenzar parkour estando en Staff Mode.
- Revisar bloqueo de cofre, drop, clic/arrastre, mano secundaria, vuelo y comandos;
  meta anticipada sin checkpoints y teletransporte cancelado por otro plugin.

Las pruebas automatizadas cubren orden/reinicio de checkpoints, política de escala,
recuperación tras recrear el almacén, rechazo de respaldo duplicado/corrupto y
conservación del respaldo si falla el guardado de playerdata. La prueba de
persistencia usa inventario vacío simulado: objetos reales requieren la prueba
con Paper indicada arriba.
