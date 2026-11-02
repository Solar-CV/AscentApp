#!/usr/bin/env python3
"""16 KB 页兼容性检查（APK / AAB）。

Android 15 起，64 位设备可以配置成 16 KB 内存页；面向 API 35+ 的应用必须能在
这种设备上加载自己的原生库。判定标准有两条，缺一不可：

  1. **ELF 对齐**：每个 `PT_LOAD` 段的 `p_align` 必须 >= 0x4000（16 KB）。
     这是链接期决定的（`-Wl,-z,max-page-size=16384`），NDK r28 起是默认值。
  2. **打包对齐**：`.so` 在 zip 里必须**不压缩**存储，且数据偏移按 16 KB 对齐。
     APK 由 `zipalign -P 16` 保证，AAB 由 AGP 的 native libs 打包策略保证。

任一条不满足，设备就会在 `dlopen` 时失败 —— 表现为「打开徽章 3D 页面直接崩」。
所以这项检查要放在发布前，而不是等用户装上才报。

用法：
    python3 tools/check_16k.py app/build/outputs/apk/release/app-release.apk
    python3 tools/check_16k.py app/build/outputs/bundle/release/app-release.aab

退出码：有任何一条不满足返回 1，并打印具体是哪一层、哪个库。
"""
from __future__ import annotations

import struct
import sys
import zipfile

PT_LOAD = 1
PAGE_16K = 0x4000
ELF64_HEADER_PHOFF = 0x20
ELF64_HEADER_PHENTSIZE = 0x36
ELF64_HEADER_PHNUM = 0x38
ELF64_PHDR_ALIGN_OFFSET = 48


def elf_load_aligns(data: bytes) -> list[int] | None:
    """返回所有 PT_LOAD 段的 p_align；不是 64 位 ELF 时返回 None。"""
    if data[:4] != b"\x7fELF":
        return None
    if data[4] != 2:  # ELFCLASS64
        return None
    (phoff,) = struct.unpack_from("<Q", data, ELF64_HEADER_PHOFF)
    (phentsize,) = struct.unpack_from("<H", data, ELF64_HEADER_PHENTSIZE)
    (phnum,) = struct.unpack_from("<H", data, ELF64_HEADER_PHNUM)
    out: list[int] = []
    for i in range(phnum):
        base = phoff + i * phentsize
        (p_type,) = struct.unpack_from("<I", data, base)
        if p_type == PT_LOAD:
            (p_align,) = struct.unpack_from("<Q", data, base + ELF64_PHDR_ALIGN_OFFSET)
            out.append(p_align)
    return out


def check(path: str) -> int:
    print(f"== {path}")
    try:
        zf = zipfile.ZipFile(path)
    except FileNotFoundError:
        print(f"  找不到文件：{path}")
        return 1
    except zipfile.BadZipFile:
        print(f"  不是 zip（APK/AAB 应该是 zip）：{path}")
        return 1

    libs = [i for i in zf.infolist() if i.filename.endswith(".so")]
    if not libs:
        print("  没有原生库，跳过")
        return 0

    is_bundle = path.endswith(".aab")
    # 直接读整份文件：要用本地文件头算数据偏移，ZipFile 不给这个信息
    with open(path, "rb") as fh:
        raw = fh.read()

    failures = 0
    for info in libs:
        data = zf.read(info.filename)
        name = info.filename.split("/")[-1]
        abi = info.filename.split("/")[1] if info.filename.count("/") >= 2 else "?"

        aligns = elf_load_aligns(data)
        if aligns is None:
            # 32 位库不在这条要求范围内（64 位设备才涉及 16 KB 页）
            print(f"  {name:34s} {abi:12s} 非 64 位，跳过")
            continue

        bad_align = [a for a in aligns if a < PAGE_16K]
        # 本地文件头：30 字节固定部分 + 文件名 + extra。
        # 注意要读**本地头**里的 extra 长度 —— central directory 的 extra 与它不一定相同，
        # 用错会让偏移算歪（第一版就是这么误报成「全部未对齐」的）。
        name_len = struct.unpack_from("<H", raw, info.header_offset + 26)[0]
        extra_len = struct.unpack_from("<H", raw, info.header_offset + 28)[0]
        data_offset = info.header_offset + 30 + name_len + extra_len
        compressed = info.compress_type != zipfile.ZIP_STORED
        misaligned = data_offset % PAGE_16K != 0

        problems = []
        if bad_align:
            problems.append(f"ELF p_align={[hex(a) for a in bad_align]}")
        # AAB 里 .so 压缩存储是正常的：bundletool 生成 APK 时会解压并按 16 KB 对齐。
        # 所以对 AAB 只问 ELF 对齐，不问 zip 偏移。
        if not is_bundle:
            if compressed:
                problems.append("zip 里被压缩存储（APK 需要未压缩）")
            if misaligned:
                problems.append(f"数据偏移 {data_offset} 未按 16 KB 对齐（余 {data_offset % PAGE_16K}）")

        if problems:
            failures += 1
            print(f"  ✗ {name:34s} {abi:12s} " + "；".join(problems))
        else:
            aligns_s = ",".join(hex(a) for a in sorted(set(aligns)))
            tail = "ELF 对齐（AAB 由 bundletool 出包时再对齐 zip）" if is_bundle else "未压缩且已对齐"
            print(f"  ✓ {name:34s} {abi:12s} p_align={aligns_s} {tail}")

    if failures:
        print(f"  失败 {failures} 个库：16 KB 页设备上会加载失败")
        return 1
    print("  全部通过")
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    rc = 0
    for path in sys.argv[1:]:
        rc |= check(path)
        print()
    return rc


if __name__ == "__main__":
    sys.exit(main())
