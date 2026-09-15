# Exmera Phases 2–6

## Phase 2 — Hardware Abstraction Layer
Exposes CPU, memory, camera, codec, Vulkan and accelerator capabilities through a stable Exmera interface. Android-specific probing stays behind `HardwareAbstractionLayer`.

## Phase 3 — EXM Format
EXM v1 now has deterministic container read/write support, manifest metadata, payload size validation and SHA-256 integrity verification.

## Phase 4 — EXM Compiler
`ExmPackCompiler` establishes the compiler boundary. It currently packages an already-produced model payload; future passes add graph simplification, distillation, pruning, quantization, clustering and compression without changing the runtime API.

## Phase 5 — EXM Runtime
`ExmRuntime`, `ExmBackend`, `LoadedExm` and `ExmValidator` provide backend selection, compatibility validation and safe model loading. CPU/GPU/NPU execution implementations can be plugged in independently.

## Phase 6 — Model Distribution
`ModelDescriptor` defines remote model metadata and `ModelStore` provides versioned local storage plus SHA-256 verification. Network download orchestration is intentionally kept above the storage layer so CDN providers can change later.

## Completion criteria
- No placeholder EXM reader remains.
- Runtime never trusts an unverified payload.
- Hardware probing is isolated from engine code.
- Compiler and runtime have stable boundaries.
- Model cache is versioned and integrity checked.

## Next
Phase 7: Camera Foundation — camera lifecycle, permissions, CameraX capture, image analysis and sensor metadata.
