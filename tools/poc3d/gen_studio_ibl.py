#!/usr/bin/env python3
"""生成 POC 用的棚拍环境光 KTX1 cubemap（含 mip 链）并计算辐照度 SH。

为什么要自己生成：徽章材质几乎全是 metallic=1，没有反射环境就会渲染成黑块。
Filament 的 cmgen 是 C++ CLI，这里用等价的最小实现：
  1. 六面 64x64 RGBA8（sRGB 编码）
  2. mip 链用盒式滤波近似预滤波（光滑环境下与 cmgen 结果差异很小）
  3. 9 系数（3 band）辐照度 SH，供 IndirectLight.irradiance() 使用
"""
import math
import struct
import sys
from pathlib import Path

N = 64
FACES = N.bit_length() - 1        # 64 -> 7 级 mip（64,32,16,8,4,2,1）

# ---- 环境定义：暗棚 + 主光 + 补光 + 顶部柔光 + 地面反光 -------------------
KEY_DIR = (-0.45, 0.55, 0.70)
FILL_DIR = (0.80, 0.10, 0.35)
TOP_DIR = (0.05, 1.0, 0.15)
BOUNCE_DIR = (0.0, -1.0, 0.25)


def norm(v):
    l = math.sqrt(sum(c * c for c in v))
    return tuple(c / l for c in v)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def smooth_lobe(d, center, radius):
    """余弦幂次近似的软光源包络。"""
    c = max(0.0, dot(d, center))
    return max(0.0, c - math.cos(radius)) / max(1e-6, 1.0 - math.cos(radius))


def radiance(d):
    y = d[1]
    # 背景：上冷下暖的极暗渐变
    t = (y + 1.0) * 0.5
    r = 0.030 + 0.070 * t
    g = 0.038 + 0.085 * t
    b = 0.052 + 0.110 * t
    # 主光：暖白，左下前方
    k = smooth_lobe(d, norm(KEY_DIR), 0.58) ** 2.0 * 1.00
    r += k * 1.00; g += k * 0.94; b += k * 0.84
    # 补光：冷蓝，右侧
    f = smooth_lobe(d, norm(FILL_DIR), 0.70) ** 1.8 * 0.46
    r += f * 0.55; g += f * 0.75; b += f * 1.00
    # 顶部柔光：中性，给金属一个从上往下的亮度梯度
    tp = smooth_lobe(d, norm(TOP_DIR), 1.20) ** 1.5 * 0.62
    r += tp * 0.92; g += tp * 0.96; b += tp * 1.00
    # 地面反光：暖褐极弱
    bnc = smooth_lobe(d, norm(BOUNCE_DIR), 0.90) ** 1.8 * 0.16
    r += bnc * 1.00; g += bnc * 0.70; b += bnc * 0.45
    return min(r, 1.0), min(g, 1.0), min(b, 1.0)


def face_dir(face, u, v):
    """u,v ∈ [-1,1]，按 GL cubemap 约定给出方向。"""
    if face == 0:   return (1.0, -v, -u)      # +X
    if face == 1:   return (-1.0, -v, u)      # -X
    if face == 2:   return (u, 1.0, v)        # +Y
    if face == 3:   return (u, -1.0, -v)      # -Y
    if face == 4:   return (u, -v, 1.0)       # +Z
    return (-u, -v, -1.0)                     # -Z


def srgb_encode(x):
    x = max(0.0, min(1.0, x))
    return 12.92 * x if x <= 0.0031308 else 1.055 * (x ** (1 / 2.4)) - 0.055


def gen_faces():
    faces = []
    for f in range(6):
        px = []
        for j in range(N):
            v = (j + 0.5) / N * 2 - 1
            for i in range(N):
                u = (i + 0.5) / N * 2 - 1
                d = norm(face_dir(f, u, v))
                r, g, b = radiance(d)
                px.append((d, r, g, b))
        faces.append(px)
    return faces


