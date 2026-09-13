#!/usr/bin/env python3
"""Offline OttoExtra inventory/export/deletion. Nothing is deleted without an unchanged plan."""
import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import sys
import time
import zipfile

PREFIX = "config/ottoextra/"
CATEGORIES = ("chat", "cache", "people", "letters", "backups")
RETENTION_DAYS = {"chat": 30, "cache": 30}


def category(relative):
    if relative == "logs/ottoextra-chat.log" or re.fullmatch(
            r"logs/chat/ottoextra-chat-\d{4}-\d{2}-\d{2}-\d+\.log", relative):
        return "chat"
    if relative.startswith((PREFIX + "backups/", PREFIX + "rpnames/backups/")):
        return "backups"
    if relative.startswith((PREFIX + "letters/", PREFIX + "drafts/", PREFIX + ".cache/letters/")) \
            or relative == "config/ottoletter-draft.json":
        return "letters"
    if relative.startswith(PREFIX + "rpnames/") or relative in (
            "config/ottonames.json", "config/ottochat-rpnames.json", "config/ottochat-rpnames-cache.json"):
        return "people"
    if relative.startswith(PREFIX + "cache/") or relative == "config/ottoletter-player-cache.json":
        return "cache"
    return None


def linked(path):
    info = path.lstat()
    return stat.S_ISLNK(info.st_mode) or bool(getattr(info, "st_file_attributes", 0) & 0x400)


def safe_path(root, relative):
    name = PurePosixPath(relative)
    if relative != name.as_posix() or not relative or name.is_absolute() or any(p in ("..", ".") or ":" in p or "\\" in p for p in name.parts):
        raise ValueError("Invalid relative path")
    current = root
    for part in name.parts:
        current = current / part
        if current.exists() or current.is_symlink():
            if linked(current):
                raise ValueError("Links/junctions are excluded: " + relative)
    if not current.resolve().is_relative_to(root):
        raise ValueError("Path leaves game directory")
    return current


@contextlib.contextmanager
def offline_lock(root):
    path = safe_path(root, PREFIX + ".privacy-data.lock")
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a+b") as handle:
        if path.stat().st_size == 0:
            handle.write(b"\0")
            handle.flush()
        handle.seek(0)
        try:
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.lockf(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB, 1)
        except OSError as error:
            raise RuntimeError("Close every Minecraft instance using this directory first.") from error
        try:
            yield
        finally:
            handle.seek(0)
            if os.name == "nt":
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.lockf(handle.fileno(), fcntl.LOCK_UN, 1)


def inventory(root):
    roots = [PREFIX + p for p in ("cache", "rpnames", "letters", "drafts", ".cache/letters", "backups")]
    roots += ["logs/chat", "logs/ottoextra-chat.log", "config/ottoletter-draft.json",
              "config/ottoletter-player-cache.json", "config/ottonames.json",
              "config/ottochat-rpnames.json", "config/ottochat-rpnames-cache.json"]
    result = []
    for relative in roots:
        path = safe_path(root, relative)
        if not path.exists():
            continue
        if path.is_file():
            paths = [path]
        else:
            paths = []
            for directory, dirs, files in os.walk(path, followlinks=False):
                for name in dirs + files:
                    if linked(Path(directory) / name):
                        raise ValueError("Links/junctions are excluded: " + str(Path(directory) / name))
                paths.extend(Path(directory) / name for name in files)
                if len(paths) > 100_000:
                    raise ValueError("Too many local files; narrow the inventory manually")
        for file in paths:
            relative = file.relative_to(root).as_posix()
            kind = category(relative)
            if kind:
                info = file.stat()
                if not stat.S_ISREG(info.st_mode):
                    raise ValueError("Non-regular data file: " + relative)
                result.append({"path": relative, "category": kind, "bytes": info.st_size,
                               "modified_ns": info.st_mtime_ns})
    return sorted(result, key=lambda item: item["path"])


def digest(path):
    with path.open("rb") as handle:
        return hashlib.file_digest(handle, "sha256").hexdigest()


