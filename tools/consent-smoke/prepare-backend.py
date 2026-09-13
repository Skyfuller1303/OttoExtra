"""Prepare a synthetic localhost SQL fixture without loading production configuration."""
from pathlib import Path
import shutil

repo = Path(__file__).resolve().parents[2]
source = repo.parent / "OttoRegions Web"
fixture = repo / "build/privacy-qa/backend-fixture"
if not fixture.resolve().is_relative_to(repo.resolve()):
    raise RuntimeError("Fixture must remain inside the OttoExtra repository")
if (fixture / "includes/config.local.php").exists():
    raise RuntimeError("Fixture contains config.local.php; refuse a potentially non-test database override")
fixture.mkdir(parents=True, exist_ok=True)
for folder in ("includes/v2", "sql/migrations"):
    shutil.copytree(source / folder, fixture / folder, dirs_exist_ok=True)
for relative in ("tools/v2-animal-breeding-db.php", "resources/lehen_polygons.json"):
    target = fixture / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source / relative, target)
# Only this fixture file supplies credentials to the copied CLI runner.
(fixture / ".env").write_text("DB_HOST=127.0.0.1\nDB_PORT=33317\nDB_NAME=ottoextra_privacy_qa\nDB_USER=root\nDB_PASS=local-qa-only\n")
(fixture / "setup.php").write_text("""<?php
$port=(int)($argv[1] ?? 33317);
if (!in_array($port,[33316,33317],true)) throw new RuntimeException('QA loopback port required');
$pdo=new PDO("mysql:host=127.0.0.1;port={$port};dbname=ottoextra_privacy_qa;charset=utf8mb4",'root','local-qa-only',[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$pdo->exec('CREATE TABLE IF NOT EXISTS external_participants (entity_key VARCHAR(128) PRIMARY KEY, uuid CHAR(36), faction VARCHAR(128)) ENGINE=InnoDB');
$pdo->exec('CREATE TABLE IF NOT EXISTS external_factions (entity_key VARCHAR(128) PRIMARY KEY, region VARCHAR(128), uuid CHAR(36)) ENGINE=InnoDB');
echo "Isolated QA fixture schema ready\\n";
""")
print(f"Prepared {fixture}; production .env/config.local.php not copied")
