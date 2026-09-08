"""Create a local Mouse Wheelie dev copy without Yarn helper-name collisions.

Only the verified 1.16.2+mc1.21.11 binary is accepted. The input is never modified.
The resulting JAR is for named development runtimes, not a release dependency.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
import zipfile

SOURCE_SHA256 = "37a55be6874104a82ec2b06954ffc77e0ec5bbbd09a007dbcae688455e3a9214"
MIXIN = "de/siphalor/mousewheelie/client/mixin/gui/other/MixinRecipeBookWidget.class"
RENAMES = {b"getLeft": b"mouseWheelie$devGetLeft", b"getTop": b"mouseWheelie$devGetTop"}


def rename_helpers(data):
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Not a Java class")
    count = struct.unpack_from(">H", data, 8)[0]
    result = bytearray(data[:10])
    offset, index = 10, 1
    found = {name: 0 for name in RENAMES}
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4,
             12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = data[offset]
        offset += 1
        result.append(tag)
        if tag == 1:
            size = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            value = data[offset:offset + size]
            offset += size
            if value in RENAMES:
                found[value] += 1
                value = RENAMES[value]
            result.extend(struct.pack(">H", len(value)))
            result.extend(value)
        else:
            if tag not in sizes:
                raise ValueError(f"Unsupported constant pool tag {tag}")
            size = sizes[tag]
            result.extend(data[offset:offset + size])
            offset += size
            if tag in (5, 6):
                index += 1
        index += 1
    if any(amount != 1 for amount in found.values()):
        raise ValueError(f"Unexpected helper constants: {found}")
    result.extend(data[offset:])
    return bytes(result)


def prepare(source, destination):
    original = source.read_bytes()
    if hashlib.sha256(original).hexdigest() != SOURCE_SHA256:
        raise ValueError("Unrecognized source JAR; refusing to patch another version")
    if source.resolve() == destination.resolve():
        raise ValueError("Input and output must be different paths")
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(original)) as before, zipfile.ZipFile(output, "w") as after:
        if len(before.namelist()) != len(set(before.namelist())):
            raise ValueError("Duplicate archive entries")
        for entry in before.infolist():
            data = before.read(entry.filename)
            after.writestr(entry, rename_helpers(data) if entry.filename == MIXIN else data)
    patched = output.getvalue()
    with zipfile.ZipFile(io.BytesIO(original)) as before, zipfile.ZipFile(io.BytesIO(patched)) as after:
        changed = [name for name in before.namelist() if before.read(name) != after.read(name)]
        if changed != [MIXIN] or after.testzip() is not None:
            raise ValueError(f"Unexpected archive changes: {changed}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        if destination.read_bytes() != patched:
            raise ValueError("A different output file already exists")
    else:
        with destination.open("xb") as target:
            target.write(patched)
    return {"source": str(source), "sourceSha256": SOURCE_SHA256, "output": str(destination),
            "outputSha256": hashlib.sha256(patched).hexdigest(), "changedEntries": changed,
            "renamedHelpers": {k.decode(): v.decode() for k, v in RENAMES.items()}}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    print(json.dumps(prepare(args.source, args.output), indent=2))
