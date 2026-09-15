# Phase 9 — Computational Imaging Core

Phase 9 turns the retained Phase 8 camera frames into an actual reconstructed image on-device.

## Implemented
- Deterministic coarse motion registration using normalized cross-correlation.
- Confidence scoring and rejection of badly aligned/high-motion frames.
- Exposure normalization against the median frame exposure.
- Robust per-pixel temporal fusion using a median/MAD outlier gate to suppress transient ghosts and moving-object contamination.
- CPU working-resolution cap (1920px long edge) to prevent multi-frame processing from exhausting phone memory.
- CameraX `YUV_420_888` to RGB/luma conversion with correct plane stride handling.
- Bitmap reconstruction output wired into the camera UI.
- CameraX `ImageProxy` ownership is released in all processing paths.
- Pure JVM tests for registration and robust fusion.

## Processing graph

```text
CameraX frames
    ↓
YUV → RGB + luma
    ↓
reference selection
    ↓
translation registration
    ↓
confidence / motion rejection
    ↓
exposure normalization
    ↓
median + MAD robust fusion
    ↓
reconstructed RGB
    ↓
Exmera result UI
```

## Why this is a real computational result

This is not a filter applied to one frame. Multiple captured sensor frames contribute samples to the output after geometric registration. Stable information is reinforced across frames while transient outliers are down-weighted.

## Current production boundary

The first path is deliberately CPU-only and bounded. It does not claim to replace missing optical hardware, and it does not yet expose RAW capture. RAW `ImageReader` synchronization, gyro-assisted rotation/translation priors, dense optical flow, HDR exposure bracketing, NPU/ GPU kernels, and full-resolution tile processing remain the next performance/quality layers.

The architecture keeps those upgrades behind the same imaging core so they can replace individual stages without changing the camera UI.