def select(items, categories, expired=False, now=None):
    now = time.time() if now is None else now
    return [item for item in items if item["category"] in categories and (not expired or
            item["category"] in RETENTION_DAYS and now - item["modified_ns"] / 1e9 >
            RETENTION_DAYS[item["category"]] * 86400)]


def plan(root, categories, expired=False):
    files = select(inventory(root), categories, expired)
    for item in files:
        item["sha256"] = digest(safe_path(root, item["path"]))
    return {"version": 1, "game_dir": str(root), "categories": sorted(categories), "files": files}


def validate(root, document):
    if document.get("version") != 1 or document.get("game_dir") != str(root):
        raise ValueError("Plan belongs to a different game directory/version")
    files = document["files"]
    seen = set()
    for item in files:
        name = item["path"]
        if name in seen or category(name) not in document["categories"] or category(name) is None:
            raise ValueError("Invalid or duplicate plan entry")
        seen.add(name)
        file = safe_path(root, name)
        info = file.stat()
        if not file.is_file() or info.st_size != item["bytes"] or info.st_mtime_ns != item["modified_ns"] \
                or digest(file) != item["sha256"]:
            raise ValueError("File changed since preview; create a new plan: " + name)
    return files


def export(root, document, target):
    files = validate(root, document)
    target = target.absolute()
    if target.resolve().is_relative_to(root):
        raise ValueError("Choose an export destination outside the game directory")
    # x: never replace an existing export. ZIP is sensitive, unencrypted, and never uploaded.
    with zipfile.ZipFile(target, "x", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("ottoextra-export-manifest.json", json.dumps(document, ensure_ascii=False, indent=2))
        for item in files:
            archive.write(safe_path(root, item["path"]), item["path"])
    # Verify every exported payload before a user can rely on the copy.
    with zipfile.ZipFile(target) as archive:
        for item in files:
            with archive.open(item["path"]) as stream:
                if hashlib.file_digest(stream, "sha256").hexdigest() != item["sha256"]:
                    raise ValueError("Export changed during copy; retain the originals")


def delete(root, document):
    files = validate(root, document)  # Whole plan validated before the first removal.
    for item in files:
        file = safe_path(root, item["path"])
        if digest(file) != item["sha256"]:
            raise ValueError("File changed during deletion; stopped")
        file.unlink()  # Explicit regular files only; never recursive directory removal.
    return len(files)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-dir", type=Path, required=True)
    parser.add_argument("action", choices=("list", "plan", "export", "delete"))
    parser.add_argument("--categories", nargs="+", choices=CATEGORIES)
    parser.add_argument("--expired", action="store_true", help="Only chat/cache files older than 30 days")
    parser.add_argument("--plan", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--confirm-plan-sha256", help="Required for deletion: hash of the reviewed plan file")
    args = parser.parse_args()
    root = args.game_dir.resolve(strict=True)
    if not (root / "config" / "ottoextra").is_dir():
        parser.error("Not an OttoExtra game directory")
    with offline_lock(root):
        if args.action == "list":
            print(json.dumps(inventory(root), ensure_ascii=False, indent=2))
            return
        if args.action == "plan":
            if not args.categories or not args.output:
                parser.error("plan requires --categories and --output")
            if args.output.resolve().is_relative_to(root):
                parser.error("Save the plan outside the game directory")
            document = plan(root, args.categories, args.expired)
            with args.output.open("x", encoding="utf-8") as handle:
                json.dump(document, handle, ensure_ascii=False, indent=2)
            print(f"{len(document['files'])} files; review {args.output}")
            print("Plan SHA-256: " + digest(args.output))
            print("Backups/exports may retain personal data and credentials. Live config/tokens, Vanilla logs and server data are excluded.")
            return
        if not args.plan:
            parser.error("export/delete requires --plan")
        raw = args.plan.read_bytes()
        document = json.loads(raw)
        if args.action == "export":
            if not args.output:
                parser.error("export requires --output")
            export(root, document, args.output)
            print("Local unencrypted export verified: " + str(args.output))
        else:
            if args.confirm_plan_sha256 != hashlib.sha256(raw).hexdigest():
                parser.error("Deletion requires the SHA-256 of the reviewed plan")
            print(f"Deleted {delete(root, document)} selected files; no automatic backup created.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, KeyError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
