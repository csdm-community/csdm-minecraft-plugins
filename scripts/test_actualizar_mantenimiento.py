import copy
from pathlib import Path
import runpy
import unittest

import yaml


MODULE = runpy.run_path(str(Path(__file__).with_name("actualizar-mantenimiento.py")))


class MaintenanceMigrationTest(unittest.TestCase):
    def test_yaml_string_formats_and_unrelated_settings(self):
        for value in (
            '"Mensaje anterior"',
            "'Mensaje anterior'",
            "Mensaje anterior # comentario",
            "'Mensaje anterior\n    con otra línea'",
            '"Mensaje anterior\\ncon otra línea"',
            "|-\n    Mensaje anterior\n    con otra línea",
            ">\n    Mensaje anterior\n    con otra línea",
        ):
            with self.subTest(value=value):
                text = (
                    "# Configuración personalizada\nmaintenance:\n  enabled: true\n"
                    f"  kick-message: {value}\n"
                    "motd:\n  enabled: false\n  online-line-1: Título\n"
                    "  online-line-2: Mi subtítulo personalizado\n"
                    "  maintenance-line-1: Mantenimiento\n"
                    "  maintenance-line-2: Regresamos pronto\n"
                    "spawn:\n  x: 42.5\n"
                )
                expected = copy.deepcopy(yaml.safe_load(text))
                for (section, field), message in MODULE["MESSAGES"].items():
                    expected[section][field] = message
                updated = MODULE["updated_config"](text)
                self.assertEqual(yaml.safe_load(updated), expected)
                self.assertIn("# Configuración personalizada", updated)
                self.assertEqual(MODULE["updated_config"](updated), updated)

    def test_missing_or_duplicate_keys_stop_without_result(self):
        for text in (
            "maintenance: {}",
            "maintenance:\n  kick-message: primero\n  kick-message: segundo",
        ):
            with self.subTest(text=text), self.assertRaises(ValueError):
                MODULE["updated_config"](text)


if __name__ == "__main__":
    unittest.main()
