# Exmera

Exmera is a hardware-aware computational photography and media engine for Android.

## Principles

- Professional capability, consumer language.
- Budget phones are first-class devices.
- Small base application; engines and models are downloadable.
- Prefer deterministic CV/GPU processing where AI is unnecessary.
- Multi-frame information is preserved for reconstruction instead of relying only on single-frame hallucination.
- `.exm` is a proposed model container; compression targets are measured, not assumed.

## Repository layout

```text
app/             Android application and UI
exmera-engine/   camera/media orchestration and device profiling
exm-runtime/     EXM format and runtime primitives
models/          model manifests and licensing metadata
native/          future C++/Vulkan acceleration
runtime/         future execution backends
```

## Model packs

- Core — camera intelligence and basic enhancement
- Smart — computational photography and automatic editing
- Creator — segmentation, matting, tracking, restoration and creative tools
- Cinema — heavy restoration, video reconstruction and high-quality processing

## Development status

v0.1 establishes the Android multi-module foundation, hardware profile selection, consumer UI, and initial EXM format primitives. The next milestone is the real EXM compiler/reader and benchmark harness, followed by CameraX capture and multi-frame fusion.
