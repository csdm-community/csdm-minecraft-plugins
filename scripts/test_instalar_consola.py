from pathlib import Path
import runpy
import unittest


MODULE = runpy.run_path(str(Path(__file__).with_name('instalar-consola.py')))


class LocalRconTest(unittest.TestCase):
    def test_accepts_ipv4_and_ipv6_loopback_representations(self):
        for address in ('127.0.0.1:25575', '[::ffff:127.0.0.1]:25575',
                        '[::ffff:7f00:1]:25575', '[::1]:25575'):
            with self.subTest(address=address):
                self.assertTrue(MODULE['local_rcon_listeners']([address]))

    def test_rejects_wildcard_external_private_and_invalid_endpoints(self):
        for address in ('0.0.0.0:25575', '[::]:25575', '*:25575',
                        '192.168.1.5:25575', '203.0.113.1:25575',
                        '[::ffff:192.168.1.5]:25575', '[::ffff:0.0.0.0]:25575',
                        '[2001:db8::1]:25575', '127.0.0.1:25565', 'invalid'):
            with self.subTest(address=address):
                self.assertFalse(MODULE['local_rcon_listeners']([address]))

    def test_every_listener_must_be_local_and_at_least_one_present(self):
        self.assertFalse(MODULE['local_rcon_listeners']([]))
        self.assertFalse(MODULE['local_rcon_listeners'](['127.0.0.1:25575', '[::]:25575']))
        self.assertTrue(MODULE['local_rcon_listeners'](['127.0.0.1:25575', '[::1]:25575']))


if __name__ == '__main__':
    unittest.main()
