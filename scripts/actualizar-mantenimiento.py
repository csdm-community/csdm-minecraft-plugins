#!/usr/bin/env python3
"""Actualiza los avisos y el nombre del servidor y conserva un respaldo."""
import json
import os
from pathlib import Path
import copy

import yaml
import shutil
import tempfile


MESSAGES = {
    ("maintenance", "kick-message"): (
        "<aqua><bold>CSDM • MANTENIMIENTO</bold></aqua>\n"
        "<gray>Estamos realizando tareas de mantenimiento.</gray>\n"
        "<gray>Vuelve a intentarlo más tarde.</gray>"
    ),
    ("motd", "online-line-1"): "<gradient:#00E5FF:#B86BFF><bold>✦ CSDM ✦</bold></gradient>",
    ("motd", "maintenance-line-1"): "<gold><bold>⚙ CSDM • MANTENIMIENTO</bold></gold>",
    ("motd", "maintenance-line-2"): "<gray>Volveremos pronto.</gray>",
}


def updated_config(text):
    # Cambiar los segmentos de los valores conserva los demás textos y comentarios.
    root = yaml.compose(text, Loader=yaml.SafeLoader)
    expected = copy.deepcopy(yaml.safe_load(text))
    edits = []
    for (section, field), message in MESSAGES.items():
        node = root
        for name in (section, field):
            if not isinstance(node, yaml.MappingNode):
                raise ValueError(f"Se esperaba una sección YAML: {section}.{field}")
            matches = [value for key, value in node.value if key.value == name]
            if len(matches) != 1:
                raise ValueError(f"Clave ausente o duplicada: {section}.{field}")
            node = matches[0]
        if not isinstance(node, yaml.ScalarNode) or node.tag != "tag:yaml.org,2002:str":
            raise ValueError(f"Se esperaba un texto en {section}.{field}")
        start, end = node.start_mark.index, node.end_mark.index
        replacement = json.dumps(message, ensure_ascii=False)
        if text[start:end].endswith("\n"):
            replacement += "\n"
        edits.append((start, end, replacement))
        expected[section][field] = message
    updated = text
    for start, end, replacement in sorted(edits, reverse=True):
        updated = updated[:start] + replacement + updated[end:]
    if yaml.safe_load(updated) != expected:
        raise ValueError("La validación detectó cambios adicionales; no se modificó el archivo.")
    return updated


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
