package io.github.leoraggy.centroidfinder;

import java.io.File;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class JavaCVPlayground {public static void main(String[] args) {
        String videoPath = "my_video.mp4";

        // 1. Setup the grabber
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();

            // 2. Setup a converter (converts raw frames to Java Images)
            Java2DFrameConverter converter = new Java2DFrameConverter();

            Frame frame;
            int frameCount = 0;

            // 3. The extraction loop
            while ((frame = grabber.grabImage()) != null) {
                // Treat this frame as an image
                var bufferedImage = converter.convert(frame);

                if (bufferedImage != null) {
                    // 4. Save the "image" to disk
                    File output = new File("frames/frame_" + frameCount + ".jpg");
                    ImageIO.write(bufferedImage, "jpg", output);
                    
                    System.out.println("Saved frame: " + frameCount);
                    frameCount++;
                }
            }
            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
