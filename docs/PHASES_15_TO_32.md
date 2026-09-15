# Exmera Phases 15–32 — hardened foundation

The remaining product surface now has executable engine contracts plus production guardrails. Phase 15 was hardened to be timestamp-aware and discontinuity-safe; Phase 16 is connected to real CameraX video recording; Phases 17–32 retain deterministic CPU/reference implementations where platform or trained-model work is required.

## Hardened capabilities

- Video: bounded temporal buffering, timestamp ordering, discontinuity rejection, optical-flow backend boundary, temporal denoise, stabilization.
- Capture: CameraX VideoCapture/Recorder with MediaStore MP4 output and graceful finalize/error state.
- Imaging: CPU computational fallbacks remain deterministic and bounded.
- Production: processing budgets, thermal degradation policy, cancellation token, storage quota, SHA-256 integrity verification and release-gate evaluation.
- Privacy: exports and model/effect packages remain hash-verifiable and metadata-safe.

## What still requires real-device/platform validation

Dense learned optical flow, trained EXM models, GPU/NPU kernels, RAW multi-frame capture, gyro fusion, HDR exposure bracketing, full codec matrix validation, hardware-specific camera controls, Play signing, store distribution, crash/ANR telemetry, and certification across representative devices cannot be truthfully completed from repository code alone. Their interfaces remain replaceable so those implementations can be added without redesigning Exmera.

## Release rule

A release is **not** considered production-ready merely because these APIs compile. The release gate must include unit/instrumentation tests, real-device camera start/stop, video recording/finalization, memory/thermal degradation, corrupted-input recovery, privacy export checks, model/effect integrity, accessibility review, and representative-device validation.
