package test;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EuclideanColorDistanceTest {

    // -----------------------------
    // Test 1: Same color = distance 0
    // -----------------------------
    @Test
    public void sameColor() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        double actual = test.distance(0x000000, 0x000000);

        assertEquals(0.0, actual, 0.0001);
    }

    // -----------------------------
    // Test 2: Black to White
    // sqrt(255^2 + 255^2 + 255^2)
    // -----------------------------
    @Test
    public void blackToWhite() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        double actual = test.distance(0x000000, 0xFFFFFF);

        assertEquals(Math.sqrt(195075), actual, 0.0001);
    }

    // -----------------------------
    // Test 3: Red to Green
    // sqrt(255^2 + 255^2)
    // -----------------------------
    @Test
    public void redToGreen() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        double actual = test.distance(0xFF0000, 0x00FF00);

        assertEquals(Math.sqrt(130050), actual, 0.0001);
    }

    // -----------------------------
    // Test 4: Red to Blue
    // sqrt(255^2 + 255^2)
    // -----------------------------
    @Test
    public void redToBlue() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        double actual = test.distance(0xFF0000, 0x0000FF);

        assertEquals(Math.sqrt(130050), actual, 0.0001);
    }

    // -----------------------------
    // Test 5: One channel difference
    // only blue changes by 255
    // -----------------------------
    @Test
    public void blueOnlyDifference() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        double actual = test.distance(0x000000, 0x0000FF);

        assertEquals(255.0, actual, 0.0001);
    }

    // -----------------------------
    // Test 6: Small difference
    // -----------------------------
    @Test
    public void smallDifference() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        double actual = test.distance(0x010203, 0x020304);

        assertEquals(Math.sqrt(3), actual, 0.0001);
    }

    // -----------------------------
    // Test 7: Invalid negative input
    // -----------------------------
    @Test(expected = IllegalArgumentException.class)
    public void negativeColor() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        test.distance(-1, 0xFFFFFF);
    }

    // -----------------------------
    // Test 8: Invalid too large input
    // -----------------------------
    @Test(expected = IllegalArgumentException.class)
    public void tooLargeColor() {
        EuclideanColorDistance test = new EuclideanColorDistance();

        test.distance(0x1000000, 0x000000);
    }
}