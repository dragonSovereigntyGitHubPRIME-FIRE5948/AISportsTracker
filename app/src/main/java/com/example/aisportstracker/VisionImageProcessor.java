package com.example.aisportstracker;

import android.graphics.Bitmap;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.camera.core.ImageProxy;
import com.google.mlkit.common.MlKitException;

import org.w3c.dom.Text;

import java.nio.ByteBuffer;

/** An interface to process the images with different vision detectors and custom image models. */
public interface VisionImageProcessor {

    /** Processes a bitmap image. */
    void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /** Processes ByteBuffer image data, e.g. used for Camera1 live preview case. */
    void processByteBuffer(
            ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay)
            throws MlKitException;

    /** Processes ImageProxy image data, e.g. used for CameraX live preview case. */
    void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) throws MlKitException;

    //// My Functions
    /** Processes ImageProxy image data, e.g. used for CameraX live preview case. */
    void processImageProxyWithoutOverlay(ImageProxy image, TextView tvReps, TextView tvAccuracy, ProgressBar bar) throws MlKitException;

    /** Stops the underlying machine learning model and release resources. */
    void stop();
}
