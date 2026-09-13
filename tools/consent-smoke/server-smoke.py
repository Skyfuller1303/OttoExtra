"""Start a localhost Fabric server, verify startup, stop via console, retain logs."""
from pathlib import Path
import subprocess
import queue
import threading
import time

root = Path(__file__).resolve().parents[2]
log = root / "build/privacy-qa/dedicated-server-run.log"
command = ["cmd.exe", "/d", "/c", "gradlew.bat", "-I", "tools/consent-smoke/server.init.gradle", "runServer", "-x", "dist", "--console=plain"]
p = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
lines = queue.Queue()
def read():
    for line in p.stdout:
        lines.put(line)
    lines.put(None)
threading.Thread(target=read, daemon=True).start()
started = False
stopped = False
success = False
deadline = time.monotonic() + 180
try:
    with log.open("w", encoding="utf-8") as out:
        while time.monotonic() < deadline:
            try:
                line = lines.get(timeout=1)
            except queue.Empty:
                continue
            if line is None:
                break
            out.write(line); out.flush()
            if 'Done (' in line and not started:
                started = True
                p.stdin.write('stop\n'); p.stdin.flush()
            stopped |= 'Stopping server' in line
            success |= 'BUILD SUCCESSFUL' in line
        if p.poll() is None:
            p.wait(timeout=15)
    if p.returncode != 0 or not (started and stopped and success):
        raise RuntimeError(f"Dedicated server failed: exit={p.returncode}, started={started}, stopped={stopped}")
    print("PASS: localhost dedicated Fabric server started and stopped cleanly with client-only OttoExtra jar in mods")
finally:
    if p.poll() is None:
        subprocess.run(["taskkill", "/pid", str(p.pid), "/t", "/f"], capture_output=True)
