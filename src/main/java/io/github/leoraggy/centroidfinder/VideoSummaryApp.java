package io.github.leoraggy.centroidfinder;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * The Video Summary Application (Tracking Largest Centroid Only).
 */
public class VideoSummaryApp {
    public static void main(String[] args) {

     org.bytedeco.javacv.FFmpegLogCallback.setLevel(org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR);
        String videoPath = "sampleInput/ensantina.mp4";
        String hexTargetColor = "48010c";
        int threshold = 30;

        // Parse hex color into a 24-bit integer
        int targetColor = 0;
        try {
            targetColor = Integer.parseInt(hexTargetColor, 16);
        } catch (NumberFormatException e) {
            System.err.println("Invalid hex target color. Please use RRGGBB format.");
            return;
        }

        // Initialize the core processing pipeline components
        ColorDistanceFinder distanceFinder = new EuclideanColorDistance();
        ImageBinarizer binarizer = new DistanceImageBinarizer(distanceFinder, targetColor, threshold);
        ImageGroupFinder groupFinder = new BinarizingImageGroupFinder(binarizer, new DfsBinaryGroupFinder());

        // Create the output directories if they don't exist
        File outputDir = new File("sampleOutput");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String csvOutputPath = "sampleOutput/video_summary.csv";

        // Open the CSV file for appending data across all frames
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvOutputPath, false))) {
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
                            // 1. Replicate Binarization Engine
                            int[][] binaryArray = binarizer.toBinaryArray(inputImage);
                            BufferedImage binaryImage = binarizer.toBufferedImage(binaryArray);

                            // Save binarized visual frame to disk
                            String binarizedFramePath = "sampleOutput/binarized_frame_" + frameNumber + ".png";
                            ImageIO.write(binaryImage, "png", new File(binarizedFramePath));

                         // 2. Replicate Connected-Component Logic (DFS Group Finding)
                            List<Group> groups = groupFinder.findConnectedGroups(inputImage);

                            // --- NEW FILTER LOGIC: Find the single largest group using the record's size() method ---
                            Group largestGroup = null;

                            for (Group group : groups) {
                                // Calling the auto-generated record method group.size()
                                if (largestGroup == null || group.size() > largestGroup.size()) {
                                    largestGroup = group;
                                }
                            }
                            // 3. Write only the single largest group's data to the CSV file
                            if (largestGroup != null) {
                                csvWriter.println(largestGroup.toCsvRow());
                            }
                            
                            csvWriter.flush(); // Push data straight to disk
                        }
                    }

                    frameNumber++;
                }

                grabber.stop();
                System.out.println("\nVideo analysis complete!");
                System.out.println("Master spreadsheet saved to: " + csvOutputPath);

            } catch (Exception e) {
                System.err.println("Error decoding video layers.");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Could not initialize master CSV writer stream.");
            e.printStackTrace();
        }
    }
}