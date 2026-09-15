# Phase 7 — Exmera Camera Foundation

Phase 7 establishes the production camera boundary used by the computational imaging pipeline.

## Delivered

- Physical camera capability discovery through Camera2 metadata.
- RAW_SENSOR capability detection when the device exposes RAW.
- YUV 4:2:0 analysis capability detection.
- JPEG capability detection.
- Sensor and active-array dimensions.
- Maximum digital zoom discovery.
- Manual exposure/focus capability discovery.
- Optical stabilization capability discovery.
- Available target FPS discovery.
- Preview + ImageAnalysis + ImageCapture CameraX binding.
- Keep-only-latest analysis backpressure to protect realtime camera latency.
- Immutable frame metadata envelope for downstream computation.
- Explicit controller lifecycle and executor shutdown.
- Zoom control through CameraX camera control.

## Important capability rule

Exmera never assumes a feature exists because the phone model claims it should. The runtime discovers what the current camera actually exposes and selects a computational fallback when hardware is unavailable.

Examples:

```text
RAW available       -> RAW-aware capture path
RAW unavailable     -> full-resolution YUV path
OIS available       -> use OIS metadata/capability
OIS unavailable     -> computational stabilization
Ultrawide available -> native ultrawide
Ultrawide missing   -> multi-frame computational wide capture
```

## AI boundary

AI is an internal accelerator, not a user-installed plugin. Future camera stages may load compatible local `.exm` models through Exmera Runtime for segmentation, depth, denoise, super-resolution, tracking, reconstruction, and other tasks. The camera pipeline remains functional without a model and can fall back to deterministic algorithms where possible.

## Next phase

Phase 8: Multi-frame Capture Engine — synchronized frame acquisition, bounded frame buffers, timestamp/metadata preservation, burst strategies, motion-aware frame selection, and resource-safe handoff into the computational imaging graph.
