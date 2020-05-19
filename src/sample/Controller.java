package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Controller {

    @FXML
    private Button button;
    // the FXML grayscale checkbox
    @FXML
    private CheckBox grayscale;
    // the FXML grayscale checkbox
    @FXML
    private ImageView histogram;
    // the FXML area for showing the current frame
    @FXML
    private ImageView currentFrame;

    // a timer for acquiring the video stream
    private Timer timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the logo to be loaded
    private Mat logo;
    private Image i,histo;

    @FXML
    protected void startCamera()
    {
        if (!this.cameraActive)
        {
            // start the video capture
            this.capture.open(0);

            // is the video stream available?
            if (this.capture.isOpened())
            {
                this.cameraActive = true;

                // grab a frame every 33 ms (30 frames/sec)
                TimerTask frameGrabber = new TimerTask() {
                    @Override
                    public void run()
                    {
                        i = grabFrame();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                currentFrame.setImage(i);
                            }
                        });
                    }
                };
                this.timer = new Timer();
                this.timer.schedule(frameGrabber, 0, 33);

                // update the button content
                this.button.setText("Stop Camera");
            }
            else
            {
                // log the error
                System.err.println("Impossible to open the camera connection...");
            }
        }
        else
        {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.button.setText("Start Camera");
            // stop the timer
            if (this.timer != null)
            {
                this.timer.cancel();
                this.timer = null;
            }
            // release the camera
            this.capture.release();
            // clean the image area
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    currentFrame.setImage(null);
                }
            });
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Image} to show
     */
    private Image grabFrame()
    {
        // init everything
        Image imageToShow = null;
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened())
        {
            try
            {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty())
                {
                    // if the grayscale checkbox is selected, convert the image
                    // (frame + logo) accordingly
                    if (grayscale.isSelected())
                    {
                        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                    }

                    // show the histogram
                    this.showHistogram(frame, grayscale.isSelected());

                    // convert the Mat object (OpenCV) to Image (JavaFX)
                    imageToShow = mat2Image(frame);
                }

            }
            catch (Exception e)
            {
                // log the (full) error
                System.err.println("ERROR: " + e);
            }
        }

        return imageToShow;
    }

    /**
     * Compute and show the histogram for the given {@link Mat} image
     *
     * @param frame
     *            the {@link Mat} image for which compute the histogram
     * @param gray
     *            is a grayscale image?
     */
    private void showHistogram(Mat frame, boolean gray)
    {
        // split the frames in multiple images
        List<Mat> images = new ArrayList<>();
        Core.split(frame, images);

        // set the number of bins at 256
        MatOfInt histSize = new MatOfInt(256);
        // only one channel
        MatOfInt channels = new MatOfInt(0);
        // set the ranges
        MatOfFloat histRange = new MatOfFloat(0, 256);

        // compute the histograms for the B, G and R components
        Mat hist_b = new Mat();
        Mat hist_g = new Mat();
        Mat hist_r = new Mat();

        // B component or gray image
        Imgproc.calcHist(images.subList(0, 1), channels, new Mat(), hist_b, histSize, histRange, false);

        // G and R components (if the image is not in gray scale)
        if (!gray)
        {
            Imgproc.calcHist(images.subList(1, 2), channels, new Mat(), hist_g, histSize, histRange, false);
            Imgproc.calcHist(images.subList(2, 3), channels, new Mat(), hist_r, histSize, histRange, false);
        }

        // draw the histogram
        int hist_w = 150; // width of the histogram image
        int hist_h = 150; // height of the histogram image
        int bin_w = (int) Math.round(hist_w / histSize.get(0, 0)[0]);

        Mat histImage = new Mat(hist_h, hist_w, CvType.CV_8UC3, new Scalar(0, 0, 0));
        // normalize the result to [0, histImage.rows()]
        Core.normalize(hist_b, hist_b, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());

        // for G and R components
        if (!gray)
        {
            Core.normalize(hist_g, hist_g, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());
            Core.normalize(hist_r, hist_r, 0, histImage.rows(), Core.NORM_MINMAX, -1, new Mat());
        }

        // effectively draw the histogram(s)
        for (int i = 1; i < histSize.get(0, 0)[0]; i++)
        {
            // B component or gray image
            Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(hist_b.get(i - 1, 0)[0])), new Point(
                    bin_w * (i), hist_h - Math.round(hist_b.get(i, 0)[0])), new Scalar(255, 0, 0), 2, 8, 0);
            // G and R components (if the image is not in gray scale)
            if (!gray)
            {
                Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(hist_g.get(i - 1, 0)[0])),
                        new Point(bin_w * (i), hist_h - Math.round(hist_g.get(i, 0)[0])), new Scalar(0, 255, 0), 2, 8,
                        0);
                Imgproc.line(histImage, new Point(bin_w * (i - 1), hist_h - Math.round(hist_r.get(i - 1, 0)[0])),
                        new Point(bin_w * (i), hist_h - Math.round(hist_r.get(i, 0)[0])), new Scalar(0, 0, 255), 2, 8,
                        0);
            }
        }

        histo = mat2Image(histImage);

        // display the whole
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                histogram.setImage(histo);
            }
        });

    }

    /**
     * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
     *
     * @param frame
     *            the {@link Mat} representing the current frame
     * @return the {@link Image} to show
     */
    private Image mat2Image(Mat frame)
    {
        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();
        // encode the frame in the buffer, according to the PNG format
        Imgcodecs.imencode(".png", frame, buffer);
        // build and return an Image created from the image encoded in the
        // buffer
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}
