# Exmera Phases 15–32

Implemented as the production foundation for the remaining product surface.

- 15 Video Engine: temporal buffer, optical-flow backend contract, CPU flow baseline, temporal denoise, stabilization.
- 16 Video Capture: capture profile, monotonic timeline, dropped-frame accounting and statistics.
- 17 Exmera Studio: nondestructive project/clip model and ordered processing graph.
- 18 Compositor/VFX: alpha compositing and NORMAL/ADD/MULTIPLY/SCREEN/OVERLAY blending.
- 19 Animation: typed keyframes, step/linear/ease interpolation and animated transforms.
- 20 Lens/Effects: parameterized effects and effect application pipeline.
- 21 Lens Store: verified EXFX package installation, hashing and lifecycle.
- 22 Exllery: media index, lightweight image embeddings, similarity search and vision inspection.
- 23 Privacy/Security: metadata sanitization, package integrity and explicit network/telemetry policy.
- 24 Export: deterministic frame export primitives with private metadata mode.
- 25 Device Tiers: capability score and adaptive processing budgets.
- 26 Performance: profiler, p95 statistics and thermal/memory adaptation.
- 27 Quality Evaluation: objective image quality gates.
- 28 Stress Testing: repeated-operation failure accounting and determinism checks.
- 29 Accessibility/UX: shared accessible-action semantics.
- 30 Production Infrastructure: health, prioritized jobs and failure boundaries.
- 31 Beta: release criteria and gates are documented below.
- 32 Production Hardening: release checklist and non-negotiable safety gates are documented below.

## Architectural honesty

These phases establish executable engine contracts and deterministic CPU fallbacks. They do not magically provide hardware capabilities or trained AI models. Dense learned optical flow, learned segmentation/depth, GPU/NPU kernels, platform video codecs, Play Store signing, remote Lens distribution, telemetry backends and real-device certification still require platform-specific implementation and validation.

## Beta gates

A beta build must pass unit tests, package integrity checks, metadata privacy checks, deterministic rendering checks, crash-free camera startup/shutdown, bounded memory behavior, and representative-device capture/export tests.

## Production hardening gates

Production release requires signed artifacts, reproducible builds, verified model/effect hashes, graceful thermal/memory degradation, cancellation-safe processing, storage quota enforcement, corrupted-input recovery, privacy-safe exports, accessibility review, crash/ANR monitoring, and device-matrix validation.
