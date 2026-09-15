# Phase 12 — Vision System

Phase 12 establishes Exmera's shared vision layer for camera, editor, tracking, portrait, and future learned EXM models.

## Implemented

- normalized detection geometry and confidence contracts
- pluggable `VisionBackend` interface
- deterministic CPU saliency/object-region fallback
- stable multi-frame tracking with IoU association and missing-frame retention
- image-quality analysis: sharpness, exposure, contrast, noise, overall score
- relative monocular-depth fallback (explicitly non-metric)
- soft foreground/saliency segmentation fallback
- one `VisionEngine` facade shared by downstream features
- unit tests for detection, tracking, quality, depth, segmentation, and orchestration

## Architecture

```text
                  VisionEngine
                       |
       +---------------+----------------+
       |               |                |
   Detection        Quality         Geometry
       |                                |
 VisionBackend                     VisionTracker
       |
 +-----+----------------+
 |                      |
CPU fallback        future EXM model
                       |
          +------------+------------+
          |            |            |
       Person        Object       Face
       tracking      tracking     tracking

Image -> quality / depth / segmentation -> Camera + Studio + VFX
```

The CPU fallback intentionally does not claim to recognize a person, face, or object class. It returns `UNKNOWN` regions. Real semantic detection belongs behind a learned EXM backend, so Exmera never presents heuristic guesses as AI facts.

Depth values are relative scene ordering, not meters. Accurate metric depth requires device depth sensors, calibrated stereo, or a trained monocular depth model.

Segmentation is likewise a soft saliency fallback. Production person/subject matting will use a trained EXM model in the same API.

## Why this design

Vision is now a first-class engine rather than functionality embedded separately in portrait, tracking, or editor features. This lets one inference result be reused by multiple consumers and lets hardware-specific or learned backends replace the CPU fallback without rewriting the application graph.
