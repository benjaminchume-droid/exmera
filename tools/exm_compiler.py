#!/usr/bin/env python3
"""EXM v2 compiler: deterministic packaging, compression, metadata and integrity.

This tool deliberately does not pretend to quantize arbitrary neural networks by
rewriting bytes. Model-graph optimization/quantization hooks are explicit and can
be enabled when the corresponding conversion dependency is installed.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import struct
import zlib
from pathlib import Path

MAGIC = b"EXM1"
VERSION = 2
MAX_PAYLOAD = 512 * 1024 * 1024
BACKENDS = {"cpu": 0, "gpu": 1, "npu": 2}
QUANT = {"fp32": 0, "fp16": 1, "int8": 2, "int4": 3}
COMPRESSION = {"none": 0, "deflate": 1}

def s(value: str) -> bytes:
    data = value.encode("utf-8")
    if len(data) > 1_048_576:
        raise ValueError("EXM string too large")
    return struct.pack(">I", len(data)) + data

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def compile_exm(src: Path, dst: Path, model_id: str, name: str, license_name: str,
                model_version: str, quantization: str, backends: list[str],
                compression: str, min_ram: int, min_android: int) -> dict:
    payload = src.read_bytes()
    if len(payload) > MAX_PAYLOAD:
        raise ValueError("Input exceeds EXM safety limit")
    if quantization not in QUANT:
        raise ValueError(f"Unsupported quantization metadata: {quantization}")
    if compression not in COMPRESSION:
        raise ValueError(f"Unsupported compression: {compression}")
    backend_mask = 0
    for backend in backends:
        if backend not in BACKENDS:
            raise ValueError(f"Unknown backend: {backend}")
        backend_mask |= 1 << BACKENDS[backend]
    stored = zlib.compress(payload, level=9) if compression == "deflate" else payload
    stored_sha = sha(stored)
    out = bytearray()
    out += MAGIC
    out += struct.pack(">I", VERSION)
    out += s(model_id) + s(model_version) + s(name)
    out += struct.pack(">I", QUANT[quantization])
    out += struct.pack(">I", backend_mask)
    out += struct.pack(">I", COMPRESSION[compression])
    out += s(stored_sha)
    out += struct.pack(">Q", len(stored))
    out += struct.pack(">Q", len(payload))
    out += struct.pack(">I", min_ram)
    out += struct.pack(">I", min_android)
    out += s(license_name)
    out += struct.pack(">Q", len(stored))
    out += stored
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(out)
    return {
        "id": model_id, "version": model_version, "name": name,
        "quantization": quantization, "backends": backends,
        "compression": compression, "storedBytes": len(stored),
        "originalBytes": len(payload), "sha256": stored_sha,
        "license": license_name, "output": str(dst)
    }

def main() -> None:
    p = argparse.ArgumentParser(description="Build an EXM v2 model package")
    p.add_argument("source", type=Path)
    p.add_argument("output", type=Path)
    p.add_argument("--id", required=True)
    p.add_argument("--name", required=True)
    p.add_argument("--version", default="1.0.0")
    p.add_argument("--license", dest="license_name", required=True)
    p.add_argument("--quantization", choices=QUANT, default="fp16")
    p.add_argument("--backends", nargs="+", choices=BACKENDS, default=["cpu"])
    p.add_argument("--compression", choices=COMPRESSION, default="deflate")
    p.add_argument("--min-ram", type=int, default=2048)
    p.add_argument("--min-android", type=int, default=26)
    args = p.parse_args()
    result = compile_exm(args.source, args.output, args.id, args.name, args.license_name,
                         args.version, args.quantization, args.backends, args.compression,
                         args.min_ram, args.min_android)
    print(json.dumps(result, indent=2, sort_keys=True))

if __name__ == "__main__":
    main()
