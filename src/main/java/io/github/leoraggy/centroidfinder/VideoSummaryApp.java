package io.github.leoraggy.centroidfinder;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

public class VideoSummaryApp {
    public static void main(String[] args) {

        String videoPath = "sampleInput/ensantina.mp4";

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {

            grabber.start();

            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            Frame frame;
            int frameNumber = 0;

            while ((frame = grabber.grabImage()) != null) {
                Mat mat = converter.convert(frame);
                System.out.println("Read frame: " + frameNumber);

                if (frameNumber > 0 && frameNumber % 60 == 0) {
                    
                    String outputPath = "sampleOutput/frame_" + frameNumber + ".jpg";
                    
                    opencv_imgcodecs.imwrite(outputPath, mat);
                    System.out.println("Saved frame " + frameNumber + " for processing.");

                    String[] nextAppArgs = { outputPath, String.valueOf(frameNumber) };

                    YourOtherClassName.main(nextAppArgs); 
                }

                if (frameNumber == 0) {
                    opencv_imgcodecs.imwrite("sampleOutput/firstFrame.jpg", mat);
                    System.out.println("Saved first frame!");
                }

                frameNumber++;

                if (frameNumber >= 200) { 
                    break;
                }
            }

            grabber.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}