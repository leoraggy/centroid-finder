package io.github.leoraggy.centroidfinder;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgcodecs;

public class VideoExperiment {

    // Opens Video
    // Reads frames one by one
    // Converts them into images
    // Saves the first frame
    // Prints metadata
    public static void main(String[] args) {

        // Get Video
        String videoPath = "sampleInput/ensantina.mp4";

        // creates video decoder pipeline
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {

            // loads video metadata, prepares decoder, opens file stream
            grabber.start();

            System.out.println("Video opened successfully!");

            // Print metadata
            System.out.println("Width: " + grabber.getImageWidth());
            System.out.println("Height: " + grabber.getImageHeight());
            System.out.println("Frame Rate: " + grabber.getFrameRate());
            System.out.println("Length in Frames: " + grabber.getLengthInFrames());

            // converter for frame (video format) -> mat (pixel matrix) 
            // needed for pixel dfs
            OpenCVFrameConverter.ToMat converter =
                    new OpenCVFrameConverter.ToMat();

            Frame frame;
            int frameNumber = 0;

            // loops and grabs frames until video ends
            while ((frame = grabber.grabImage()) != null) {

                // converts frame to mat
                Mat mat = converter.convert(frame);

                System.out.println("Read frame: " + frameNumber);

                // Save first frame as image
                if (frameNumber == 0) {
                    // writes image to disk to see the first frame
                    opencv_imgcodecs.imwrite(
                            "sampleOutput/firstFrame.jpg",
                            mat
                    );

                    System.out.println("Saved first frame!");
                }

                frameNumber++;

                // Stop after 10 frames for testing
                if (frameNumber >= 10) {
                    break;
                }
            }

            // closes video file
            grabber.stop();

            System.out.println("Done!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}