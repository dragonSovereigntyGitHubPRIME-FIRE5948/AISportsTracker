package com.example.aisportstracker.kotlin.posedetector

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.example.aisportstracker.GraphicOverlay
import com.example.aisportstracker.java.classification.PoseClassifierProcessor
import com.example.aisportstracker.kotlin.VisionProcessorBase
import com.google.android.gms.tasks.Task
import com.google.android.odml.image.MlImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Handler

/** A processor to run pose detector. */
class PoseDetectorProcessor(
    private val context: Context,
    options: PoseDetectorOptionsBase,
    private val showInFrameLikelihood: Boolean,
    private val visualizeZ: Boolean,
    private val rescaleZForVisualization: Boolean,
    private val runClassification: Boolean,
    private val isStreamMode: Boolean
) : VisionProcessorBase<PoseDetectorProcessor.PoseWithClassification>(context) {

    private val detector: PoseDetector
    private val classificationExecutor: Executor

    private var poseClassifierProcessor: PoseClassifierProcessor? = null

    /** Internal class to hold Pose and classification results. */
    class PoseWithClassification(val pose: Pose, val classificationResult: List<String>)

    init {
        detector = PoseDetection.getClient(options)
        classificationExecutor = Executors.newSingleThreadExecutor()
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<PoseWithClassification> {
        return detector
            .process(image)
            .continueWith(
                classificationExecutor
            ) { task ->
                val pose = task.getResult()
                var classificationResult: List<String> = ArrayList()
                if (runClassification) {
                    if (poseClassifierProcessor == null) {
                        poseClassifierProcessor = PoseClassifierProcessor(context, isStreamMode)
                    }
                    classificationResult = poseClassifierProcessor!!.getPoseResult(pose)
                }
                PoseWithClassification(pose, classificationResult)
            }
    }

    override fun detectInImage(image: MlImage): Task<PoseWithClassification> {
        return detector
            .process(image)
            .continueWith(
                classificationExecutor
            ) { task ->
                val pose = task.getResult()
                var classificationResult: List<String> = ArrayList()
                if (runClassification) {
                    if (poseClassifierProcessor == null) {
                        poseClassifierProcessor = PoseClassifierProcessor(context, isStreamMode)
                    }
                    classificationResult = poseClassifierProcessor!!.getPoseResult(pose)
                }
                PoseWithClassification(pose, classificationResult)
            }
    }


    // My Functions
    override fun onSuccessMY(
        poseWithClassification: PoseWithClassification,
        tvReps: TextView,
        tvAccuracy: TextView,
        bar: ProgressBar
    ) {
        // 1. update reps
        tvReps.setText(poseWithClassification.classificationResult[0])

        // 2. exercise accuracy progress bar
        // Before clicking the button the progress bar will invisible
        // so we have to change the visibility of the progress bar to visible
        // setting the progressbar visibility to visible
        if (poseWithClassification.classificationResult.size > 1) {
            bar.visibility = View.VISIBLE
//            Thread(Runnable {
                //3.
                var accuracy = poseWithClassification.classificationResult[1].toFloat().toInt()*10
                bar.progress = accuracy
                tvAccuracy.setText(accuracy.toString() + "% Accurate")
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
//            })
//                .start()
        }
    }

    override fun onSuccess(
        poseWithClassification: PoseWithClassification,
        graphicOverlay: GraphicOverlay,
    ) {
        graphicOverlay.add(
            PoseGraphic(
                graphicOverlay,
                poseWithClassification.pose,
                showInFrameLikelihood,
                visualizeZ,
                rescaleZForVisualization,
                poseWithClassification.classificationResult
            )
        )
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Pose detection failed!", e)
    }

    override fun isMlImageEnabled(context: Context?): Boolean {
        // Use MlImage in Pose Detection by default, change it to OFF to switch to InputImage.
        return true
    }

    companion object {
        private val TAG = "PoseDetectorProcessor"
    }
}