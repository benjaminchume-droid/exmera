# EXM container v1

`.exm` is Exmera's proposed model container. It is a packaging/runtime format, not a new neural-network architecture.

## Goals

1. Deterministic metadata and integrity verification.
2. Chunked weights for memory-aware loading.
3. Multiple quantization variants.
4. CPU/GPU/NPU compatibility declarations.
5. License provenance carried with every artifact.
6. Forward-compatible versioning.

## Planned layout

```text
magic + version + flags
manifest
model graph
tensor index
chunk index
compressed weight chunks
license metadata
sha-256 integrity data
```

## Quantization

Supported target labels: FP32, FP16, INT8 and INT4. Actual quality/size/runtime results must be measured per model and device. EXM does not claim a fixed compression ratio.

## Streaming

The runtime should be able to locate and decode only the chunks required by an execution plan. It must not assume that the complete model can fit in RAM.
