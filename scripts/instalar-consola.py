#!/usr/bin/env python3
"""Instala mcrcon 0.7.2 y habilita RCON local en el servicio csdm-verify."""
import hashlib
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import subprocess
import tempfile
import time
from urllib.request import Request, urlopen

SERVER = Path('/opt/csdm-verify/server')
PROPERTIES = SERVER / 'server.properties'
SERVICE = 'csdm-verify'
BINARY = Path('/usr/local/lib/csdm/mcrcon')
CREDENTIALS = Path('/etc/csdm-rcon.json')
CONSOLE = Path('/usr/local/bin/csdm-consola')
BACKUP_ROOT = Path('/opt/csdm-verify/backups')
SOURCE = 'https://raw.githubusercontent.com/Tiiffi/mcrcon/b5951e96349ec2ecf72b468459fc503a7067ba1f/mcrcon.c'
SOURCE_SHA = 'b6dad4f81ccd077c45566ccc5f922f623d43c595795eaede49eb488f41018600'
WRAPPER = '''#!/usr/bin/env python3
import json, os, sys
if os.geteuid() != 0:
    sys.exit('Ejecuta sudo csdm-consola.')
with open('/etc/csdm-rcon.json') as f:
    credentials = json.load(f)
environment = dict(os.environ, MCRCON_HOST='127.0.0.1', MCRCON_PORT='25575', MCRCON_PASS=credentials['password'])
binary = '/usr/local/lib/csdm/mcrcon'
command = ' '.join(sys.argv[1:]).strip().lstrip('/')
arguments = [binary, '--', command] if command else [binary, '-t']
if not command:
    print('Consola CSDM. Comandos sin /. Salir: Q o Ctrl+C.', flush=True)
os.execve(binary, arguments, environment)
'''


def run(*args, **kwargs):
    return subprocess.run(args, check=True, timeout=180, **kwargs)


def setting(name):
    return run('systemctl', 'show', SERVICE, '-p', name, '--value', capture_output=True, text=True).stdout.strip()


def configure(text, password):
    updates = {'enable-rcon': 'true', 'rcon.ip': '127.0.0.1',
               'rcon.port': '25575', 'rcon.password': password}
    for key, value in updates.items():
        pattern = r'^' + re.escape(key) + r'=.*$'
        matches = re.findall(pattern, text, flags=re.MULTILINE)
        if len(matches) > 1:
            raise ValueError(f'Propiedad duplicada: {key}')
        if matches:
            text = re.sub(pattern, key + '=' + value, text, flags=re.MULTILINE)
        else:
            text = text.rstrip('\n') + '\n' + key + '=' + value + '\n'
    return text


def listeners():
    output = run('ss', '-H', '-ltn', 'sport = :25575', capture_output=True, text=True).stdout
    return [line.split()[3] for line in output.splitlines() if line.strip()]


def local_rcon_listeners(addresses):
    if not addresses:
        return False
    for endpoint in addresses:
        try:
            host, port = endpoint.rsplit(':', 1)
            if host.startswith('[') and host.endswith(']'):
                host = host[1:-1]
            address = ipaddress.ip_address(host)
            if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped:
                address = address.ipv4_mapped
            if port != '25575' or not address.is_loopback:
                return False
        except ValueError:
            return False
    return True


def interrupted(signum, frame):
    raise KeyboardInterrupt('Instalación interrumpida')


