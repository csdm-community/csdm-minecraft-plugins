#!/usr/bin/env python3
"""Actualiza los dos textos de mantenimiento existentes y conserva un respaldo."""
import json
import os
from pathlib import Path
import re
import shutil
import tempfile


MESSAGES = {
    ("maintenance", "kick-message"): (
        "<aqua><bold>ARCHIVO CSDM</bold></aqua>\n"
        "<gray>Estamos realizando tareas de mantenimiento.</gray>\n"
        "<gray>Vuelve a intentarlo más tarde.</gray>"
    ),
    ("motd", "maintenance-line-2"): "<gray>El Archivo volverá pronto</gray>",
}


def updated_config(text):
    section = None
    found = []
    output = []
    for line in text.splitlines(keepends=True):
        header = re.match(r"^([\w-]+):", line)
        if header:
            section = header.group(1)
        field = re.match(r"^  ([\w-]+):\s*(.*?)\s*$", line)
        key = (section, field.group(1)) if field else None
        if key in MESSAGES:
            # El formato esperado es un escalar citado en una sola línea.
            value = field.group(2)
            if not re.fullmatch(r'"(?:[^"\\]|\\.)*"|\'(?:[^\']|\'\')*\'', value):
                raise ValueError(f"Formato inesperado en {section}.{key[1]}; no se modificó el archivo.")
            found.append(key)
            line = f"  {key[1]}: {json.dumps(MESSAGES[key], ensure_ascii=False)}\n"
        output.append(line)
    if sorted(found) != sorted(MESSAGES):
        raise ValueError("Faltan claves o están duplicadas; no se modificó el archivo.")
    return "".join(output)


def main():
    path = Path("/opt/csdm-verify/server/plugins/CSDMAdmin/config.yml")
    original = path.read_text(encoding="utf-8")
    updated = updated_config(original)
    if original == updated:
        print("Los textos ya están actualizados. Ejecuta /csdmadmin recargar en Minecraft.")
        return
    backup_dir = Path("/opt/csdm-verify/backups")
    backup_dir.mkdir(parents=True, exist_ok=True)
    backup = Path(tempfile.mkdtemp(prefix="textos-mantenimiento-", dir=backup_dir)) / "config.yml"
    shutil.copy2(path, backup)
    metadata = path.stat()
    descriptor, name = tempfile.mkstemp(prefix="config-", suffix=".tmp", dir=path.parent)
    temporary = Path(name)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            stream.write(updated)
            stream.flush()
            os.fsync(stream.fileno())
            os.fchown(stream.fileno(), metadata.st_uid, metadata.st_gid)
            os.fchmod(stream.fileno(), metadata.st_mode & 0o7777)
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)
    print(f"Textos actualizados. Respaldo: {backup}")
    print("Ejecuta /csdmadmin recargar en Minecraft para aplicarlos.")


if __name__ == "__main__":
    main()
