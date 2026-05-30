Progress: Created a short TODO plan tracking audit, refactors, tests, docs, and CI.

Refactoring & Code Quality

Modularize: Extract video I/O, frame-sampling, binarization, and group-finding into well-defined interfaces so logic can be unit-tested and reused. Example entry points: VideoSummaryApp.java and VideoExperiment.java.
Remove magic numbers: Replace frameNumber % 60 and other constants with named configuration (CLI option or config file) and document their meaning.
Avoid commented-out code: Clean or move experiments into separate sample classes (e.g., VideoExperiment, JavaCVPlayground) so main code stays focused.
Error Handling & Logging

Stop using System.exit in library contexts: Replace System.exit(...) in VideoSummaryApp with exceptions or return codes; let the top-level launcher decide termination. System.exit is disruptive for integration (e.g., Spring Boot).
Replace println/printStackTrace: Use a logging framework (SLF4J + Logback) instead of System.out/err and e.printStackTrace() so you can control levels and integrate into CI/production logs. Files with many prints: VideoSummaryApp.java, ImageSummaryApp.java.
Improve exception messages: Provide actionable errors (include parameter values and expected formats) and log exceptions with context (e.g., which file/frame failed).
Resource guarantees: You already use try-with-resources in many places—ensure every native resource (FFmpeg grabber, converters) is closed on all error paths.
Input Validation & CLI Robustness

Harden parsing: Validate CLI args (file exists/readable, numeric ranges for threshold, hex color length and characters). Fail fast with helpful usage text.
Charset & atomic writes: Use Files.newBufferedWriter(path, StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING) and consider atomic write (write temp file then move) for CSV output.
Path handling: Use java.nio.file.Path and avoid relying on File#getParentFile()/mkdirs() without checking return values.
Performance & Memory

Frame sampling vs. grabbing: Rather than scanning every frame and using frameNumber % 60, use time-based sampling by fps (or skip frames via grabber API) to reduce unnecessary decoding.
Avoid full-frame copies: Minimize conversions between Frame, Mat, and BufferedImage—convert only when needed, re-use converter instances, and avoid building large in-memory structures per frame if possible.
Batching and back-pressure: If processing is slower than decoding, add a bounded queue or back-pressure; consider a producer/consumer model to decode and process frames in separate threads.
Profiling: Add a simple profiler/metrics (frame processing time, memory use) to know bottlenecks before optimizing.
Security & Hardening

Validate output paths: Prevent path traversal or writing outside intended directories (especially if any input is user-controlled). Reject paths with .. or absolute paths when not allowed; apply least privilege.
Native libs & logging: You already set FFmpegLogCallback level—ensure native dependencies are pinned and consider verifying native library loading to avoid unexpected failures on different platforms.
Resource exhaustion: Sanitize inputs (max frame count, max allowed group size) to avoid OOM on malicious or extremely large files.
Testing

Unit tests for pure logic: Add tests for ColorDistanceFinder, ImageBinarizer, DfsBinaryGroupFinder, and Group behavior (you already have some tests; expand coverage).
Integration tests for I/O: Add an integration test that runs processing on a small, committed sample clip (or a scripted series of frames) to assert CSV output format and expected group results.
Mock external deps: For fast/unit tests, mock FFmpegFrameGrabber/JavaCV conversions or provide a small adapter that feeds deterministic frames.
Automated reproducible data: Commit tiny sample inputs under processor/src/test/resources rather than relying on local sampleInput/ paths.
Documentation

CLI README: Add a short README for the processor module with usage examples, expected CSV schema, and recommended runtime memory/CPU.
Javadocs and API notes: Document public interfaces (ImageBinarizer, ImageGroupFinder, ColorDistanceFinder) and expected contracts (thread-safety, side effects).
Developer notes: Document how to reproduce experiments (where VideoExperiment or JavaCVPlayground live) and how to add new tests.
CI & Quality

Automate checks: Add a GitHub Actions workflow (or equivalent) that runs mvn -T 1C test, runs static analysis (SpotBugs, PMD, Checkstyle), and enforces formatting (Spotless).
Enforce Java version & dependency checks: Pin compiler level and verify native dependency compatibility matrix in CI.
Prioritization (suggested)

Replace System.exit and centralize error handling (prevents accidental shutdowns).
Introduce logging and replace printStackTrace/println (observability).
Harden CLI input validation and path handling (safety).
Add unit tests for core pure logic classes (confidence).
Measure and fix performance hotspots (profiling-driven).
Add CI to run tests and linters (automation).
Would you like me to (a) expand any item into a concrete checklist/PR plan, (b) create a starter refactor branch that replaces println/System.exit with SLF4J and exceptions, or (c) generate the CI workflow and test skeletons?

GPT-5 mini • 1x
