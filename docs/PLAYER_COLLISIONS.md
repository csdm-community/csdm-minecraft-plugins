# Empujones entre jugadores

CSDMCommunity 0.3.13 establece `COLLISION_RULE = NEVER` en los equipos
`csdm_t_` que ya utiliza para los rangos. La regla se aplica a todos los
jugadores administrados por CSDMCommunity, en todos los mundos, incluido el
parkour y la plataforma de verificación.

Se conservan los equipos individuales y las etiquetas de rango. Si
`nametags.enabled` es `false`, se mantienen los equipos sin prefijos ni sufijos
para conservar la regla de colisión. No se crea un segundo equipo que desplace
al jugador de su equipo de rango.

La regla se reaplica al sincronizar la presentación: entrada, recarga,
cambios de rango y actualización periódica. Al abandonar el parkour o el modo
staff, restaurar `Player#setCollidable` no cambia la regla del equipo.

Esto desactiva el empuje por contacto; no borra la hitbox ni cambia las reglas
de daño, el retroceso de ataques, la escala visual o las skins.

La API de Bukkit recomienda la regla del equipo para controlar colisiones
entre jugadores, debido a la predicción de movimiento del cliente:
https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/LivingEntity.html#setCollidable(boolean)

## Instalación y comprobación

Ejecutar `bash scripts/actualizar-community.sh SHA_COMPLETO` como root en el VPS.
El instalador compila antes de detener Paper, respalda CSDMCommunity y actualiza
solo su JAR. No requiere cambiar la configuración existente.

Después del reinicio, probar con dos jugadores Java y con Java/Bedrock:

- Caminar uno contra otro y contra un jugador quieto en el lobby.
- Repetir durante el parkour, en un checkpoint y después de abandonar la ruta.
- Comprobar las etiquetas de rango, incluida una cuenta Personalidad.
- Volver a entrar y cambiar un rango para confirmar que la regla persiste.

La comprobación visual con clientes reales queda pendiente de la instalación.
Si otro plugin cambia el scoreboard o el equipo del jugador, debe conservar
esta regla para no volver a habilitar el empuje.
