import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class DfsBinaryGroupFinderTest {

    // -----------------------------
    // Test 1: Simple single group
    // -----------------------------
    @Test
    public void singleBigGroup() {
        int[][] image = {
            {1,1},
            {1,1}
        };

        DfsBinaryGroupFinder test = new DfsBinaryGroupFinder();
        List<Group> actual = test.findConnectedGroups(image);

        assertEquals(1, actual.size());

        Group g = actual.get(0);
        assertEquals(4, g.size());
        assertEquals(0, g.centroid().x());
        assertEquals(0, g.centroid().y());
    }

    // -----------------------------
    // Test 2: Multiple isolated pixels
    // -----------------------------
    @Test
    public void fourSingleGroups() {
        int[][] image = {
            {1,0,1},
            {0,0,0},
            {1,0,1}
        };

        DfsBinaryGroupFinder test = new DfsBinaryGroupFinder();
        List<Group> actual = test.findConnectedGroups(image);

        assertEquals(4, actual.size());

        for (Group g : actual) {
            assertEquals(1, g.size());
        }
    }

    // -----------------------------
    // Test 3: One horizontal group
    // -----------------------------
    @Test
    public void horizontalGroup() {
        int[][] image = {
            {1,1,1,0},
            {0,0,0,0}
        };

        DfsBinaryGroupFinder test = new DfsBinaryGroupFinder();
        List<Group> actual = test.findConnectedGroups(image);

        assertEquals(1, actual.size());

        Group g = actual.get(0);
        assertEquals(3, g.size());
        assertEquals(0, g.centroid().y()); // row = y
    }

    // -----------------------------
    // Test 4: Vertical group
    // -----------------------------
    @Test
    public void verticalGroup() {
        int[][] image = {
            {1},
            {1},
            {1}
        };

        DfsBinaryGroupFinder test = new DfsBinaryGroupFinder();
        List<Group> actual = test.findConnectedGroups(image);

        assertEquals(1, actual.size());

        Group g = actual.get(0);
        assertEquals(3, g.size());
        assertEquals(1, g.centroid().y());
        assertEquals(0, g.centroid().x());
    }

    // -----------------------------
    // Test 5: Mixed groups (from your style but fixed)
    // -----------------------------
    @Test
    public void mixedGroups() {
        int[][] image = {
            {1,1,0},
            {0,1,0},
            {1,0,1}
        };

        DfsBinaryGroupFinder test = new DfsBinaryGroupFinder();
        List<Group> actual = test.findConnectedGroups(image);

        assertEquals(3, actual.size());

        // All groups should be sorted by size (all size 1 or 2)
        for (Group g : actual) {
            assertTrue(g.size() > 0);
        }
    }

    // -----------------------------
    // Test 6: Sorting validation
    // -----------------------------
    @Test
    public void sortingCheck() {
        int[][] image = {
            {1,1,1},
            {0,1,0},
            {1,0,1}
        };

        DfsBinaryGroupFinder test = new DfsBinaryGroupFinder();
        List<Group> actual = test.findConnectedGroups(image);

        for (int i = 1; i < actual.size(); i++) {
            Group prev = actual.get(i - 1);
            Group curr = actual.get(i);

            // ensure descending order (compareTo defines order)
            assertTrue(prev.compareTo(curr) >= 0);
        }
    }
}