SH_BASIS = [
    lambda x, y, z: 0.282095,
    lambda x, y, z: 0.488603 * y,
    lambda x, y, z: 0.488603 * z,
    lambda x, y, z: 0.488603 * x,
    lambda x, y, z: 1.092548 * x * y,
    lambda x, y, z: 1.092548 * y * z,
    lambda x, y, z: 0.315392 * (3 * z * z - 1),
    lambda x, y, z: 1.092548 * x * z,
    lambda x, y, z: 0.546274 * (x * x - y * y),
]
CONV = [math.pi, 2 * math.pi / 3, 2 * math.pi / 3, 2 * math.pi / 3,
        math.pi / 4, math.pi / 4, math.pi / 4, math.pi / 4, math.pi / 4]


def spherical_harmonics(faces):
    dw = 4 * math.pi / (6 * N * N)
    acc = [[0.0, 0.0, 0.0] for _ in range(9)]
    for face in faces:
        for d, r, g, b in face:
            for i, basis in enumerate(SH_BASIS):
                y = basis(*d)
                acc[i][0] += r * y * dw
                acc[i][1] += g * y * dw
                acc[i][2] += b * y * dw
    out = []
    for i in range(9):
        l = 0 if i == 0 else (1 if i < 4 else 2)
        out.extend([acc[i][0] * CONV[l], acc[i][1] * CONV[l], acc[i][2] * CONV[l]])
    return out


def downsample(face):
    """盒式滤波降采样一个面（N x N 像素列表 -> N/2）。"""
    half = int(math.sqrt(len(face))) // 2
    out = []
    w = half * 2
    for j in range(half):
        for i in range(half):
            acc_r = acc_g = acc_b = 0.0
            dsum = [0.0, 0.0, 0.0]
            for dj in range(2):
                for di in range(2):
                    d, r, g, b = face[(j * 2 + dj) * w + (i * 2 + di)]
                    acc_r += r; acc_g += g; acc_b += b
                    dsum[0] += d[0]; dsum[1] += d[1]; dsum[2] += d[2]
            d = norm(tuple(c / 4 for c in dsum))
            out.append((d, acc_r / 4, acc_g / 4, acc_b / 4))
    return out


def write_ktx(path, faces):
    levels = [faces]
    cur = faces
    while len(cur[0]) > 1:
        cur = [downsample(f) for f in cur]
        levels.append(cur)

    buf = bytearray()
    buf += bytes([0xAB, 0x4B, 0x54, 0x58, 0x20, 0x31, 0x31, 0xBB, 0x0D, 0x0A, 0x1A, 0x0A])
    buf += struct.pack("<13I",
                       0x04030201,   # endianness
                       0x1401,       # GL_UNSIGNED_BYTE
                       1,            # glTypeSize
                       0x1908,       # GL_RGBA
                       0x8058,       # GL_RGBA8
                       0x1908,       # base internal format
                       N, N, 0, 0, 6, len(levels), 0)
    for lvl in levels:
        size = len(lvl[0]) * 4
        buf += struct.pack("<I", size)
        for f in lvl:
            data = bytearray()
            for _d, r, g, b in f:
                data += bytes((int(srgb_encode(r) * 255 + 0.5),
                               int(srgb_encode(g) * 255 + 0.5),
                               int(srgb_encode(b) * 255 + 0.5), 255))
            buf += data
            pad = (-size) % 4
            if pad:
                buf += b"\x00" * pad
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_bytes(bytes(buf))
    return len(buf), len(levels)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/env/studio_ibl.ktx"
    faces = gen_faces()
    sh = spherical_harmonics(faces)
    size, levels = write_ktx(out, faces)
    print(f"wrote {out}  {size} bytes  mips={levels}")
    print("SH (9x3, linear):")
    for i in range(9):
        print("    %.6ff, %.6ff, %.6ff," % (sh[i * 3], sh[i * 3 + 1], sh[i * 3 + 2]))


if __name__ == "__main__":
    main()
