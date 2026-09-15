# Phase 14 — Portrait & Depth

Phase 14 turns Exmera's relative depth and vision primitives into a portrait/depth processing layer shared by camera and editor workflows.

## Built

- `DepthCalibration` for normalized depth remapping.
- `DepthProcessor` confidence estimation.
- `PortraitMaskEstimator` combining relative depth with known face/person detections.
- Edge-aware mask softening to reduce hard segmentation boundaries.
- `PortraitDepthEngine` CPU reference implementation for depth-aware blur.
- Foreground protection and configurable edge protection.
- Aperture-inspired bokeh strength control.
- Dimension/range safety checks.
- Deterministic unit tests.

## Processing graph

```text
RGB Frame
   │
   ├── Vision detections ──┐
   │                       │
   └── Depth estimation ──┼──> Portrait mask
                           │
                           ▼
                     Depth-aware blur
                           │
                           ▼
                     Portrait output
```

The current depth estimator is a relative CPU fallback. It is not a metric depth sensor and should not be presented as physical distance. Likewise, the fallback mask is not equivalent to a learned person-matting model.

The architecture is ready for learned EXM backends to replace depth and segmentation while retaining the same portrait processing API.
