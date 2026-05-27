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
 * The Video Summary Application (Accepting dynamic runtime arguments).
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

        try {
            threshold = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid threshold value. Must be a valid integer string.");
            System.exit(1);
            return;
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
                            // 1. Binarization Engine
                            int[][] binaryArray = binarizer.toBinaryArray(inputImage);
                            BufferedImage binaryImage = binarizer.toBufferedImage(binaryArray);

                            // Save binarized visual frame adjacent to output CSV folder path
                            // if (parentDir != null) {
                            //     String binarizedFramePath = parentDir.getAbsolutePath() + File.separator + "binarized_frame_" + frameNumber + ".png";
                            //     ImageIO.write(binaryImage, "png", new File(binarizedFramePath));
                            // }

                            // 2. Connected-Component Logic (DFS Group Finding)
                            List<Group> groups = groupFinder.findConnectedGroups(inputImage);

                            // Find the single largest group using the record's size() method
                            Group largestGroup = null;
                            for (Group group : groups) {
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