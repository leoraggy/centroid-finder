import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;



import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BinarizingImageGroupFinderTest {

    private BinarizingImageGroupFinder finder;
    private SimpleBinarizer binarizer;
    private DfsBinaryGroupFinder groupFinder;

    @BeforeEach
    void setUp() {
        // Create simple implementations of dependencies
        binarizer = new SimpleBinarizer();
        groupFinder = new DfsBinaryGroupFinder();

        // Create the finder with real dependencies
        finder = new BinarizingImageGroupFinder(binarizer, groupFinder);
    }

    @Test
    @DisplayName("Test with simple 3x3 image containing one white group")
    void testSimpleImage() {
        // Create a 3x3 image with a single white 2x2 square in the top-left
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fill entire image with black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 3, 3);

        // Draw a 2x2 white square in the top-left
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 2, 2);
        g.dispose();

        // Find connected groups
        List<Group> groups = finder.findConnectedGroups(image);

        // Verify results
        assertEquals(1, groups.size(), "Should find exactly one group");
        assertEquals(4, groups.get(0).size(), "Group should have 4 pixels");
        assertEquals(0, groups.get(0).centroid().x(), "Centroid X should be 0");
        assertEquals(0, groups.get(0).centroid().y(), "Centroid Y should be 0");
    }

    @Test
    @DisplayName("Test with image containing multiple separate white groups")
    void testMultipleGroups() {
        // Create a 5x5 image with two separate white groups
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fill entire image with black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 5, 5);

        // Draw a white pixel group in the top-left
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 2, 2);

        // Draw another white pixel group in the bottom-right
        g.fillRect(3, 3, 2, 2);
        g.dispose();

        // Find connected groups
        List<Group> groups = finder.findConnectedGroups(image);

        // Verify results
        assertEquals(2, groups.size(), "Should find exactly two groups");

        // Both groups should be the same size
        assertEquals(4, groups.get(0).size(), "First group should have 4 pixels");
        assertEquals(4, groups.get(1).size(), "Second group should have 4 pixels");

        // Groups should be sorted by centroid Y in descending order
        assertTrue(groups.get(0).centroid().y() > groups.get(1).centroid().y(),
                "Groups should be sorted by centroid Y in descending order");
    }

    @Test
    @DisplayName("Test with all-black image")
    void testAllBlackImage() {
        // Create a 4x4 all-black image
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 4, 4);
        g.dispose();

        // Find connected groups
        List<Group> groups = finder.findConnectedGroups(image);

        // Verify results
        assertTrue(groups.isEmpty(), "Should find no groups in an all-black image");
    }

    @Test
    @DisplayName("Test with all-white image")
    void testAllWhiteImage() {
        // Create a 4x4 all-white image
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 4, 4);
        g.dispose();

        // Find connected groups
        List<Group> groups = finder.findConnectedGroups(image);

        // Verify results
        assertEquals(1, groups.size(), "Should find one group in an all-white image");
        assertEquals(16, groups.get(0).size(), "Group should contain all 16 pixels");
        assertEquals(1, groups.get(0).centroid().x(), "Centroid X should be 1");
        assertEquals(1, groups.get(0).centroid().y(), "Centroid Y should be 1");
    }

    @Test
    @DisplayName("Test with L-shaped white pattern")
    void testLShapedPattern() {
        // Create a 3x3 image with an L-shaped white pattern
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fill with black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 3, 3);

        // Draw L-shape in white
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1, 3); // Vertical line
        g.fillRect(0, 2, 3, 1); // Horizontal line
        g.dispose();

        // Find connected groups
        List<Group> groups = finder.findConnectedGroups(image);

        // Verify results
        assertEquals(1, groups.size(), "Should find one group for the L shape");
        assertEquals(5, groups.get(0).size(), "Group should have 5 pixels");
        // The centroid coordinates depend on the specific implementation of the L-shape
    }

    @Test
    @DisplayName("Test with disconnected white pixels")
    void testDisconnectedPixels() {
        // Create a 4x4 image with disconnected white pixels at corners
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fill with black
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 4, 4);

        // Draw white pixels at corners
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1, 1); // Top-left
        g.fillRect(3, 0, 1, 1); // Top-right
        g.fillRect(0, 3, 1, 1); // Bottom-left
        g.fillRect(3, 3, 1, 1); // Bottom-right
        g.dispose();

        // Find connected groups
        List<Group> groups = finder.findConnectedGroups(image);

        // Verify results
        assertEquals(4, groups.size(), "Should find four separate groups");

        // All groups should be single pixels
        for (Group group : groups) {
            assertEquals(1, group.size(), "Each group should be a single pixel");
        }

        // First group should be bottom-right (highest Y and X)
        assertEquals(3, groups.get(0).centroid().y(), "First group should have highest Y");
        assertEquals(3, groups.get(0).centroid().x(), "First group should have highest X");
    }

    /**
     * A simple implementation of ImageBinarizer for testing
     */
    private static class SimpleBinarizer implements ImageBinarizer {
        @Override
        public int[][] toBinaryArray(BufferedImage image) {
            if (image == null) {
                throw new NullPointerException("Image cannot be null");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int[][] result = new int[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Get the RGB color at the pixel
                    int rgb = image.getRGB(x, y);
                    // Convert to a binary value (1 for white, 0 for non-white)
                    // Using a simple threshold - if the color is close to white, count as white
                    Color color = new Color(rgb);
                    int average = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                    result[y][x] = (average > 200) ? 1 : 0;
                }
            }

            return result;
        }

        @Override
        public BufferedImage toBufferedImage(int[][] image) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'toBufferedImage'");
        }
    }

    @Test
    @DisplayName("Test diagonally touching white pixels are not connected")
    void testDiagonalNotConnected() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 2, 2);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1, 1); // Top-left
        g.fillRect(1, 1, 1, 1); // Bottom-right
        g.dispose();

        List<Group> groups = finder.findConnectedGroups(image);
        assertEquals(2, groups.size(), "Diagonal pixels should be separate groups");
    }

    @Test
    @DisplayName("Test one pixel wide vertical white line")
    void testVerticalLine() {
        BufferedImage image = new BufferedImage(1, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1, 4); // Full vertical line
        g.dispose();

        List<Group> groups = finder.findConnectedGroups(image);
        assertEquals(1, groups.size(), "Should find one vertical group");
        assertEquals(4, groups.get(0).size(), "Group should have 4 pixels");
        assertEquals(0, groups.get(0).centroid().x(), "Centroid X should be 0");
        assertEquals(1, groups.get(0).centroid().y(), "Centroid Y should be 1");
    }

    @Test
    @DisplayName("Test white cross shape with different lengths")
    void testUnevenCross() {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 5, 5);

        g.setColor(Color.WHITE);
        g.fillRect(2, 0, 1, 5); // Vertical line
        g.fillRect(1, 2, 3, 1); // Horizontal line
        g.dispose();

        List<Group> groups = finder.findConnectedGroups(image);
        assertEquals(1, groups.size(), "Cross shape should be a single group");
        assertEquals(7, groups.get(0).size(), "Group should contain 7 unique pixels");

    }

}