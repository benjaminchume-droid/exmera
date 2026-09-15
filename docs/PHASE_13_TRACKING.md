# Phase 13 — Tracking Engine

Phase 13 turns the basic Vision IoU tracker into a persistent tracking subsystem suitable for camera, editor, and video workflows.

## Implemented

- Persistent track IDs across frames.
- Velocity and acceleration estimates in normalized coordinates.
- Predicted bounds using a constant-acceleration model.
- Greedy global candidate association using predicted IoU and center distance.
- Class-aware association when both the existing track and detection have known classes.
- Tentative → tracked lifecycle based on configurable confirmation hits.
- Occluded state for short detection gaps.
- Lost state after the normal occlusion window.
- Removal after a configurable re-identification window.
- Confidence decay while detections are missing.
- Re-acquisition of a recently missing target while retaining its track ID.
- Coordinate mapping primitives for normalized/view/image-facing integrations.
- Reset support for camera-session boundaries.
- Deterministic, dependency-free CPU implementation.

## State model

```text
TENTATIVE --confirmed--> TRACKED
TRACKED --missing--> OCCLUDED
OCCLUDED --missing--> LOST
OCCLUDED --detected--> TRACKED
LOST --detected--> TRACKED
LOST --expired--> REMOVED
```

## Association

Each frame scores compatible track/detection pairs using predicted IoU and normalized center distance. Candidate pairs are sorted by score and consumed greedily, preventing one detection from being assigned to multiple tracks.

The tracker does not claim semantic identity by itself. Learned embeddings and appearance descriptors belong in a future EXM-backed re-identification backend.

## Motion model

The current baseline keeps velocity and acceleration for each track and predicts:

`position(t + dt) = position(t) + velocity * dt + 0.5 * acceleration * dt²`

Measured velocity and acceleration are exponentially smoothed to reduce jitter. This is intentionally lightweight and deterministic; a future production backend can replace it with a Kalman/extended-Kalman or learned motion model without changing the public snapshots.

## Coordinate model

Vision detections use normalized `[0,1]` coordinates. `TrackingTransform` provides scale and offset mapping so the same track can be projected into another normalized view/editor coordinate space. Physical camera calibration and crop/rotation matrices can be layered on top later.

## Tests

Coverage includes ID persistence, motion estimation, short occlusion recovery, lost-track expiry, class-aware association, and coordinate transforms.

## Remaining intelligence work

This phase deliberately does not pretend that geometry alone can recognize a person after a long occlusion. Future learned EXM backends should add appearance embeddings, face re-identification, object descriptors, optical-flow assistance, and stronger camera-motion compensation.
