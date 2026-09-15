# Phase 11 — Pixel Creator

Phase 11 adds the first complete Pixel Creator reconstruction layer to Exmera. It sits after multi-frame fusion and super-resolution and is designed as a backend boundary so a trained `.exm` model can replace the deterministic CPU implementation later without changing the camera graph.

## Pipeline

```text
Captured frames
    ↓
Motion alignment
    ↓
Robust multi-frame fusion
    ↓
2× / 4× super-resolution
    ↓
Pixel Creator
 ┌───────────────┐
 │ image analysis│
 │ structure map │
 │ texture map   │
 │ detail map    │
 │ reconstruction│
 │ color preserve│
 └───────────────┘
    ↓
Final bitmap
```

## Implemented

- `PixelSemanticAnalyzer` computes deterministic structure, texture, and detail confidence maps from the actual image.
- `PixelCreatorBackend` is the inference boundary for future EXM models.
- `CpuPixelCreatorBackend` provides a bounded, deterministic reconstruction fallback using multi-scale local fields, edge residual recovery, local contrast, and conservative denoising.
- `PixelCreatorEngine` performs analysis, backend selection, reconstruction, and an objective reconstruction-preservation score.
- Camera capture processing now invokes Pixel Creator after Phase 9 fusion and Phase 10 super-resolution.
- Pixel values are clamped to the valid `[0, 1]` range.
- Constant images remain stable, avoiding artificial detail on flat content.
- Tests cover dimensions, bounds, semantic maps, constant-image stability, and backend substitution.

## Important model boundary

The CPU backend does **not** claim to be a learned generative model. It only reconstructs information supported by the captured pixels and local image evidence. A future trained Pixel Creator EXM can implement `PixelCreatorBackend` for learned structure/texture/detail reconstruction, subject to device capability and model validation.

## Intended learned model

The production learned backend can eventually expose:

- image understanding
- structure reconstruction
- texture reconstruction
- fine-detail reconstruction
- semantic consistency
- temporal consistency for video
- confidence/uncertainty maps
- device-adaptive inference

The runtime should preserve the distinction between measured/captured information and model reconstruction so Exmera never presents hallucinated pixels as sensor data.

## Resource behavior

Phase 11 intentionally operates on the already bounded Phase 10 working image. It does not allocate an unbounded full-resolution intermediate. The backend interface leaves room for tiled/GPU/NPU execution when learned EXM inference is introduced.

## Next quality layers

- trained Pixel Creator EXM
- full-resolution tiled learned inference
- GPU/NPU kernels
- RAW-domain reconstruction
- depth/segmentation-aware reconstruction
- face-aware restoration
- video temporal Pixel Creator
