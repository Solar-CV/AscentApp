#!/usr/bin/env python3
"""把 KHR_materials_emissive_strength 烘焙进 emissiveFactor。

原因：Filament gltfio 对 emissive_strength 的支持不稳定，模型里 6×/8× 的
灯笼、闪光、脉冲在 APK 里就退化成普通亮色块，看起来"没有光效"。
glTF 允许 emissiveFactor 超过 1，因此直接乘上去即可，效果与扩展等价，
而且不依赖 gltfio 是否认识这个扩展。

用法：python3 tools/poc3d/bake_emissive.py app/src/main/assets/badges/*.glb
"""
import json
import struct
import sys
from pathlib import Path

EXT = "KHR_materials_emissive_strength"


def read_glb(data: bytes):
    magic, version, length = struct.unpack("<III", data[:12])
    assert magic == 0x46546C67, "not a GLB"
    off = 12
    chunks = []
    while off < length:
        clen, ctype = struct.unpack("<II", data[off:off + 8])
        body = data[off + 8: off + 8 + clen]
        chunks.append([ctype, body])
        off += 8 + clen
    return chunks


def write_glb(chunks) -> bytes:
    out = bytearray()
    body = bytearray()
    for ctype, data in chunks:
        pad = (-len(data)) % 4
        padded = data + (b" " * pad if ctype == 0x4E4F534A else b"\x00" * pad)
        body += struct.pack("<II", len(padded), ctype) + padded
    out += struct.pack("<III", 0x46546C67, 2, 12 + len(body))
    out += body
    return bytes(out)


def bake(path: Path) -> int:
    chunks = read_glb(path.read_bytes())
    if not chunks or chunks[0][0] != 0x4E4F534A:
        return 0
    doc = json.loads(chunks[0][1].decode("utf-8"))
    changed = 0
    for mat in doc.get("materials", []):
        ext = (mat.get("extensions") or {}).pop(EXT, None)
        if not ext:
            continue
        strength = float(ext.get("emissiveStrength", 1.0))
        if strength == 1.0:
            continue
        base = mat.get("emissiveFactor", [1.0, 1.0, 1.0])
        mat["emissiveFactor"] = [round(c * strength, 6) for c in base]
        if not mat.get("extensions"):
            mat.pop("extensions", None)
        changed += 1
    if not changed:
        return 0
    used = doc.get("extensionsUsed") or []
    if EXT in used and not any(
        EXT in (m.get("extensions") or {}) for m in doc.get("materials", [])
    ):
        used.remove(EXT)
    if used:
        doc["extensionsUsed"] = used
    else:
        doc.pop("extensionsUsed", None)

    chunks[0][1] = json.dumps(doc, separators=(",", ":")).encode("utf-8")
    path.write_bytes(write_glb(chunks))
    return changed


def main():
    total = 0
    for arg in sys.argv[1:]:
        p = Path(arg)
        n = bake(p)
        if n:
            print(f"{p.name}: baked {n} emissive material(s)")
        total += n
    print("materials updated:", total)


if __name__ == "__main__":
    main()
