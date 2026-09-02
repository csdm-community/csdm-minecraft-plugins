# Instalación en el VPS de CSDM Verify

## 1. Detener Paper

En la consola interactiva donde aparece `>` escribe:

```text
stop
```

Espera a volver al prompt `root@srv1948605:...#`.

## 2. Instalar plugins públicos

Sube `install-public-plugins.sh` y `bStats-config.yml` a la misma carpeta del
servidor y ejecuta:

```bash
chmod 0750 install-public-plugins.sh
./install-public-plugins.sh /opt/csdm-verify/server
```

Instala versiones fijadas y verificadas por SHA-512 de:

- LuckPerms 5.5.71 (Bukkit/Paper)
- FancyNpcs 2.11.0
- FancyHolograms 2.11.0

El instalador deja la telemetría bStats desactivada antes del primer arranque.
Si encuentra una configuración previa con telemetría habilitada, se detiene y
no instala nada hasta que la revises.

ViaVersion y ViaBackwards ya deben permanecer instalados.

## 3. Instalar los plugins CSDM

Coloca `CSDMVerify.jar`, `CSDMCommunity.jar`, `CSDMAdmin.jar` y
`install-custom-plugins.sh` en una carpeta temporal del VPS. Luego ejecuta:

```bash
chmod 0750 install-custom-plugins.sh
./install-custom-plugins.sh /opt/csdm-verify/server "$(pwd)"
```

## 4. Primera prueba

Mantén `CSDMVerify` desactivado hasta disponer del endpoint del backend. Inicia
Paper una vez para generar las configuraciones. Comprueba que aparecen:

```text
[CSDMAdmin] ... enabled
[CSDMCommunity] ... enabled
[CSDMVerify] Verificacion desactivada
```

Antes de abrir el servidor al público, configura el spawn con:

```text
/csdmadmin setspawn
```

Concede el rango propietario a `7245` desde la consola de Paper:

```text
lp user 7245 parent set csdm-owner
```

La primera vez, `7245` debe haber entrado al menos una vez para evitar errores
de resolución del perfil. LuckPerms también admite el UUID autenticado.

Mientras el servidor siga en pruebas puedes conservar la whitelist y ejecutar:

```text
whitelist add 7245
```

Cuando `verify.csdm.tv` se abra al público, cambia `white-list=false` y
`enforce-whitelist=false`; de lo contrario, los usuarios no podrán verificar
sus cuentas.

## 5. Activar CSDMVerify

Configura `plugins/CSDMVerify/config.yml`, define el secreto como variable de
entorno `CSDM_INTERNAL_SECRET` en el servicio de Paper y cambia `enabled: true`.
Nunca coloques una clave `service_role` de Supabase en el plugin.

## 6. MOTD e icono

El MOTD se configura en `plugins/CSDMAdmin/config.yml`. Paper cargará como
icono `server-icon.png` si el archivo es PNG de 64 × 64 y se coloca junto a
`paper.jar`.
