# CSDM Minecraft Plugins

Plugins Paper para la infraestructura Minecraft de CSDM.

## Módulos

- `csdm-verify`: vinculación segura de una cuenta CSDM con el UUID autenticado por Minecraft.
- `csdm-community`: rangos de staff delegados a LuckPerms y medallas de CSDM.
- `csdm-admin`: políticas del lobby, noche permanente, spawn y modo mantenimiento.

El módulo de comunidad también personaliza los mensajes de entrada/salida. El
módulo de administración publica un MOTD de dos líneas y mantiene la noche y el
clima sin depender de EssentialsX.

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
