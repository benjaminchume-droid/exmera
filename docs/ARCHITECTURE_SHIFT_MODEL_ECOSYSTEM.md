# Exmera Open-Model / EXM Architecture

Exmera no longer treats every AI capability as a model that must be trained internally. The product architecture is now:

```text
Open model/checkpoint
        |
        v
License audit -> conversion -> optimization -> compression
        |
        v
      .EXM
        |
        v
   Exmera Runtime
     /   |   \
   CPU  GPU  NPU
        |
        v
 Camera / Video / Studio / Exllery
```

## Curated redistribution policy

A model is not automatically commercially redistributable because its repository is called open source. Exmera tracks code, checkpoint, dataset, and bundled-asset licensing separately.

### Approved starting catalog

- Real-ESRGAN — BSD-3-Clause repository; package only a checkpoint whose terms have been verified.
- SAM 2 — the upstream project states its model checkpoints, demo code, and training code are Apache-2.0.
- MMPose — Apache-2.0 project; audit selected checkpoint/data terms.
- MMSegmentation — Apache-2.0 project; audit selected model/data terms.
- OpenCV — Apache-2.0 for OpenCV 4.5.0+.

### Review before bundling

PaddleOCR, RAFT, RIFE, Depth Anything, and DINOv2 remain in the review queue until the exact checkpoint and asset terms are recorded.

### Blocked by default

Pretrained InsightFace weights and current Ultralytics YOLO distributions are not bundled until the appropriate commercial redistribution rights are obtained.

## EXM v2

EXM v2 now records:

- model identity/version
- quantization metadata
- CPU/GPU/NPU backend availability
- compression method
- stored SHA-256
- stored and original sizes
- minimum Android/RAM
- license identifier

The runtime verifies the stored payload before decompression and enforces a maximum decompressed payload size.

## Compiler

`tools/exm_compiler.py` produces deterministic EXM v2 packages with DEFLATE compression and integrity metadata. It intentionally does not claim to perform neural-network quantization merely by changing a label. Graph rewriting and true quantization are separate compiler passes and must be implemented with a compatible model-conversion backend.

## Remaining production tracks

1. EXM graph importer/optimizer and true FP16/INT8 conversion.
2. Android GPU/NPU execution backends.
3. Model benchmark matrix and per-device model selection.
4. Camera RAW/gyro/HDR-bracket/full-resolution fusion.
5. Production dense optical flow and temporal video reconstruction.
6. Studio timeline/editor UI and render graph.
7. Exllery indexing/search UI and background indexing.
8. Lens Store distribution, signatures, permissions and sandboxing.
9. Export codec matrix, metadata privacy and crash-safe background rendering.
10. Real-device validation across low/mid/high-tier Android hardware.
11. Release signing, staged rollout, crash/ANR telemetry and certification.

The architecture deliberately allows these tracks to progress independently because all learned components are exposed through EXM model/backend contracts.
