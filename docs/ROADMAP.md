# Exmera implementation roadmap

## Milestone 0 — Foundation
- [x] Android multi-module project
- [x] Consumer-first camera shell
- [x] Live CameraX preview
- [x] Device tier detection
- [x] EXM manifest primitives
- [x] Model pack manifest

## Milestone 1 — EXM laboratory
- [ ] Canonical binary EXM encoder
- [ ] EXM decoder and chunk index
- [ ] SHA-256 verification
- [ ] ONNX ingestion
- [ ] FP16/INT8 conversion adapters
- [ ] Compression benchmark harness
- [ ] RAM/latency/quality report
- [ ] Real-ESRGAN-General-x4v3 benchmark

## Milestone 2 — Camera engine
- [x] Camera foundation and capability discovery
- [x] ImageAnalysis pipeline
- [x] Original-frame multi-frame retention
- [x] Bounded frame ownership and lifecycle
- [x] Timestamp/orientation preservation
- [x] Deterministic frame selection primitive
- [ ] Camera2 RAW ImageReader capture
- [x] motion alignment
- [x] multi-frame fusion
- [x] exposure normalization / robust fusion
- [x] temporal denoise / outlier suppression
- [x] first reconstructed output
- [x] result comparison/result UI
- [x] Phase 10 deterministic super-resolution backend
- [x] 2× and 4× output pipeline
- [x] EXM-ready super-resolution backend interface
- [x] objective image-quality metric primitives
- [ ] Camera2 RAW ImageReader integration
- [ ] dense optical-flow alignment
- [ ] gyro-assisted registration
- [ ] HDR exposure bracketing
- [ ] full-resolution tiled learned fusion
- [ ] GPU/NPU accelerated kernels
- [ ] trained learned super-resolution model

## Milestone 3 — Vision
- [ ] face detection/tracking
- [ ] person/object tracking
- [ ] segmentation
- [ ] depth
- [ ] quality analysis

## Milestone 4 — Video
- [ ] temporal denoise
- [ ] optical flow
- [ ] temporal fusion
- [ ] video super-resolution
- [ ] stabilization
- [ ] consistency correction

## Milestone 5 — Creator / Cinema
- [ ] cutout and matting
- [ ] object removal
- [ ] relighting
- [ ] advanced restoration
- [ ] effect package runtime
- [ ] cinema model pack
