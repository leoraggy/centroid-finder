package com.leoraggy.centroid_finder.controller;

import com.leoraggy.centroid_finder.model.JobStatusResponse;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class VideoProcessingController {

    @Value("${video.mounting.directory}")
    private String videoDirectory;

    // Thread-safe map to store jobId -> JobStatusResponse
    private final Map<String, JobStatusResponse> jobTracker = new java.util.concurrent.ConcurrentHashMap<>();

  // 1. GET /api/videos - List Available Videos
@GetMapping("/api/videos")
public ResponseEntity<?> listVideos() {
    try {
        File folder = new File(videoDirectory);
        
        // Ensure the path points to a valid directory
        if (!folder.exists() || !folder.isDirectory()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Configured path is not a valid directory"));
        }

        // Filter and grab only real files (optionally filtering by .mp4/.mov if you want)
        String[] fileNames = folder.list((dir, name) -> name.endsWith(".mp4")); // Excludes hidden files like .DS_Store
        
        if (fileNames == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error reading video directory"));
        }

        return ResponseEntity.ok(Arrays.asList(fileNames)); // Returns 200 OK with actual file list
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error reading video directory"));
    }
}

  @GetMapping(value = "/thumbnail/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> getThumbnail(@PathVariable String filename) {
        try {
            // 1. Construct the absolute path to the target video file
            File videoFile = new File(videoDirectory, filename);
            if (!videoFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", "Video file not found: " + filename));
            }

            // 2. Build the FFmpeg command line string execution
            // -i : input file
            // -ss 00:00:01 : seek to the 1-second mark to capture a clear frame
            // -vframes 1 : extract exactly 1 frame
            // -f image2 -c:v mjpeg pipe:1 : stream the output format directly as mjpeg into our Java memory pipeline
          // Replace "ffmpeg" with the absolute path to your local ffmpeg.exe file
            // Pass the absolute file path directly to the executable
            ProcessBuilder pb = new ProcessBuilder(
                "C:\\ffmpeg-8.1.1-essentials_build\\bin\\ffmpeg.exe", "-y", "-ss", "00:00:01", "-i", videoFile.getAbsolutePath(),
                "-vframes", "1", "-f", "image2", "-c:v", "mjpeg", "pipe:1"
            );

            // 3. Fire the execution process
            Process process = pb.start();

            // 4. Read the incoming binary data stream from FFmpeg out into a Java byte array
            try (InputStream is = process.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();

                // Wait a brief moment to ensure the command closes nicely
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                
                if (!finished || process.exitValue() != 0) {
                    throw new RuntimeException("FFmpeg failed to process execution safely.");
                }

                // 5. Return the true image array bytes directly to Postman / Browser
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

        // 1. Initialize the job tracker to "processing"
        jobTracker.put(jobId, new JobStatusResponse("processing"));

        // 2. Run the JAR file asynchronously in a background thread
        new Thread(() -> {
            try {
              // Dynamically grabs the video's parent folder path and puts the CSV right next to it
                String outputCsvPath = videoFile.getParent() + File.separator + filename + ".csv";
                
                // Construct the command to run your JAR file
                // java -jar path/to/your.jar <input_video> <output_csv> <color> <threshold>
                ProcessBuilder pb = new ProcessBuilder(
                    "java", "-jar", "C:\\Users\\L_Rag\\OneDrive\\Documents\\GitHub\\SDEV334\\centroid-finder\\processor\\target\\centroidfinder-1.0-SNAPSHOT-jar-with-dependencies.jar", 
                    videoFile.getAbsolutePath(),
                    outputCsvPath,
                    targetColor,
                    String.valueOf(threshold)
                );
                pb.inheritIO();

                Process process = pb.start();
                
                // Wait for the external JAR to finish processing
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    // Update tracker to "done" if successful
                    jobTracker.put(jobId, new JobStatusResponse("done", outputCsvPath, null));
                } else {
                    // Update tracker to "error" if the JAR failed
                    jobTracker.put(jobId, new JobStatusResponse("error", null, "Processor exited with error code: " + exitCode));
                }

            } catch (Exception e) {
                jobTracker.put(jobId, new JobStatusResponse("error", null, "Exception running process: " + e.getMessage()));
            }
        }).start(); // <-- Starts the thread background worker execution immediately

        // 3. Instantly return 202 to the user while the thread works silently
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error starting job: " + e.getMessage()));
    }
}

    // 4. GET /process/{jobId}/status - Get Real Processing Job Status
    @GetMapping("/process/{jobId}/status")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        try {
            // Look up the job in our concurrent tracker map
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
}