def main():
    if os.geteuid() != 0:
        raise RuntimeError('Ejecuta como root.')
    if setting('WorkingDirectory') != str(SERVER):
        raise RuntimeError('El servicio no usa el directorio esperado.')
    run('systemctl', 'is-active', '--quiet', SERVICE)
    if any(path.exists() for path in (BINARY, CREDENTIALS, CONSOLE)):
        raise RuntimeError('Ya existen archivos de consola. Prueba csdm-consola; no se sobrescribieron.')
    if listeners():
        raise RuntimeError('El puerto 25575 ya está ocupado.')
    original = PROPERTIES.read_text()
    if re.search(r'^enable-rcon=true\s*$', original, re.MULTILINE):
        raise RuntimeError('RCON ya está configurado; no se reemplazaron sus credenciales.')
    password = secrets.token_hex(32)
    configure(original, password)  # validar antes de detener el servicio
    with tempfile.TemporaryDirectory(prefix='csdm-rcon-') as temporary:
        source = Path(temporary) / 'mcrcon.c'
        executable = Path(temporary) / 'mcrcon'
        print('Descargando y verificando mcrcon 0.7.2...', flush=True)
        with urlopen(Request(SOURCE, headers={'User-Agent': 'CSDM-Console-Setup'}), timeout=60) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != SOURCE_SHA:
            raise RuntimeError('La descarga no coincide con el SHA-256 esperado.')
        source.write_bytes(data)
        if not shutil.which('gcc'):
            run('apt-get', 'update')
            run('apt-get', 'install', '-y', 'gcc', 'libc6-dev')
        run('gcc', '-std=gnu99', '-O2', '-Wall', '-Wextra', '-o', str(executable), str(source))
        run(str(executable), '-v')
        BACKUP_ROOT.mkdir(parents=True, exist_ok=True)
        backup = Path(tempfile.mkdtemp(prefix='consola-', dir=BACKUP_ROOT))
        saved = False
        touched = False
        try:
            print('Deteniendo Minecraft para activar la consola local...', flush=True)
            touched = True
            run('systemctl', 'stop', SERVICE)
            if setting('MainPID') != '0':
                raise RuntimeError('El proceso no terminó.')
            shutil.copy2(PROPERTIES, backup / 'server.properties')
            metadata = PROPERTIES.stat()
            saved = True
            PROPERTIES.write_text(configure(PROPERTIES.read_text(), password))
            os.chown(PROPERTIES, metadata.st_uid, metadata.st_gid)
            # server.properties ahora contiene una contraseña de administración.
            PROPERTIES.chmod(0o600)
            BINARY.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(executable, BINARY)
            BINARY.chmod(0o755)
            CREDENTIALS.write_text(json.dumps({'password': password}) + '\n')
            CREDENTIALS.chmod(0o600)
            CONSOLE.write_text(WRAPPER)
            CONSOLE.chmod(0o755)
            run('systemctl', 'start', SERVICE)
            print('Esperando el arranque y comprobando RCON (hasta 120 segundos)...', flush=True)
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                addresses = listeners()
                if addresses and not local_rcon_listeners(addresses):
                    raise RuntimeError('RCON no quedó limitado a localhost. Dirección detectada: '
                                       + ', '.join(addresses))
                if addresses:
                    try:
                        result = subprocess.run([str(CONSOLE), 'list'], capture_output=True, text=True, timeout=5)
                        if result.returncode == 0:
                            print('Dirección local comprobada: ' + ', '.join(addresses), flush=True)
                            print(result.stdout.strip(), flush=True)
                            break
                    except subprocess.TimeoutExpired:
                        pass
                if setting('ActiveState') == 'failed':
                    raise RuntimeError('El servicio falló durante el arranque.')
                time.sleep(2)
            else:
                raise RuntimeError('No se pudo verificar RCON dentro del tiempo esperado.')
        except BaseException:
            print(f'Instalación detenida. Respaldo: {backup}', flush=True)
            if touched:
                run('systemctl', 'stop', SERVICE)
                if setting('MainPID') != '0':
                    raise RuntimeError('No se restauró: Paper sigue ejecutándose. Revisa el servicio.')
                if saved:
                    shutil.copy2(backup / 'server.properties', PROPERTIES)
                    os.chown(PROPERTIES, metadata.st_uid, metadata.st_gid)
                    for path in (CONSOLE, CREDENTIALS, BINARY):
                        path.unlink(missing_ok=True)
                run('systemctl', 'start', SERVICE)
                print('Configuración anterior restaurada y arranque solicitado.', flush=True)
            raise
    print(f'LISTO: RCON verificado en 127.0.0.1:25575. Respaldo: {backup}')
    print('Abre la consola con: csdm-consola')


if __name__ == '__main__':
    os.umask(0o077)
    signal.signal(signal.SIGTERM, interrupted)
    try:
        main()
    except (Exception, KeyboardInterrupt) as error:
        raise SystemExit(f'DETENIDO: {error}')
