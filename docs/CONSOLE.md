# Consola del VPS

El servicio `csdm-verify` puede seguir ejecutándose con `StandardInput=null`.
`journalctl -u csdm-verify -f -o cat` muestra registros, pero no envía comandos.

Como root, ejecutar `python3 scripts/instalar-consola.py` en el VPS instala un
cliente [mcrcon 0.7.2](https://github.com/Tiiffi/mcrcon/tree/v0.7.2) compilado desde
una revisión fija, cuyo código se verifica por SHA-256 antes de compilar.
Necesita Python 3, systemd, ss y GCC; si falta GCC instala gcc y libc6-dev con apt.

El instalador reinicia Minecraft. Configura `rcon.ip=127.0.0.1`, puerto 25575 y
una contraseña aleatoria, conservando el puerto y la dirección del juego. No abre
puertos en el firewall. Paper permite configurar la IP de RCON por separado:
[código de Paper](https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/server/dedicated/DedicatedServerProperties.java.patch).

Guarda un respaldo en `/opt/csdm-verify/backups/consola-*`, restringe los archivos
que contienen la contraseña y comprueba la dirección de escucha y la respuesta
a `list`. Si falla, restaura las propiedades y solicita el arranque anterior.
No vuelve a instalar sobre archivos de consola existentes.

## Uso

```bash
csdm-consola
```

Escribir comandos sin `/`. `Q`, Ctrl+C o Ctrl+D cierran solo el cliente.
`stop` sí detiene Minecraft; usar `Q` para salir normalmente.

También acepta un comando desde el shell:

```bash
csdm-consola "csdmadmin recargar"
csdm-consola "csdmadmin mantenimiento off"
```

La consola muestra respuestas a comandos. Para seguir todos los registros usar
otra terminal con `journalctl -u csdm-verify -n 50 -f -o cat`.
Las credenciales se leen localmente desde `/etc/csdm-rcon.json` (root, modo 600)
y se pasan al cliente mediante su entorno, sin incluirlas en los argumentos.
