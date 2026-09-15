# Phase 1 — Exmera Core Architecture

Phase 1 establishes the stable runtime boundary that every future Exmera feature builds on.

## Runtime layers

```text
                    EXMERA APP
                        |
                   ExmeraEngine
                        |
              +---------+---------+
              |         |         |
           Capture   Compute    Render
              |         |         |
              +---------+---------+
                        |
                    MediaGraph
                        |
             +----------+----------+
             |          |          |
          Camera      Vision     Export
           nodes      / AI       nodes
                        |
                  EXM Runtime
                        |
                 CPU / GPU / NPU
```

## Contracts

- `ExmeraEngine` owns lifecycle and installed graph state.
- `EngineConfig` contains execution policy without exposing hardware details to features.
- `DeviceProfile` describes the detected device capability tier.
- `MediaFrame` is a lightweight frame descriptor; pixel buffers remain outside the descriptor so zero-copy/native storage can be added later.
- `MediaNode` is the common processing contract.
- `MediaGraph` owns ordered node execution and rejects duplicate node IDs.
- `CaptureSubsystem`, `ComputeSubsystem`, and `RenderSubsystem` define the three primary engine domains.

## Lifecycle

```text
CREATED -> READY -> RUNNING <-> PAUSED
                    |
                    v
                  STOPPED
                    |
                    v
                  CLOSED
```

A graph must be installed before processing. Processing is rejected unless the engine is running.

## Phase 1 non-goals

Phase 1 does not yet implement camera HAL drivers, asynchronous scheduling, GPU/Vulkan execution, NPU delegates, model loading, or pixel-buffer ownership. Those systems plug into these contracts in later phases without requiring the application UI to know their implementation details.
