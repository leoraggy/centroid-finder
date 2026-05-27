package com.leoraggy.centroid_finder.controller;

import com.leoraggy.centroid_finder.model.JobStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoProcessingController.class)
class VideoProcessingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoProcessingController videoProcessingController;

    @TempDir
    Path sharedTempDir; // Automatically handles OS-independent temporary file structure

    @BeforeEach
    void setUp() {
        // Dynamically inject the mock temporary directory path into the @Value field
        ReflectionTestUtils.setField(videoProcessingController, "videoDirectory", sharedTempDir.toString());
    }

    // ==========================================
    // 1. TESTS FOR GET /api/videos
    // ==========================================

    @Test
    void listVideos_Success() throws Exception {
        // Arrange: Create mock .mp4 files in our temporary directory
        Files.createFile(sharedTempDir.resolve("sample1.mp4"));
        Files.createFile(sharedTempDir.resolve("sample2.mp4"));
        Files.createFile(sharedTempDir.resolve("ignore_me.txt")); // Should be filtered out

        // Act & Assert
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("sample1.mp4", "sample2.mp4")));
    }

    @Test
    void listVideos_NotADirectory_Returns500() throws Exception {
        // Arrange: Point to a broken or non-existent path
        ReflectionTestUtils.setField(videoProcessingController, "videoDirectory", "C:\\invalid\\path\\here");

        // Act & Assert
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("Configured path is not a valid directory")));
    }

    // ==========================================
    // 2. TESTS FOR GET /thumbnail/{filename}
    // ==========================================

    @Test
    void getThumbnail_FileNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/thumbnail/missing_video.mp4"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("Video file not found")));
    }

    // Note: A "Success" test for /thumbnail requires mocking the actual ProcessBuilder/FFmpeg execution.
    // Since it targets a strict local C:\ address, it's bypassed here to prevent system errors.

    // ==========================================
    // 3. TESTS FOR POST /process/{filename}
    // ==========================================

    @Test
    void startProcessingJob_MissingParams_Returns400() throws Exception {
        mockMvc.perform(post("/process/video.mp4")) // Missing targetColor and threshold
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing targetColor or threshold query parameter.")));
    }

    @Test
    void startProcessingJob_FileNotFound_Returns404() throws Exception {
        mockMvc.perform(post("/process/ghost_video.mp4")
                        .param("targetColor", "red")
                        .param("threshold", "15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Video file not found")));
    }

    @Test
    void startProcessingJob_Success_Returns202() throws Exception {
        // Arrange: Create the video file so validation passes
        String testVideoName = "real_video.mp4";
        Files.createFile(sharedTempDir.resolve(testVideoName));

        // Act & Assert
        mockMvc.perform(post("/process/" + testVideoName)
                        .param("targetColor", "#FF0000")
                        .param("threshold", "25"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId", notNullValue()));
    }

    // ==========================================
    // 4. TESTS FOR GET /process/{jobId}/status
    // ==========================================

    @Test
    void getJobStatus_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/process/fake-uuid-123/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Job ID not found")));
    }

    @Test
    void getJobStatus_Success_ReturnsStatus() throws Exception {
        // Arrange: Simulate an existing job in the controller's map
        String mockJobId = "test-job-xyz";
        JobStatusResponse mockResponse = new JobStatusResponse("processing");
        
        // Use Reflection to extract the map and populate it
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, JobStatusResponse> tracker = 
                (ConcurrentHashMap<String, JobStatusResponse>) ReflectionTestUtils.getField(videoProcessingController, "jobTracker");
        
        if (tracker != null) {
            tracker.put(mockJobId, mockResponse);
        }

        // Act & Assert
        mockMvc.perform(get("/process/" + mockJobId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("processing")));
    }
}