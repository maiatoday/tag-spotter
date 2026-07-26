# JVM Memory Profiling CLI Guide (for KMP Desktop Apps on macOS)

This guide provides a copy-pasteable set of terminal commands to capture both snapshot memory metrics and continuous live traces on macOS for Kotlin Multiplatform Desktop (JVM) applications.

---

## 1. Locate the Process ID (PID)
First, list running JVM processes to find the PID of your running Desktop application:

```bash
# Option A: Built-in JDK process tool
jps -l
```
*Sample output:*
`1811 net.maiatoday.tagspotter.desktop.MainKt`  👉 **PID is `1811`**

```bash
# Option B: Standard Unix process search
ps aux | grep desktopApp
```

---

## 2. Instant Memory Snapshots

### A. Total Physical RAM (RSS) & Virtual Memory
`ps` provides the true physical RAM allocated by macOS to the process (including off-heap native C++ allocations like WebKit and Skia):

```bash
# Formatted process memory summary
ps -p <PID> -o pid,rss,vsz,%cpu,%mem
```
* **`RSS` (Resident Set Size)**: Physical RAM in **Kilobytes** (Divide by 1,024 for MB).
* **`VSZ` (Virtual Size)**: Total virtual memory reserved.
* **`%CPU`**: Current CPU usage across all cores.

### B. JVM Heap Memory Breakdown
`jcmd` inspects the Java Virtual Machine heap generations (Garbage-First / G1 GC):

```bash
# Detailed heap generation breakdown (Young, Old, Metaspace)
jcmd <PID> GC.heap_info
```

### C. Garbage Collection Counters
`jstat` reports young generation, full GC count, and GC pause durations:

```bash
# Print GC stats (1-second interval, 3 iterations)
jstat -gc <PID> 1000 3
```
* **`YGC`**: Young Generation GC count.
* **`FGC`**: Full GC count (triggers UI thread pauses).
* **`FGCT`**: Total time spent in Full GC pauses (in seconds).

### D. Native Memory Tracking (NMT)
To track non-heap native allocations (C++ libraries, graphics buffers, WebKit engine):

```bash
# Detailed native vs heap allocation breakdown
jcmd <PID> VM.native_memory summary
```

---

## 3. Continuous Live Sampling (1-Second Interval Loop)

To record a continuous time-series trace while interacting with the app (e.g., logging in, loading maps, panning, navigating back):

### A. Run 60-Second Sampler in Terminal
```bash
# Run a 60-second continuous loop logging RSS memory, CPU, and Heap to a file
zsh -c 'for i in {1..60}; do \
  echo "=== $(date +%H:%M:%S) ===" >> perf_trace.log; \
  ps -p <PID> -o pid,rss,%cpu >> perf_trace.log; \
  jcmd <PID> GC.heap_info 2>&1 | grep "garbage-first" >> perf_trace.log; \
  sleep 1; \
done'
```

### B. Monitor Stream Live in Terminal
To watch RAM and CPU update in real time while using the app:

```bash
# Watch physical RAM (in MB) and CPU update every second
watch -n 1 "ps -p <PID> -o pid,rss,%cpu,%mem | awk 'NR==1{print} NR==2{print \$1, \$2/1024 \" MB\", \$3\"%\", \$4\"%\"}'"
```

---

## 4. Converting RSS Memory from KB to MB
To quickly convert the `perf_trace.log` output into megabytes for blog post graphs:

```bash
# Simple awk script to print Timestamp, RAM in MB, and CPU %
awk '/===/ {time=$2} /<PID>/ {printf "%s -> %.1f MB RAM (CPU: %s%%)\n", time, $2/1024, $3}' perf_trace.log
```

---

## 5. Key Lessons for KMP Performance Analysis

1. **Heap vs Physical RSS Memory**:
   Standard JVM profilers (like VisualVM) only show Java Heap (~35 MB). Using `ps -o rss` reveals the true OS physical memory footprint (~570 MB), uncovering off-heap native library leaks like JavaFX WebKit.
2. **Detecting Unmounting Leaks**:
   If physical RAM (RSS) does not decrease when navigating away from a heavy screen (e.g. from Map back to Gallery), native libraries are failing to release C++ allocations when Compose unmounts the View.
