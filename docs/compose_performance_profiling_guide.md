# Kotlin Multiplatform Compose Performance Assessment Guide

This guide provides a comprehensive framework for assessing, measuring, and optimizing Compose UI performance across all Kotlin Multiplatform (KMP) targets (**Android**, **Desktop JVM**, **iOS**, and **Web WasmJS**). It forms the technical research foundation for **Blog Post #4**.

---

## 1. Core Principles of Compose Performance Assessment

Evaluating Compose UI performance revolves around four primary metric pillars:

### A. Frame Rendering & Jank Rate
- **Target Budget**: 60 FPS (16.6ms/frame) or 120 FPS (8.3ms/frame).
- **Jank Definition**: Any frame execution exceeding the hardware budget causes a dropped frame, perceived as stuttering during scrolling, panning, or transitions.
- **Key Metric**: Percentage of janky frames during a continuous interaction pass.

### B. Recomposition Efficiency
- **Recomposition Frequency**: How many times a `@Composable` re-executes when underlying state changes.
- **Skippable Composables**: Ensuring composables skip recomposition when inputs haven't changed using `@Stable` and `@Immutable` types.
- **Deferring State Reads**: Deferring state reads to the **Measure/Layout** or **Draw** phase (e.g., using `Modifier.graphicsLayer { alpha = animatedAlpha.value }` instead of recomposing the parent layout).

### C. Memory Footprint & Allocation Rate
- **Heap Allocations**: Short-lived object allocations during composition/drawing cause frequent Garbage Collection (GC) pauses ("GC churn").
- **Off-Heap / Native Memory**: Skia graphics surfaces, GPU textures, and image bitmaps allocate native off-heap memory.

### D. Startup Latency
- **Time to First Frame (TTFF)**: Time from process launch to the first Compose frame draw.
- **Time to Interactive (TTI)**: Time from launch until the UI responds to user input.

> [!IMPORTANT]
> **CRITICAL Compose Benchmark Rule**: Never benchmark Debug builds!
> Debug builds of Compose disable skipping optimizations, keep extra debug metadata, inject layout inspection hooks, and run un-minified code. **Always benchmark on Release / Minified builds with R8/ProGuard enabled.**

---

## 2. Platform Comparison Matrix: Compose Engines & Execution Bottlenecks

Compose Multiplatform uses **Compose Multiplatform (powered by JetBrains & Skiko)**, but execution engines and memory managers vary significantly per target:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Compose Multiplatform                          │
└────────────────────┬────────────────────┬───────────────────────────────┘
                     │                    │
          ┌──────────┴────────┐  ┌────────┴─────────┐
          │   Native Android  │  │ Skiko Platform  │
          │   (Jetpack HWUI)  │  │  (Skia Canvas)  │
          └───────────────────┘  └────────┬─────────┘
                                          │
                  ┌───────────────────────┼───────────────────────┐
                  ▼                       ▼                       ▼
            Desktop JVM               iOS Metal               Web Wasm
          (OpenGL/Vulkan)            (MetalKit)            (WebGL Canvas)
