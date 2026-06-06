package com.leoraggy.centroid_finder.controller;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping; // Added this import
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leoraggy.centroid_finder.model.JobStatusResponse;

@RestController
@RequestMapping("/api") // <-- 1. This handles the base prefix for all endpoints
public class VideoProcessingController {

    @Value("${video.mounting.directory}")
    private String videoDirectory;

    // Thread-safe map to store jobId -> JobStatusResponse
    private final Map<String, JobStatusResponse> jobTracker = new java.util.concurrent.ConcurrentHashMap<>();

    // 2. Map becomes: GET /api/videos
    @GetMapping("/videos") 
    public ResponseEntity<?> listVideos() {
        try {
            File folder = new File(videoDirectory);
            
            if (!folder.exists() || !folder.isDirectory()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Configured path is not a valid directory"));
            }

            String[] fileNames = folder.list((dir, name) -> name.endsWith(".mp4"));
            
            if (fileNames == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Error reading video directory"));
            }

            return ResponseEntity.ok(Arrays.asList(fileNames));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error reading video directory"));
        }
    }

    // 3. Map becomes: GET /api/thumbnail/{filename}
    @GetMapping(value = "/thumbnail/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> getThumbnail(@PathVariable String filename) {
        try {
            File videoFile = new File(videoDirectory, filename);
            if (!videoFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "Video file not found: " + filename));
            }

            ProcessBuilder pb = new ProcessBuilder(
                "C:\\ffmpeg-8.1.1-essentials_build\\bin\\ffmpeg.exe", "-y", "-ss", "00:00:01", "-i", videoFile.getAbsolutePath(),
                "-vframes", "1", "-f", "image2", "-c:v", "mjpeg", "pipe:1"
            );

            Process process = pb.start();

            try (InputStream is = process.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();

                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                
                if (!finished || process.exitValue() != 0) {
                    throw new RuntimeException("FFmpeg failed to process execution safely.");
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(imageBytes);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Error generating thumbnail: " + e.getMessage()));
        }
    }

    // 4. Map becomes: POST /api/process/{filename}
    @PostMapping("/process/{filename}")
    public ResponseEntity<?> startProcessingJob(
            @PathVariable String filename,
            @RequestParam(required = false) String targetColor,
            @RequestParam(required = false) Integer threshold) {

        if (targetColor == null || threshold == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing targetColor or threshold query parameter."));
        }

        try {
            String jobId = UUID.randomUUID().toString();
            File videoFile = new File(videoDirectory, filename);

            if (!videoFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Video file not found: " + filename));
            }

            jobTracker.put(jobId, new JobStatusResponse("processing"));

            new Thread(() -> {
                try {
                    String outputCsvPath = videoFile.getParent() + File.separator + filename + ".csv";
                    
                    ProcessBuilder pb = new ProcessBuilder(
                        "java", "-jar", "C:\\Users\\L_Rag\\OneDrive\\Documents\\GitHub\\SDEV334\\centroid-finder\\processor\\target\\centroidfinder-1.0-SNAPSHOT-jar-with-dependencies.jar", 
                        videoFile.getAbsolutePath(),
                        outputCsvPath,
                        targetColor,
                        String.valueOf(threshold)
                    );
                    pb.inheritIO();

                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        jobTracker.put(jobId, new JobStatusResponse("done", outputCsvPath, null));
                    } else {
                        jobTracker.put(jobId, new JobStatusResponse("error", null, "Processor exited with error code: " + exitCode));
                    }

                } catch (Exception e) {
                    jobTracker.put(jobId, new JobStatusResponse("error", null, "Exception running process: " + e.getMessage()));
                }
            }).start();

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error start job: " + e.getMessage()));
        }
    }

    // 5. Map becomes: GET /api/process/{jobId}/status
    @GetMapping("/process/{jobId}/status")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        try {
            JobStatusResponse currentStatus = jobTracker.get(jobId);

            if (currentStatus == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Job ID not found"));
            }

            return ResponseEntity.ok(currentStatus);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching job status"));
        }
    }

    @GetMapping("/process/{jobId}/download")
    public ResponseEntity<?> downloadJobResult(@PathVariable String jobId) {
        try {
            JobStatusResponse currentStatus = jobTracker.get(jobId);

            if (currentStatus == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Job ID not found."));
            }

            if (!"done".equals(currentStatus.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Job is not completed yet. Current status: " + currentStatus.getStatus()));
            }

            String csvPath = currentStatus.getResult();
            if (csvPath == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Output path not recorded for this job."));
            }

            File file = new File(csvPath);
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "The generated file could not be found on the server."));
            }

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error provisioning file download: " + e.getMessage()));
        }
    }
    }

