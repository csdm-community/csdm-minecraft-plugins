# Instalación en el VPS de CSDM Verify

Esta guía corresponde a CSDM Minecraft Plugins 0.3.3 y al servicio systemd
`csdm-verify` ubicado en `/opt/csdm-verify/server`.

## 1. Detener Paper y crear un respaldo

```bash
systemctl stop csdm-verify
systemctl is-active csdm-verify

backup_dir="/opt/csdm-verify/backups/plugins-$(date -u +%Y%m%dT%H%M%SZ)"
install -d -m 0750 "$backup_dir"
cp -a /opt/csdm-verify/server/plugins "$backup_dir/plugins"
cp -a /etc/csdm-verify.env "$backup_dir/csdm-verify.env"
```

`systemctl is-active` debe responder `inactive` antes de reemplazar los JAR.

## 2. Instalar los plugins públicos

Desde el directorio descomprimido de la versión:

```bash
chmod 0750 install-public-plugins.sh install-custom-plugins.sh
./install-public-plugins.sh /opt/csdm-verify/server
```

Instala versiones fijadas y verificadas por SHA-512 de LuckPerms, FancyNpcs y
FancyHolograms. ViaVersion y ViaBackwards deben permanecer instalados.

Si el script informa que bStats ya está habilitado, revisa
`plugins/bStats/config.yml`, cambia `enabled: false` y repite el comando.

## 3. Instalar los plugins CSDM

```bash
./install-custom-plugins.sh /opt/csdm-verify/server "$(pwd)"
```

El instalador valida los JAR y coloca `CSDMVerify.jar`, `CSDMCommunity.jar` y
`CSDMAdmin.jar`.

## 4. Migrar la configuración de 0.2.0

El modelo anterior de rangos no es compatible con 0.3.0. Conserva su copia y
permite que `CSDMCommunity` genere la nueva configuración:

```bash
community_config=/opt/csdm-verify/server/plugins/CSDMCommunity/config.yml
if test -f "$community_config"; then
  mv "$community_config" "${community_config}.pre-0.3.0"
fi
```

En `CSDMAdmin`, desactiva el vuelo global. Los rangos autorizados usarán
`/fly`:

```bash
admin_config=/opt/csdm-verify/server/plugins/CSDMAdmin/config.yml
if test -f "$admin_config"; then
  sed -i 's/^  allow-flight: true$/  allow-flight: false/' "$admin_config"
fi
```

No elimines `CSDMAdmin/config.yml`: contiene el spawn configurado del lobby.

## 5. Secreto del puente de moderación

La misma cadena aleatoria debe configurarse como secreto de la Edge Function
`CSDM_MINECRAFT_ADMIN_SECRET` y en el VPS. No uses una clave `service_role`.

En `/etc/csdm-verify.env`:

```text
CSDM_MINECRAFT_ADMIN_SECRET=REEMPLAZAR_CON_SECRETO_ALEATORIO_DE_64_CARACTERES
```

En `/opt/csdm-verify/server/plugins/CSDMAdmin/config.yml`:

```yaml
moderation:
  backend-url: "https://PROJECT_REF.supabase.co/functions/v1/minecraft-moderation"
  internal-secret-env: "CSDM_MINECRAFT_ADMIN_SECRET"
  request-timeout-seconds: 5
```

La sanción local funciona aunque este puente aún no esté configurado.

## 6. Iniciar y comprobar

```bash
chown -R minecraft:minecraft /opt/csdm-verify/server/plugins
systemctl start csdm-verify
systemctl status csdm-verify --no-pager
journalctl -u csdm-verify -n 160 --no-pager
```

Comprueba que `CSDMVerify`, `CSDMCommunity` y `CSDMAdmin` aparecen habilitados y
que no hay stack traces.

La actualización 0.3.3 conserva la configuración existente. Al iniciar,
`CSDMVerify` incorpora únicamente las claves de mensajes que falten; así se
reparan `museum-title` y `museum-subtitle` sin borrar textos personalizados.
`CSDMCommunity` activa los tags sobre los jugadores incluso si el bloque
`nametags` todavía no existe en el `config.yml` del VPS. También reemplaza el
formato `<RANGO • Nick> mensaje` del chat por `RANGO • Nick: mensaje`.

## 7. Asignar Dirección a 7245

Después de que `7245` entre al servidor, ejecuta en la consola de Paper:

```text
rangos asignar 7245 direccion
```

También puedes usar directamente LuckPerms:

```text
lp user 7245 parent set csdm-direccion
```

Los rangos de prestigio son independientes. Por ejemplo:

```text
rangos asignar 7245 inmortal
```

El resultado visible sobre el jugador será:

```text
DIRECCIÓN • 7245 • INMORTAL
```

## 8. Comandos principales

```text
/staff                  Activa o desactiva Staff Mode
/sm                     Alias corto de /staff
/fly                    Activa o desactiva el vuelo autorizado
/rangos                 Consulta los rangos disponibles
/sancionar              Abre el flujo de sanciones
```

Conserva `online-mode=true`. Cuando `verify.csdm.tv` se abra al público cambia
`white-list=false` y `enforce-whitelist=false`; mientras siga en pruebas puedes
mantener `7245` en la whitelist.