```

| Dimension | Android | Desktop (JVM) | iOS (UIKit/Metal) | Web (WasmJS) |
| :--- | :--- | :--- | :--- | :--- |
| **Graphics Engine** | Android HWUI / RenderThread | Skiko + Skia OpenGL / Software | Skiko + MetalKit (Apple Metal) | Skiko + WebGL Canvas |
| **Memory Manager** | Android ART GC | JVM GC (G1GC / ZGC) | Kotlin/Native GC + iOS ARC | Wasm GC + Browser JS Engine |
| **Primary Bottleneck** | Recomposition / Main Thread | JVM GC pauses & JIT compilation | C/Kotlin/Native interop & Metal GPU bounds | Wasm bundle size & WebGL draw calls |
| **Release Flag** | `minifyEnabled = true` | `build/libs` release jar | `release` build configuration | `-opt` Wasm production build |

---

## 3. iOS Performance Assessment & Tooling

On iOS, Compose Multiplatform renders directly onto an Apple **`CAMetalLayer` / `MetalKit`** view using Skia compiled to Kotlin/Native.

### A. Key iOS Bottlenecks to Measure
1. **Metal GPU Draw Time**: Time spent rendering Skia GPU draw commands in Metal shaders.
2. **Kotlin/Native GC Churn**: Garbage collection overhead on Apple silicon ARM64 CPUs.
3. **UIKit / Skia Interop Overhead**: Switching between native iOS views (`UIKitView`, `MKMapView`) and Compose Skia canvas layers.

### B. Available Tools for iOS Performance
1. **Xcode Instruments (The Primary Toolsuite)**:
   - **Time Profiler**: Measures CPU usage and pinpoints Kotlin/Native method call stacks and thread contention.
   - **Metal System Trace**: Analyzes GPU frame rendering duration, shader compilation, and frame pacing (checking for 60/120Hz consistency).
   - **Allocations & Leaks**: Tracks Kotlin/Native heap objects vs. native iOS Metal texture buffers.
   - **Core Animation Instrument**: Live frame rate (FPS), GPU driver statistics, and offscreen rendering detection.
2. **Code-Level Instrumentation**:
   - **`CADisplayLink`**: Attach a display link callback in Kotlin/Native to track actual FPS and dropped frames in code.
   - **`os_signpost` / `os_log`**: Emit custom Instruments markers around Compose layout or recomposition passes to view them on the Xcode Instruments timeline.

---

## 4. Web Performance Assessment & Tooling

On Web (WasmJS), Compose renders onto an HTML `<canvas>` element using Skia compiled to WebAssembly (Wasm) and WebGL.

### A. Key Web Bottlenecks to Measure
1. **Wasm Binary Download & Init (LCP)**: Size of the compiled `.wasm` file and font/resource assets.
2. **First Contentful Paint (FCP)**: Time taken to instantiate WebGL context and issue the first Skia render pass.
3. **Canvas Re-render & Interaction to Next Paint (INP)**: Time between a DOM mouse/touch event on the `<canvas>` and the Skia draw frame submission.

### B. Available Tools for Web Performance
1. **Chrome DevTools (Primary Suite)**:
   - **Performance Panel**: Record user interactions (scrolling, dragging map). Generates flame charts showing Wasm execution, WebGL call overhead, and frame drops.
   - **Performance Monitor**: Real-time HUD overlay showing live CPU %, Wasm Heap Size, DOM Nodes, and FPS.
   - **Memory Panel**: Take Heap Snapshots to detect leaked Kotlin Wasm objects or unreleased Skia image textures.
   - **Lighthouse**: Measure Web Vitals (FCP, LCP, INP, CLS) for initial bundle loading.
2. **Browser Native APIs in Code**:
   - **`performance.mark()` & `performance.measure()`**: High-precision timers in JS/Wasm to record Compose rendering milestones.
   - **`requestAnimationFrame` FPS counter**: Monitor frame times directly on the HTML canvas.

---

## 5. Desktop JVM Performance Assessment & Tooling

On Desktop JVM, Compose renders via Skiko onto AWT/Swing or pure Skia OpenGL/Vulkan windows.

### Available Tools for Desktop JVM
1. **JVM Profilers**:
   - **Async-Profiler / JProfiler / VisualVM**: Identifies CPU hot methods and allocation bottlenecks in Kotlin code.
2. **`jcmd` Native Memory Tracking (NMT)**:
   - Track native off-heap memory (Skia surfaces, C++ OpenGL textures) vs JVM Java Heap:
     ```bash
     jcmd <PID> VM.native_memory summary
     jcmd <PID> GC.heap_info
     ```
3. **JMH (Java Microbenchmark Harness)**:
   - For micro-benchmarking core algorithms (e.g. coordinate distance calculations, tile layout math).
