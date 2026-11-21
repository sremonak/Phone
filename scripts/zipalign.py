#!/usr/bin/env python3
"""Rebuild a ZIP/APK so every STORED entry's data starts on an aligned offset."""
import struct
import sys
import zipfile
import zlib


def align(src_path, dst_path, alignment=4):
    zin = zipfile.ZipFile(src_path, "r")
    out = open(dst_path, "wb")
    central = []
    offset = 0
    for info in zin.infolist():
        data = zin.read(info.filename)
        name = info.filename.encode("utf-8")
        flags = info.flag_bits & 0x800
        if info.is_dir():
            comp = b""
            method = 0
        elif info.compress_type == zipfile.ZIP_STORED:
            comp = data
            method = 0
        else:
            compressor = zlib.compressobj(6, zlib.DEFLATED, -15)
            comp = compressor.compress(data) + compressor.flush()
            method = 8
        crc = zlib.crc32(data) & 0xFFFFFFFF
        t = info.date_time
        dosdate = ((t[0] - 1980) << 9) | (t[1] << 5) | t[2]
        dostime = (t[3] << 11) | (t[4] << 5) | (t[5] // 2)

        data_start = offset + 30 + len(name)
        pad = (alignment - (data_start % alignment)) % alignment
        extra = b"\x00" * pad

        local = struct.pack(
            "<IHHHHHIIIHH",
            0x04034B50, 20, flags, method, dostime, dosdate,
            crc, len(comp), len(data), len(name), len(extra),
        )
        out.write(local)
        out.write(name)
        out.write(extra)
        out.write(comp)

        central.append((
            name, method, flags, dostime, dosdate, crc,
            len(comp), len(data), offset, info.external_attr,
        ))
        offset += len(local) + len(name) + len(extra) + len(comp)

    cd_start = offset
    for entry in central:
        (name, method, flags, dostime, dosdate, crc,
         csize, usize, local_off, ext_attr) = entry
        cd = struct.pack(
            "<IHHHHHHIIIHHHHHII",
            0x02014B50, 0x031E, 20, flags, method, dostime, dosdate,
            crc, csize, usize, len(name), 0, 0, 0, 0, ext_attr, local_off,
        )
        out.write(cd)
        out.write(name)
        offset += len(cd) + len(name)
    cd_size = offset - cd_start
    eocd = struct.pack(
        "<IHHHHIIH",
        0x06054B50, 0, 0, len(central), len(central), cd_size, cd_start, 0,
    )
    out.write(eocd)
    out.close()
    zin.close()


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("usage: zipalign.py <in.apk> <out.apk> [alignment]")
        sys.exit(2)
    alignment = int(sys.argv[3]) if len(sys.argv) > 3 else 4
    align(sys.argv[1], sys.argv[2], alignment)
