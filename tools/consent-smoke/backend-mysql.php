<?php
/** Isolated SQL regression checks. Credentials below belong only to disposable localhost containers. */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') exit(1);
$root = realpath(__DIR__ . '/../../build/privacy-qa/backend-fixture');
if ($root === false || basename($root) !== 'backend-fixture') throw new RuntimeException('QA fixture missing');
$port = (int) ($argv[1] ?? 33316);
if (!in_array($port, [33316, 33317], true)) throw new RuntimeException('Only QA loopback ports permitted');
$env = [];
function envValue(array $unused, string $key, string $default = ''): string {
    global $root;
    return match ($key) {
        'OTTOEXTRA_SECRETS_DIR' => $root . '/nonexistent-fixture-secrets',
        'OTTOEXTRA_JWT_CURRENT' => 'qa:' . str_repeat('ab', 32),
        'OTTOEXTRA_CHAT_TRANSLATION_KEY' => base64_encode(str_repeat('Q', 32)),
        'OTTOEXTRA_CHAT_TRANSLATION_TTL' => '60',
        default => $default,
    };
}
require $root . '/includes/v2/auth.php';
require $root . '/includes/v2/chat_translations.php';
$pdo = new PDO("mysql:host=127.0.0.1;port={$port};dbname=ottoextra_privacy_qa;charset=utf8mb4", 'root', 'local-qa-only', [PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC]);

