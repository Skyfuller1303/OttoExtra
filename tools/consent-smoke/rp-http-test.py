"""Real PHP front controllers against a disposable loopback database; no production config."""
from pathlib import Path
import json, os, shutil, socket, subprocess, time, urllib.request, urllib.error

repo = Path(__file__).resolve().parents[2]
source = repo.parent / 'OttoRegions Web'
fixture = repo / 'build/rp-security/http-fixture'
fixture.mkdir(parents=True, exist_ok=True)
if (fixture / 'includes/config.local.php').exists():
    raise RuntimeError('Unexpected fixture override')
for p in (source / 'includes').rglob('*.php'):
    if p.name == 'config.local.php': continue
    target = fixture / p.relative_to(source)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, target)
for relative in ['api/index.php', 'v2/index.php', 'tools/rp-privacy.php']:
    target = fixture / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source / relative, target)
db = 'ottoextra_rp_http_qa_' + str(os.getpid())
state = fixture / ('private-' + str(os.getpid()))
php = ['C:/php/php.exe', '-n', '-d', 'extension_dir=C:/php/ext', '-d', 'extension=php_pdo_mysql.dll', '-d', 'extension=php_mbstring.dll', '-d', 'extension=php_sodium.dll']
definition = "define('OTTO_RP_STATE_DIR', " + repr(state.as_posix()) + ");\n"
(fixture / '.env').write_text(f'DB_HOST=127.0.0.1\nDB_PORT=33317\nDB_NAME={db}\nDB_USER=root\nDB_PASS=local-qa-only\nOTTOEXTRA_ALLOW_HTTP=1\nOTTOEXTRA_SECRETS_DIR={state.as_posix()}/unused\nOTTOEXTRA_JWT_CURRENT=qa:' + 'ab'*32 + '\n', encoding='utf-8')
(fixture / 'router.php').write_text("<?php\n" + definition + """
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
if (str_starts_with($path, '/v2/')) { $_GET['route'] = substr($path, 4); require __DIR__ . '/v2/index.php'; }
else { require __DIR__ . '/api/index.php'; }
""", encoding='utf-8')
connect = f"$p=new PDO('mysql:host=127.0.0.1;port=33317;charset=utf8mb4','root','local-qa-only',[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);"
setup = definition + connect + f"$p->exec('CREATE DATABASE `{db}`');" + """
require __DIR__ . '/includes/rp_backups.php';
rpPrivacyInitialize('2026-09-11 14:10:01'); rpBackupInitialize();
require __DIR__ . '/includes/bootstrap.php';
if (!$pdo instanceof PDO) { throw new RuntimeException($dbError); }
$pdo->exec('ALTER TABLE external_participants ADD COLUMN name VARCHAR(120) NULL');
ensureSchema($pdo, $config); // Upstream normally creates the dynamic name column.
require __DIR__ . '/includes/rp_name_upload.php';
$pdo->exec("INSERT INTO external_participants (entity_key,uuid,name,rp_name) VALUES
('11111111-1111-4111-8111-111111111111','11111111-1111-4111-8111-111111111111','AliceQa','Alice RP'),
('22222222-2222-4222-8222-222222222222','22222222-2222-4222-8222-222222222222','BobQa','Bob RP')");
$lock=rpPrivacyLock(true); $s=rpPrivacyState(); $s['uploads_enabled']=true; rpPrivacySave($s); fclose($lock);
echo json_encode(v2IssueJwt('11111111-1111-4111-8111-111111111111','AliceQa'));
"""
(fixture / 'setup.php').write_text('<?php\n' + setup, encoding='utf-8')
(fixture / 'manage.php').write_text('<?php\n' + definition + "require __DIR__ . '/tools/rp-privacy.php';", encoding='utf-8')
process = None
log = None
passed = 0
def check(condition, message):
    global passed
    if not condition: raise AssertionError(message)
    passed += 1
    print('ok: ' + message)
