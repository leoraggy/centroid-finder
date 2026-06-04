package io.github.leoraggy.centroidfinder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * Code Overview: VideoSummaryApp
VideoSummaryApp is a command-line computer vision utility that tracks a specific color throughout a video file and logs its movement to a CSV file.

How It Works:
1. Parses Inputs: Accepts 4 runtime arguments: input video path, output CSV path, target hex color (e.g., FF0000), and a color variance threshold.
2. Frames Sampling: Opens the video using JavaCV/FFmpeg and decodes exactly 1 out of every 60 frames to optimize performance.
3. Binarization: Uses a 3D Euclidean formula to calculate how close each pixel's color is to the target. Pixels within the threshold become 1 (active); others become 0 (background).
4. Object Detection: Runs a Depth-First Search (DFS) grouping algorithm to clump adjacent active pixels into shapes.
5. Data Export: Identifies the single largest group in the frame and writes its size and center coordinates (centroid_x, centroid_y) as a new row in the output CSV.

Quick Facts:
Coordinate System: Standard graphics space where (0,0) is the top-left corner.

Fail-Safe: Flushes the CSV writer on every frame to prevent data loss if the process is interrupted.

Exit Codes: Returns 0 on success, 1 on invalid arguments or rendering errors.

Usage: java -jar processor.jar <videoPath> <csvOutputPath> <hexTargetColor> <threshold>
 */
public class VideoSummaryApp {
    public static void main(String[] args) {
        // 1. Enforce strict parameter input checking
        if (args.length < 4) {
            System.err.println("Error: Insufficient engine arguments provided.");
            System.err.println("Usage: java -jar processor.jar <videoPath> <csvOutputPath> <hexTargetColor> <threshold>");
            System.exit(1); 
        }

        // 2. Map command line arguments over to your pipeline variables
        String videoPath      = args[0];
        String csvOutputPath  = args[1];
        String hexTargetColor = args[2];
        int threshold;

        File videoFile = new File(videoPath);
        if (!videoFile.exists() || !videoFile.isFile()) {
            System.err.println("Error: Input video file does not exist or is invalid: " + videoPath);
            System.exit(1);
        }

        try {
            threshold = Integer.parseInt(args[3]);
            if (threshold < 0 || threshold > 441) {
                System.err.println("Error: Threshold must be an integer between 0 and 441.");
                System.exit(1);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid threshold value. Must be a valid integer string.");
            System.exit(1);
        }

        // Parse hex color into a 24-bit integer
        int targetColor = 0;
        try {
            targetColor = Integer.parseInt(hexTargetColor, 16);
        } catch (NumberFormatException e) {
            System.err.println("Invalid hex target color. Please use RRGGBB format.");
            System.exit(1);
            return;
        }

        // Mute noisy native FFmpeg background log statements
        org.bytedeco.javacv.FFmpegLogCallback.setLevel(org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR);

        // Initialize the core processing pipeline components
        ColorDistanceFinder distanceFinder = new EuclideanColorDistance();
        ImageBinarizer binarizer = new DistanceImageBinarizer(distanceFinder, targetColor, threshold);
        ImageGroupFinder groupFinder = new BinarizingImageGroupFinder(binarizer, new DfsBinaryGroupFinder());

        // 3. Dynamically resolve output parent folder based on the passed CSV string path
        File csvFile = new File(csvOutputPath);
        File parentDir = csvFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (csvFile.exists() && csvFile.isFile()) {
            System.out.println("Warning: Output CSV file already exists. Overwriting: " + csvOutputPath);
        }

        // Open the CSV file for appending data across all frames
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile, false))) {
            // Write a CSV Header to keep the data organized
            csvWriter.println("group_size,centroid_x,centroid_y");

            System.out.println("Opening video stream: " + videoPath);
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
                grabber.start();

                Java2DFrameConverter converter = new Java2DFrameConverter();
                Frame frame;
                int frameNumber = 0;

                // Loop through the video frame-by-frame
                while ((frame = grabber.grabImage()) != null) {
                    
                    // Process exactly every 60 frames
                    if (frameNumber % 60 == 0) {

                        // Convert JavaCV Frame to standard Java BufferedImage
                        BufferedImage inputImage = converter.convert(frame);

                        if (inputImage != null) {
                            // 1. Connected-Component Logic (DFS Group Finding)
                            List<Group> groups = groupFinder.findConnectedGroups(inputImage);

                            // Find the single largest group using the record's size() method
                            Group largestGroup = null;
                            for (Group group : groups) {
                                if (largestGroup == null || group.size() > largestGroup.size()) {
                                    largestGroup = group;
                                }
                            }

                            // 2. Write only the single largest group's data to the CSV file
                            if (largestGroup != null) {
                                csvWriter.println(largestGroup.toCsvRow());
                            }
                            
                            csvWriter.flush(); // Push data straight to disk
                        }
                    }

                    frameNumber++;
                }

                grabber.stop();
                System.out.println("Video analysis complete! Master spreadsheet saved to: " + csvOutputPath);
                System.exit(0); // Tell Spring Boot process finished cleanly!

            } catch (Exception e) {
                System.err.println("Error decoding video layers.");
                e.printStackTrace();
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Could not initialize master CSV writer stream.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}