// Both independent PHP processes finish SELECT before either DELETE executes.
if (($argv[2] ?? '') === 'worker') {
    $barrier = $argv[4]; $worker = $argv[5];
    if (!str_starts_with($barrier, $root . DIRECTORY_SEPARATOR . 'race-') || !in_array($worker, ['a','b'], true)) exit(2);
    class RaceStatement extends PDOStatement {
        protected function __construct() {}
        public function fetch(int $mode = PDO::FETCH_DEFAULT, int $cursorOrientation = PDO::FETCH_ORI_NEXT, int $cursorOffset = 0): mixed {
            $row = parent::fetch($mode, $cursorOrientation, $cursorOffset);
            if (str_contains($this->queryString, 'SELECT username, uuid, created_at FROM auth_challenges')) {
                global $barrier, $worker;
                file_put_contents($barrier . DIRECTORY_SEPARATOR . $worker . '.ready', 'ready');
                $until = microtime(true) + 10;
                while (count(glob($barrier . DIRECTORY_SEPARATOR . '*.ready') ?: []) < 2) {
                    if (microtime(true) > $until) throw new RuntimeException('Race barrier timeout');
                    usleep(10000);
                }
            }
            return $row;
        }
    }
    $pdo->setAttribute(PDO::ATTR_STATEMENT_CLASS, [RaceStatement::class]);
    echo v2ConsumeChallenge($pdo, $argv[3]) !== null ? '1' : '0';
    exit;
}
$passed = 0; $failed = 0;
function check(bool $ok, string $label): void { global $passed, $failed; $ok ? $passed++ : $failed++; echo ($ok ? 'PASS: ' : 'FAIL: ') . $label . "\n"; }
$alice='11111111-1111-4111-8111-111111111111'; $bob='22222222-2222-4222-8222-222222222222'; $eve='33333333-3333-4333-8333-333333333333';
v2EnsureSchema($pdo);
foreach (['auth_challenges','revoked_tokens','banned_uuids'] as $table) $pdo->exec("DELETE FROM {$table}");
check(v2ValidateJwt($pdo, null)['error'] === 'token_missing', 'Missing authentication denied');
$issued=v2IssueJwt($alice, 'AliceQa');
$jwt=$issued['token'];
$valid=v2ValidateJwt($pdo, 'Bearer '.$jwt);
check(($valid['ok'] ?? false) && $valid['uuid']===$alice, 'Signed token identifies Alice');
check(!(v2ValidateJwt($pdo, 'Bearer '.substr($jwt,0,-2).'xx')['ok'] ?? false), 'Altered token rejected');
$pdo->prepare('INSERT INTO revoked_tokens VALUES (?, UTC_TIMESTAMP())')->execute([$valid['jti']]);
check(v2ValidateJwt($pdo, 'Bearer '.$jwt)['error']==='token_revoked', 'Revocation applies to existing token');
$pdo->exec('DELETE FROM revoked_tokens');
$pdo->prepare('INSERT INTO banned_uuids VALUES (?, ?, UTC_TIMESTAMP())')->execute([$alice,'QA fixture']);
check(v2ValidateJwt($pdo, 'Bearer '.$jwt)['error']==='banned', 'Ban applies to existing token');
$pdo->exec('DELETE FROM banned_uuids');
$id=v2CreateChallenge($pdo, 'AliceQa', $alice);
check(v2ConsumeChallenge($pdo,$id)['uuid']===$alice && v2ConsumeChallenge($pdo,$id)===null,'Sequential challenge replay rejected');
$old=v2CreateChallenge($pdo,'AliceQa',$alice);
$pdo->prepare('UPDATE auth_challenges SET created_at=DATE_SUB(UTC_TIMESTAMP(), INTERVAL 3 MINUTE) WHERE server_id=?')->execute([$old]);
check(v2ConsumeChallenge($pdo,$old)===null,'Expired challenge rejected');
$old=v2CreateChallenge($pdo,'AliceQa',$alice);
$pdo->prepare('UPDATE auth_challenges SET created_at=DATE_SUB(UTC_TIMESTAMP(), INTERVAL 3 MINUTE) WHERE server_id=?')->execute([$old]);
v2CreateChallenge($pdo,'BobQa',$bob);
check((int)$pdo->query("SELECT COUNT(*) FROM auth_challenges WHERE server_id=".$pdo->quote($old))->fetchColumn()===0,'New challenge deletes old challenge rows');
$id=v2CreateChallenge($pdo,'AliceQa',$alice);
$barrier=$root . DIRECTORY_SEPARATOR . 'race-' . bin2hex(random_bytes(5)); mkdir($barrier);
$children=[];
foreach (['a','b'] as $worker) {
    $command=[PHP_BINARY,'-n','-d','extension_dir=C:/php/ext','-d','extension=php_pdo_mysql.dll','-d','extension=php_sodium.dll',__FILE__,(string)$port,'worker',$id,$barrier,$worker];
    $pipes=[];
    $proc=proc_open($command,[0=>['pipe','r'],1=>['file',$barrier.'/'.$worker.'.result','w'],2=>['file',$barrier.'/'.$worker.'.error','w']],$pipes);
    if (!is_resource($proc)) throw new RuntimeException('Worker failed to start');
    fclose($pipes[0]); $children[$worker]=$proc;
}
$winners=0;
foreach ($children as $worker=>$proc) {
    check(proc_close($proc)===0,'Concurrent challenge worker '.$worker.' completed');
    $winners+=(int)file_get_contents($barrier.'/'.$worker.'.result');
}
check($winners===1,'Concurrent challenge has exactly one winner (actual='.$winners.')');
$sql=file_get_contents($root.'/sql/migrations/20260719_chat_translation_shares.sql');
foreach (preg_split('/;\s*(?:\r?\n|$)/',trim($sql)) ?: [] as $statement) if (trim($statement)!=='') $pdo->exec($statement);
$pdo->exec('DELETE FROM chat_translation_shares');
$share=v2ChatCreateShare($pdo,$alice,'Private QA text','allowlist',[$bob],['Translation QA']);
check($share!==null,'Create encrypted allowlist share');
check(v2ChatGetShare($pdo,$share['id'],$alice)['status']==='ok','Owner reads own share');
check(v2ChatGetShare($pdo,$share['id'],$bob)['status']==='ok','Allowed recipient reads share');
check(v2ChatGetShare($pdo,$share['id'],$eve)['status']==='forbidden','Third account cannot read private share');
check(count(v2ChatInbox($pdo,$bob))===1 && count(v2ChatInbox($pdo,$eve))===0,'Inbox enforces recipient separation');
$stored=$pdo->query('SELECT original_ciphertext FROM chat_translation_shares')->fetchColumn();
check(!str_contains($stored,'Private QA text'),'Original text is encrypted in SQL storage');
$pdo->prepare('UPDATE chat_translation_shares SET expires_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 SECOND) WHERE share_id=?')->execute([$share['id']]);
check(v2ChatGetShare($pdo,$share['id'],$bob)['status']==='expired' && count(v2ChatInbox($pdo,$bob))===0,'Expired share inaccessible before cleanup');
check(v2ChatCleanupExpired($pdo)===1,'Cleanup removes expired share');
check((int)$pdo->query('SELECT COUNT(*) FROM chat_translation_messages')->fetchColumn()===0 && (int)$pdo->query('SELECT COUNT(*) FROM chat_translation_recipients')->fetchColumn()===0,'Cleanup cascades to messages and recipients');
$live=v2ChatCreateShare($pdo,$alice,'Live QA text','allowlist',[$bob]);
check(v2ChatCleanupExpired($pdo)===0 && v2ChatGetShare($pdo,$live['id'],$bob)['status']==='ok','Cleanup preserves unexpired share');
if ($port === 33317) {
    require $root . '/includes/v2/animal_breeding.php';
    $pdo->exec('DELETE FROM animal_breeding_observations');
    $pdo->exec('DELETE FROM animal_breeding_horses');
    $now=(int)floor(microtime(true)*1000);
    $cutoff=$now-60*24*60*60*1000;
    $insert=$pdo->prepare('INSERT INTO animal_breeding_horses (entity_uuid, observed_at_ms, source_updated_at_ms, created_by_uuid, updated_by_uuid) VALUES (?, ?, ?, ?, ?)');
    $observation=$pdo->prepare('INSERT INTO animal_breeding_observations (entity_uuid, observed_at_ms, actor_uuid) VALUES (?, ?, ?)');
    $ids=[];
    for ($i=0;$i<502;$i++) {
        $id='44444444-4444-4444-8444-'.str_pad(dechex($i),12,'0',STR_PAD_LEFT); $ids[]=$id;
        $observed=$i===501 ? $cutoff+1 : $cutoff;
        $insert->execute([$id,$observed,$observed,$alice,$alice]);
        $observation->execute([$id,$observed,$alice]);
    }
    $pdo->prepare('INSERT INTO animal_breeding_finder_targets (player_uuid, entity_uuid) VALUES (?, ?)')->execute([$alice,$ids[0]]);
    $pdo->exec("CREATE TRIGGER qa_retention_failure BEFORE DELETE ON animal_breeding_horses FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA simulated interruption'");
    try {
        $thrown=false;
        try { v2AnimalBreedingCleanupStaleHorses($pdo,$now); } catch (PDOException $e) { $thrown=true; }
        check($thrown && (int)$pdo->query('SELECT COUNT(*) FROM animal_breeding_observations')->fetchColumn()===502, 'Interrupted retention batch rolls back observation deletion');
    } finally { $pdo->exec('DROP TRIGGER qa_retention_failure'); }
    check(v2AnimalBreedingCleanupStaleHorses($pdo,$now)===501,'Retention deletes 501 expired horses across two batches');
    check((int)$pdo->query('SELECT COUNT(*) FROM animal_breeding_observations')->fetchColumn()===1,'Expired observations removed; fresh one remains');
    check((int)$pdo->query('SELECT COUNT(*) FROM animal_breeding_finder_targets')->fetchColumn()===0,'Finder foreign key cascades after horse deletion');
    check((int)$pdo->query('SELECT COUNT(*) FROM animal_breeding_horses')->fetchColumn()===1 && v2AnimalBreedingCleanupStaleHorses($pdo,$now)===0,'60-day boundary, fresh row and repeat cleanup behave correctly');
}
echo "{$passed} passed, {$failed} failed; SQL=".$pdo->query('SELECT VERSION()')->fetchColumn()."\n";
exit($failed===0 ? 0 : 1);