try:
    result = subprocess.run(php + [str(fixture / 'setup.php')], cwd=fixture, capture_output=True, text=True)
    if result.returncode:
        print(result.stdout, result.stderr); result.check_returncode()
    token = json.loads(result.stdout)['token']
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0)); port = sock.getsockname()[1]
    log = (fixture / 'server.log').open('w', encoding='utf-8')
    process = subprocess.Popen(php + ['-S', f'127.0.0.1:{port}', str(fixture / 'router.php')], cwd=fixture, stdout=log, stderr=log, creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    def request(path, body=None, authenticated=False):
        headers = {'Content-Type': 'application/json'}
        if authenticated: headers['Authorization'] = 'Bearer ' + token
        req = urllib.request.Request(f'http://127.0.0.1:{port}' + path, data=None if body is None else json.dumps(body).encode(), headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=8) as response: return response.status, json.loads(response.read())
        except urllib.error.HTTPError as e: return e.code, json.loads(e.read())
    until = time.monotonic() + 10
    while True:
        try: status, _ = request('/api/index.php?action=community-participant-rp-name', {}); break
        except urllib.error.URLError:
            if time.monotonic() > until: raise
            time.sleep(.1)
    check(status == 410, 'Real legacy controller refuses uploads')
    check(request('/v2/rp-names/observations', {'rp_name': 'Alice New'})[0] == 401, 'Real V2 controller requires token')
    check(request('/v2/rp-names/observations', {'rp_name': 'Alice New'}, True)[0] == 200, 'Real V2 controller accepts verified self update')
    check(request('/v2/rp-names/observations', {'target_player_name': 'BobQa', 'rp_name': 'Bob Suggested'}, True)[0] == 202, 'Real V2 controller queues foreign proposal')
    check(request('/v2/rp-names/observations', {'actor_uuid': '22222222-2222-4222-8222-222222222222', 'rp_name': 'Fake'}, True)[0] == 422, 'Real V2 rejects actor spoofing')
    def manage(*args):
        out = subprocess.run(php + [str(fixture / 'manage.php'), *args], cwd=fixture, capture_output=True, text=True, check=True)
        return json.loads(out.stdout)
    proposals = manage('proposals')
    check(len(proposals) == 1, 'Owner CLI lists queued proposal')
    manage('approve-proposal', '11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222', '--apply')
    check(manage('proposals') == [], 'Owner CLI approval consumes proposal')
    manage('block', '22222222-2222-4222-8222-222222222222', '--apply')
    check(request('/v2/rp-names/observations', {'target_player_name': 'BobQa', 'rp_name': 'Bob Again'}, True)[0] == 403, 'HTTP cannot reimport blocked target')
    manage('restore-begin', '--apply')
    check(request('/api/index.php?action=public-player-compact')[0] == 503, 'Legacy publication closed during restore')
    check(request('/v2/player-compact', authenticated=True)[0] == 503, 'V2 publication closed during restore')
    manage('restore-finish', '--apply')
    check(request('/v2/rp-names/observations', {'rp_name': 'Alice Restored'}, True)[0] == 200, 'Verified writes work after restore validation')
    upstream = fixture / 'upstream'
    upstream.mkdir(exist_ok=True)
    for endpoint in ['factions', 'regions', 'orders', 'wars']:
        (upstream / endpoint).write_text('[]', encoding='utf-8')
    (upstream / 'participants').write_text(json.dumps([
        {'uuid': '11111111-1111-4111-8111-111111111111', 'name': 'AliceQa', 'rp_name': 'Overwrite Attempt', 'rp_name_source': 'legacy'},
        {'uuid': '22222222-2222-4222-8222-222222222222', 'name': 'BobQa', 'rp_name': 'Resurrection Attempt'}]), encoding='utf-8')
    (fixture / 'sync-test.php').write_text('<?php\n' + definition + """
require __DIR__ . '/includes/bootstrap.php';
require __DIR__ . '/includes/public_sync_runner.php';
$before=$pdo->query("SELECT rp_name_expires_at FROM external_participants WHERE name='AliceQa'")->fetchColumn();
$result=runPublicEndpointSync($pdo, $argv[1]);
echo json_encode(['result'=>$result,'before'=>$before,'rows'=>$pdo->query('SELECT name,rp_name,rp_name_expires_at FROM external_participants ORDER BY name')->fetchAll(PDO::FETCH_ASSOC)]);
""", encoding='utf-8')
    synced = subprocess.run(php + [str(fixture / 'sync-test.php'), upstream.as_uri()], cwd=fixture, capture_output=True, text=True, check=True)
    imported = json.loads(synced.stdout)
    check('error' not in imported['result']['summary']['participants'], 'Real participant import completes')
    check(imported['rows'][0]['rp_name'] == 'Alice Restored', 'Import cannot overwrite confirmed name')
    check(imported['rows'][0]['rp_name_expires_at'] == imported['before'], 'Import cannot renew confirmed expiry')
    check(imported['rows'][1]['rp_name'] is None, 'Import cannot resurrect blocked player')
    print(f'PASS: {passed} real PHP HTTP/CLI checks')
finally:
    if process is not None:
        process.terminate()
        try: process.wait(timeout=5)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
    if log: log.close()
    subprocess.run(php + ['-r', connect + f"$p->exec('DROP DATABASE IF EXISTS `{db}`');"], capture_output=True, check=True)
