# Phase 10 — Super Resolution

Phase 10 turns the Phase 9 multi-frame reconstruction into a reusable super-resolution stage.

## Pipeline

```text
CameraX YUV frames
      ↓
bounded RGB/luma conversion
      ↓
motion registration
      ↓
exposure normalization
      ↓
robust multi-frame fusion / denoise
      ↓
CPU reconstruction SR
      ↓
2× or 4× output
```

## Implementation

- `SuperResolutionScale` exposes 2× and 4× output.
- `SuperResolutionConfig` controls scale, tile target, overlap, and restrained sharpening.
- `SuperResolutionBackend` is the stable model boundary for future EXM learned SR models.
- `CpuSuperResolutionBackend` is the guaranteed device-independent fallback.
- Reconstruction uses bicubic interpolation followed by an edge-gated residual sharpen; values are clamped to the valid normalized range.
- `SuperResolutionEngine` selects the first compatible backend and returns backend metadata with the result.
- `ImageQualityMetrics` provides deterministic MAE, PSNR, and edge-energy primitives for regression tests and later device benchmarks.
- The camera capture coordinator now performs Phase 9 fusion followed by 2×/4× super-resolution and releases all `ImageProxy` resources in a `finally` block.

## Why this is not called a learned model

The repository does not yet contain trained Exmera weights. The CPU backend therefore does not pretend to hallucinate learned detail. It reconstructs missing samples deterministically. A future `EXM` backend can implement a trained Real-ESRGAN-class or Exmera-native model behind the same `SuperResolutionBackend` interface without changing the camera API.

## Memory strategy

Phase 9 limits camera working frames to a bounded working dimension. Phase 10 also keeps the super-resolution API backend-based so GPU/NPU and tiled learned inference can replace the CPU implementation without changing callers. Full-resolution/tiled inference remains an explicit optimization target before very large 4× exports are enabled by default on constrained devices.

## Validation

Tests cover:

- 2× dimensions
- 4× dimensions
- normalized output bounds
- constant-image stability
- deterministic quality metrics
- backend selection metadata

## Next quality layer

The next implementation should add Camera2 RAW `ImageReader`, gyro-assisted registration, dense optical flow, exposure bracketing, full-resolution tiled fusion, and a trained EXM super-resolution backend. Those are hardware/model upgrades rather than reasons to weaken this deterministic fallback.
