# Phase 8 — Multi-Frame Capture Engine

Phase 8 turns Exmera's camera stream into a controlled source of original frames for computational imaging.

## Delivered

- Bounded, resource-safe multi-frame retention.
- Exact ownership rules for `ImageProxy` frames.
- Automatic oldest-frame eviction when the buffer is full.
- Explicit capture lifecycle: `IDLE -> CAPTURING -> COMPLETE`.
- Configurable capture target with safe bounds.
- Monotonic sequence IDs and sensor timestamps preserved per frame.
- Frame dimensions, format and rotation preserved.
- Deterministic frame-ranking primitive using sharpness, exposure and motion scores.
- Camera preview wired to the multi-frame capture engine.
- Consumer camera button starts a five-frame computational capture.
- Frames remain available as a handoff set for the upcoming alignment/fusion stages.

## Ownership contract

`ImageProxy` is never copied blindly into an unbounded collection. While a capture is inactive, the analyzer closes each frame immediately. During a capture, the engine owns the proxy until it is evicted, reset, or closed.

This prevents the classic CameraX failure mode where retained analysis images exhaust the camera pipeline and stall preview.

## Capture flow

```text
CameraX ImageAnalysis
        |
        v
ExmeraCameraController
        |
        v
MultiFrameCaptureEngine
        |
        +--> bounded frame buffer
        |
        +--> timestamps / sequence / orientation
        |
        +--> frame selection
        |
        v
Phase 9 alignment + fusion
```

## Important limitation

Phase 8 preserves the original YUV analysis frames exposed by CameraX. RAW capture is still capability-dependent and requires a Camera2/RAW image reader path; this is deliberately kept separate from the YUV analysis stream so the application does not assume that every phone exposes RAW or supports every stream combination.

## AI boundary

The capture engine is model-agnostic. Later phases can run on-device EXM models over the retained frames for motion estimation, alignment, depth, segmentation, denoising, super-resolution and reconstruction. No cloud inference is required by this architecture.

## Next

Phase 9 — Computational Imaging Core: motion alignment, robust frame registration, exposure fusion, temporal denoise and the first real multi-frame image reconstruction pipeline.
