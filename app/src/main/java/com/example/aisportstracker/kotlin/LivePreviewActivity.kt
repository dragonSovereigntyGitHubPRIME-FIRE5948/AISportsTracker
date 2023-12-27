package com.example.aisportstracker.kotlin

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.example.aisportstracker.CameraSource
import com.example.aisportstracker.CameraSourcePreview
import com.example.aisportstracker.GraphicOverlay
import com.example.aisportstracker.R
import com.example.aisportstracker.kotlin.posedetector.PoseDetectorProcessor
import com.example.aisportstracker.preferences.PreferenceUtils
import com.example.aisportstracker.preferences.SettingsActivity
import com.example.aisportstracker.preferences.SettingsActivity.LaunchSource
import com.google.android.gms.common.annotation.KeepName
import java.io.IOException

/** Live preview demo for ML Kit APIs. */
@KeepName
class LivePreviewActivity :
    AppCompatActivity(), OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var selectedModel = POSE_DETECTION


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_vision_live_preview)

        preview = findViewById(R.id.preview_view)
        if (preview == null) {
            Log.d(TAG, "Preview is null")
        }

        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        val spinner = findViewById<Spinner>(R.id.spinner)
        val options: MutableList<String> = ArrayList()
        options.add(POSE_DETECTION)

        // Creating adapter for spinner
//        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)

        // Drop down layout style - list view with radio button
//        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        // attaching data adapter to spinner
//        spinner.adapter = dataAdapter
//        spinner.onItemSelectedListener = this

        val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
        facingSwitch.setOnCheckedChangeListener(this)

        // Settings Btn
        val settingsButton = findViewById<ImageView>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, LaunchSource.LIVE_PREVIEW)
            startActivity(intent)
        }

        createCameraSource(selectedModel)
    }

    @Synchronized
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        selectedModel = parent?.getItemAtPosition(pos).toString()
        Log.d(TAG, "Selected model: $selectedModel")
        preview?.stop()
        createCameraSource(selectedModel)
        startCameraSource()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Log.d(TAG, "Set facing")
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource?.setFacing(CameraSource.CAMERA_FACING_FRONT)
            } else {
                cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
            }
        }
        preview?.stop()
        startCameraSource()
    }

    private fun createCameraSource(model: String) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
        }
        try {
            when (model) {
                POSE_DETECTION -> {
                    val poseDetectorOptions = PreferenceUtils.getPoseDetectorOptionsForLivePreview(this)
                    Log.i(TAG, "Using Pose Detector with options $poseDetectorOptions")
                    val shouldShowInFrameLikelihood =
                        PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this)
                    val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this)
                    val rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this)
                    val runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this)

                    cameraSource!!.setMachineLearningFrameProcessor(
                        PoseDetectorProcessor(
                            this,
                            poseDetectorOptions,
                            shouldShowInFrameLikelihood,
                            visualizeZ,
                            rescaleZ,
                            runClassification,
                            /* isStreamMode = */ true
                        )
                    )

                }
                else -> Log.e(TAG, "Unknown model: $model")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor: $model", e)
            Toast.makeText(
                applicationContext,
                "Can not create image processor: " + e.message,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    // FUNCTIONS

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        createCameraSource(selectedModel)
        startCameraSource()
    }

    /** Stops the camera. */
    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource?.release()
        }
    }

    companion object {
        private const val POSE_DETECTION = "Pose Detection"
        private const val TAG = "LivePreviewActivity"
    }
}