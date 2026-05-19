package io.github.leoraggy.centroidfinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VideoSummaryAppTest {

    private ColorDistanceFinder distanceFinder;
    private ImageBinarizer binarizer;
    private ImageGroupFinder groupFinder;
    
    private final int targetColor = Integer.parseInt("48010c", 16);
    private final int threshold = 30;

    @BeforeEach
    void setUp() {
        // Initialize pipeline exactly how it's configured inside VideoSummaryApp
        distanceFinder = new EuclideanColorDistance();
        binarizer = new DistanceImageBinarizer(distanceFinder, targetColor, threshold);
        groupFinder = new BinarizingImageGroupFinder(binarizer, new DfsBinaryGroupFinder());
    }

    @Test
    void testImageBinarizationLogic() {
        // Create a tiny 2x2 mock image
        BufferedImage mockImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        
        // Exact target color match (should map to white / 1)
        mockImage.setRGB(0, 0, new Color(0x48, 0x01, 0x0C).getRGB());
        // Far away color mismatch (should map to black / 0)
        mockImage.setRGB(1, 1, new Color(0xFF, 0xFF, 0xFF).getRGB());

        int[][] binaryArray = binarizer.toBinaryArray(mockImage);

        // Verify array dimensions match image dimensions
        assertEquals(2, binaryArray.length, "Height dimensions should match matrix length");
        assertEquals(2, binaryArray[0].length, "Width dimensions should match matrix row width");
        
        // Check pixel values (1 for match, 0 for mismatch)
        assertEquals(1, binaryArray[0][0], "Target color pixel should evaluate to 1 (white)");
        assertEquals(0, binaryArray[1][1], "Distant background pixel should evaluate to 0 (black)");
    }

    @Test
    void testFindingSingleLargestCentroid() {
        // Create a 5x5 grid with two distinct, isolated clusters of target color pixels
        BufferedImage mockImage = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        
        // Clear background to white
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                mockImage.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        // Cluster A (Size: 1 pixel) located at (1, 1)
        mockImage.setRGB(1, 1, new Color(0x48, 0x01, 0x0C).getRGB());

        // Cluster B (Size: 3 pixels) located contiguously at top right
        mockImage.setRGB(3, 0, new Color(0x48, 0x01, 0x0C).getRGB());
        mockImage.setRGB(4, 0, new Color(0x48, 0x01, 0x0C).getRGB());
        mockImage.setRGB(4, 1, new Color(0x48, 0x01, 0x0C).getRGB());

        // Run your application's group finder logic
        List<Group> groups = groupFinder.findConnectedGroups(mockImage);
        
        // Assert total clusters tracked
        assertEquals(2, groups.size(), "Should detect exactly 2 distinct contiguous groups");

        // Mimic the 'Largest Group Only' loop logic from VideoSummaryApp
        Group largestGroup = null;
        for (Group group : groups) {
            if (largestGroup == null || group.size() > largestGroup.size()) {
                largestGroup = group;
            }
        }

        // Assert the filter cleanly isolated Cluster B (size 3) over Cluster A (size 1)
        assertNotNull(largestGroup, "Should have isolated a non-null largest group");
        assertEquals(3, largestGroup.size(), "The identified largest group should have a size of 3 pixels");
    }

    @Test
    void testCsvOutputGeneration(@TempDir Path tempDir) throws Exception {
        // Uses JUnit's @TempDir to avoid polluting or accidentally deleting real files on your workspace
        Path tempCsvFile = tempDir.resolve("test_video_summary.csv");
        
        // Simulate printing a data point using a real Group record instance
        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(tempCsvFile))) {
            writer.println("group_size,centroid_x,centroid_y"); // Header
            
            Group dummyLargestGroup = new Group(5498, new Coordinate(320, 240));
            writer.println(dummyLargestGroup.toCsvRow());
        }

        // Verify file creation matches expectations
        assertTrue(Files.exists(tempCsvFile), "The application summary CSV should exist on disk");

        // Read contents back to confirm proper structure formatting
        List<String> lines = Files.readAllLines(tempCsvFile);
        assertEquals(2, lines.size(), "CSV file should contain exactly two rows (Header + Data)");
        assertEquals("group_size,centroid_x,centroid_y", lines.get(0), "CSV header layout must remain uniform");
        assertEquals("5498,320,240", lines.get(1), "CSV body formatting must accurately serialize custom record strings");
    }
}