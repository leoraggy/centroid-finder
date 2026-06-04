package test;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class DistanceImageBinarizerTest {

    @Test
    public void testAllPixelsBelowThreshold() {
        // 2x2 image, all pixels = 0x123456
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int color = 0x123456;
        image.setRGB(0, 0, color);
        image.setRGB(0, 1, color);
        image.setRGB(1, 0, color);
        image.setRGB(1, 1, color);

        ColorDistanceFinder alwaysLow = (a, b) -> 5.0; // Always below threshold
        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(alwaysLow, 0x000000, 10);

        int[][] expected = {
                {1, 1},
                {1, 1}
        };

        assertArrayEquals(expected, binarizer.toBinaryArray(image));
    }

    @Test
    public void testAllPixelsAboveThreshold() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int color = 0xABCDEF;
        image.setRGB(0, 0, color);
        image.setRGB(0, 1, color);
        image.setRGB(1, 0, color);
        image.setRGB(1, 1, color);

        ColorDistanceFinder alwaysHigh = (a, b) -> 100.0; // Always above threshold
        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(alwaysHigh, 0x000000, 10);

        int[][] expected = {
                {0, 0},
                {0, 0}
        };

        assertArrayEquals(expected, binarizer.toBinaryArray(image));
    }

    @Test
    public void testMixedPixels() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        // Full 32-bit ARGB values (alpha = FF)
        int red = 0xFFFF0000;
        int green = 0xFF00FF00;

        image.setRGB(0, 0, red);   // Top-left
        image.setRGB(0, 1, green); // Bottom-left
        image.setRGB(1, 0, green); // Top-right
        image.setRGB(1, 1, red);   // Bottom-right

        // Mock distance function: only red matches target
        ColorDistanceFinder redMatchOnly = (rgb1, rgb2) -> rgb1 == rgb2 ? 0.0 : 100.0;

        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(redMatchOnly, red, 10);

        int[][] expected = {
                {1, 0},
                {0, 1}
        };

        assertArrayEquals(expected, binarizer.toBinaryArray(image));
    }

    //toBufferedImageTests

    @Test
    public void convertsAllOnesToWhitePixels() {
        int[][] binary = {
                {1, 1},
                {1, 1}
        };

        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(null, 0, 0);
        BufferedImage image = binarizer.toBufferedImage(binary);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                assertEquals(0xFFFFFF, image.getRGB(x, y) & 0xFFFFFF, "Pixel at (" + x + "," + y + ") should be white");
            }
        }
    }

    @Test
    public void convertsAllZerosToBlackPixels() {
        int[][] binary = {
                {0, 0},
                {0, 0}
        };

        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(null, 0, 0);
        BufferedImage image = binarizer.toBufferedImage(binary);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                assertEquals(0x000000, image.getRGB(x, y) & 0xFFFFFF, "Pixel at (" + x + "," + y + ") should be black");
            }
        }
    }

    @Test
    public void convertsMixedBinaryMatrixCorrectly() {
        int[][] binary = {
                {1, 0},
                {0, 1}
        };

        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(null, 0, 0);
        BufferedImage image = binarizer.toBufferedImage(binary);

        assertEquals(0xFFFFFF, image.getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0x000000, image.getRGB(1, 0) & 0xFFFFFF);
        assertEquals(0x000000, image.getRGB(0, 1) & 0xFFFFFF);
        assertEquals(0xFFFFFF, image.getRGB(1, 1) & 0xFFFFFF);
    }

    @Test
    public void testRectangularImageDimensions() {
        // 3 wide, 2 high (3 columns, 2 rows)
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        
        // Mock finder where distance is equal to the pixel's RGB value itself 
        // to distinctively map spots
        ColorDistanceFinder simpleFinder = (pixelColor, target) -> (double) pixelColor;
        
        // Threshold is 5. Values <= 5 become 1, > 5 become 0
        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(simpleFinder, 0, 5);

        // Row 0
        image.setRGB(0, 0, 1);  // <= 5 -> 1
        image.setRGB(1, 0, 10); // > 5  -> 0
        image.setRGB(2, 0, 1);  // <= 5 -> 1

        // Row 1
        image.setRGB(0, 1, 10); // > 5  -> 0
        image.setRGB(1, 1, 1);  // <= 5 -> 1
        image.setRGB(2, 1, 10); // > 5  -> 0

        int[][] expected = {
                {1, 0, 1}, // row 0 (y=0)
                {0, 1, 0}  // row 1 (y=1)
        };

        // If your implementation mixes up width/height or x/y, 
        // this will throw an exception or fail the assertion.
        assertArrayEquals(expected, binarizer.toBinaryArray(image));
    }

    @Test
    public void testToBufferedImageRectangular() {
        int[][] binary = {
                {1, 0, 1}, // row 0
                {0, 1, 0}  // row 1
        }; // 2 rows, 3 columns -> width = 3, height = 2

        DistanceImageBinarizer binarizer = new DistanceImageBinarizer(null, 0, 0);
        BufferedImage image = binarizer.toBufferedImage(binary);

        assertEquals(3, image.getWidth(), "Width should match array columns");
        assertEquals(2, image.getHeight(), "Height should match array rows");

        // Verify specific coordinates: getRGB(x, y)
        assertEquals(0xFFFFFF, image.getRGB(0, 0) & 0xFFFFFF); // row 0, col 0
        assertEquals(0x000000, image.getRGB(1, 0) & 0xFFFFFF); // row 0, col 1
        assertEquals(0xFFFFFF, image.getRGB(2, 0) & 0xFFFFFF); // row 0, col 2
    }
}