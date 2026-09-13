import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
import zipfile

spec = importlib.util.spec_from_file_location("local_data", Path(__file__).with_name("local-data.py"))
data = importlib.util.module_from_spec(spec)
spec.loader.exec_module(data)


class LocalDataTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = (Path(self.tmp.name) / "game").resolve()
        self.root.mkdir()
        self.chat = self.write("logs/chat/ottoextra-chat-2026-09-11-1.log", "private chat")
        self.legacy = self.write("logs/ottoextra-chat.log", "old chat")
        self.cache = self.write("config/ottoextra/cache/skins.json", "{}")
        self.letter = self.write("config/ottoextra/letters/sent/letter.json", "personal letter")
        self.backup = self.write("config/ottoextra/backups/settings/ottoextra/letters/old.json", "backup")
        self.vanilla = self.write("logs/latest.log", "vanilla")
        self.config = self.write("config/ottoextra/ottoextra.json", "config and consent")
        self.token = self.write("config/ottoextra/account-token.json", "secret fixture")

    def write(self, relative, text):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def test_inventory_keeps_archives_distinct_and_excludes_config_tokens_vanilla(self):
        result = {item["path"]: item["category"] for item in data.inventory(self.root)}
        self.assertEqual("letters", result[self.letter.relative_to(self.root).as_posix()])
        self.assertEqual("backups", result[self.backup.relative_to(self.root).as_posix()])
        self.assertEqual(5, len(result))

    def test_preview_is_read_only_export_verified_and_deletion_selective(self):
        document = data.plan(self.root, ["chat"])
        self.assertTrue(self.chat.exists())
        target = Path(self.tmp.name) / "export.zip"
        data.export(self.root, document, target)
        with zipfile.ZipFile(target) as archive:
            self.assertEqual(b"private chat", archive.read(self.chat.relative_to(self.root).as_posix()))
        self.assertEqual(2, data.delete(self.root, document))
        self.assertFalse(self.chat.exists())
        for kept in (self.cache, self.letter, self.backup, self.vanilla, self.config, self.token):
            self.assertTrue(kept.exists())
        with self.assertRaises(FileExistsError):
            data.export(self.root, data.plan(self.root, ["cache"]), target)

    def test_changed_file_invalidates_whole_plan_before_any_deletion(self):
        document = data.plan(self.root, ["chat"])
        self.legacy.write_text("modified")
        with self.assertRaises(ValueError):
            data.delete(self.root, document)
        self.assertTrue(self.chat.exists())
        self.assertTrue(self.legacy.exists())

    def test_tampered_plan_cannot_delete_unrelated_or_outside_paths(self):
        document = data.plan(self.root, ["chat"])
        for malicious in ("../outside", "logs/latest.log", "config/ottoextra/ottoextra.json", "C:/secret"):
            altered = json.loads(json.dumps(document))
            altered["files"][0]["path"] = malicious
            with self.assertRaises((ValueError, OSError)):
                data.delete(self.root, altered)
        self.assertTrue(self.chat.exists())

    def test_expired_selection_never_prunes_letters_or_backups(self):
        now = time.time()
        for file in (self.chat, self.letter, self.backup, self.cache):
            os.utime(file, (now - 31 * 86400, now - 31 * 86400))
        document = data.plan(self.root, data.CATEGORIES, expired=True)
        self.assertEqual({"chat", "cache"}, {item["category"] for item in document["files"]})
        self.assertEqual(2, len(document["files"]))

    def test_active_session_lock_blocks_another_process(self):
        with data.offline_lock(self.root):
            result = subprocess.run([sys.executable, str(Path(data.__file__)), "--game-dir", str(self.root), "list"],
                                    text=True, capture_output=True, timeout=5)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Close every Minecraft", result.stderr)

    def test_export_inside_game_directory_is_rejected(self):
        with self.assertRaises(ValueError):
            data.export(self.root, data.plan(self.root, ["letters"]), self.root / "export.zip")


if __name__ == "__main__":
    unittest.main()
