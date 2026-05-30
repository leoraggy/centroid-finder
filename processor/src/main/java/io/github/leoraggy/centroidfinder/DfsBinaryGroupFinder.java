package io.github.leoraggy.centroidfinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DfsBinaryGroupFinder implements BinaryGroupFinder {
   /**
    * Finds connected pixel groups of 1s in an integer array representing a binary image.
    * 
    * The input is a non-empty rectangular 2D array containing only 1s and 0s.
    * If the array or any of its subarrays are null, a NullPointerException
    * is thrown. If the array is otherwise invalid, an IllegalArgumentException
    * is thrown.
    *
    * Pixels are considered connected vertically and horizontally, NOT diagonally.
    * The top-left cell of the array (row:0, column:0) is considered to be coordinate
    * (x:0, y:0). Y increases downward and X increases to the right. For example,
    * (row:4, column:7) corresponds to (x:7, y:4).
    *
    * The method returns a list of sorted groups. The group's size is the number 
    * of pixels in the group. The centroid of the group
    * is computed as the average of each of the pixel locations across each dimension.
    * For example, the x coordinate of the centroid is the sum of all the x
    * coordinates of the pixels in the group divided by the number of pixels in that group.
    * Similarly, the y coordinate of the centroid is the sum of all the y
    * coordinates of the pixels in the group divided by the number of pixels in that group.
    * The division should be done as INTEGER DIVISION.
    *
    * The groups are sorted in DESCENDING order according to Group's compareTo method.
    * 
    * @param image a rectangular 2D array containing only 1s and 0s
    * @return the found groups of connected pixels in descending order
    */
    @Override
    public List<Group> findConnectedGroups(int[][] image) {  
        // edge cases
        if (image == null || image.length == 0 || image[0] == null) throw new NullPointerException();    

        List<Group> connectedGroups = new ArrayList<>();

        // Traverses every cell
        for(int row = 0; row < image.length; row++){
            for(int col = 0; col < image[0].length; col++){
                // check if its a 0 or 1
                if (image[row][col] != 0 && image[row][col] != 1) throw new NullPointerException();

                // Starts dfs when finding unvisited 1
                if(image[row][col] == 1){

                    // stats[0] = size
                    // stats[1] = sum of x values 
                    // stats[2] = sum of y values 
                    int[] stats = new int[3];

                    dfs(image, row, col, stats);

                    // calculate centroids
                    int size = stats[0];
                    int centroidX = stats[1] / size;
                    int centroidY = stats[2] / size;

                    // Create and store group
                    connectedGroups.add(new Group(size, new Coordinate(centroidX, centroidY)));
                }
            }
        }
        
        // sort by descending order
        Collections.sort(connectedGroups, Collections.reverseOrder());

        return connectedGroups;
    }

    private void dfs(int[][] image, int row, int col, int[] stats) {
        // Check if, out of bounds or if cell == 0
        if (row < 0 || col < 0 || row >= image.length || 
        col >= image[0].length || image[row][col] == 0) {
            return;
        }

        // Mark current cell as visited
        image[row][col] = 0;
        
        // Update group statistics
        stats[0]++;         // size
        stats[1] += col;    //  x sum
        stats[2] += row;    //  y sum

        // Explore possible moves
        dfs(image, row - 1, col, stats); // UP
        dfs(image, row + 1, col, stats); // DOWN
        dfs(image, row, col - 1, stats); // LEFT
        dfs(image, row, col + 1, stats); // RIGHT

    }

    }

    

