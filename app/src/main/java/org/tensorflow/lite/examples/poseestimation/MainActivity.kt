/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.poseestimation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import org.tensorflow.lite.examples.poseestimation.camera.CameraSource
import org.tensorflow.lite.examples.poseestimation.data.Device
import org.tensorflow.lite.examples.poseestimation.ml.PoseClassifier
import org.tensorflow.lite.examples.poseestimation.ml.TrackerType
import org.tensorflow.lite.examples.poseestimation.ui.PoseEstimationContent

class MainActivity : AppCompatActivity() {

    private var cameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        setContent {
            MaterialTheme {
                PoseEstimationScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PoseEstimationScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var personScore by remember { mutableStateOf(0f) }
        var fps by remember { mutableStateOf(0) }
        var poseLabels by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }

        var modelPos by remember { mutableIntStateOf(1) } // Default: MoveNet Thunder
        var device by remember { mutableStateOf(Device.CPU) }
        var trackerPos by remember { mutableIntStateOf(0) } // Default: Off
        var isClassifyPose by remember { mutableStateOf(false) }

        var showScore by remember { mutableStateOf(true) }
        var showTracker by remember { mutableStateOf(false) }
        var showClassifierOption by remember { mutableStateOf(true) }

        var hasCameraPermission by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }

        var resumeCount by remember { mutableIntStateOf(0) }
        var lensFacing by rememberSaveable { mutableIntStateOf(android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) }
        var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

        fun configureCameraAfterOpen() {
            if (modelPos == 2 && device == Device.GPU) {
                Toast.makeText(context, getString(R.string.tfe_pe_gpu_error), Toast.LENGTH_LONG).show()
            }
            createPoseEstimator(context, modelPos, device, cameraSource,
                onShowScore = { showScore = it },
                onShowTracker = { showTracker = it },
                onShowClassifier = { showClassifierOption = it }
            )
            // If classification was already on, we need to set it
            if (isClassifyPose) {
                cameraSource?.setClassifier(PoseClassifier.create(context))
            }
            // If tracker was already on, we need to set it
            cameraSource?.setTracker(
                when (trackerPos) {
                    1 -> TrackerType.BOUNDING_BOX
                    2 -> TrackerType.KEYPOINTS
                    else -> TrackerType.OFF
                }
            )
        }

        fun restartCamera() {
            val surfaceView = surfaceViewRef ?: return
            cameraSource?.close()
            cameraSource = null
            openCamera(
                coroutineScope = lifecycleScope,
                surfaceView = surfaceView,
                lensFacing = lensFacing,
                currentCameraSource = cameraSource,
                onCameraSourceCreated = { cameraSource = it },
                onFPS = { fps = it },
                onDetected = { score, labels ->
                    personScore = score ?: 0f
                    poseLabels = labels ?: emptyList()
                }
            )
            configureCameraAfterOpen()
        }

        // Handle Lifecycle
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        cameraSource?.resume()
                        if (hasCameraPermission && cameraSource == null) {
                            restartCamera()
                        }
                        resumeCount++
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        cameraSource?.close()
                        cameraSource = null
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                cameraSource?.close()
                cameraSource = null
            }
        }

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, R.string.tfe_pe_request_permission, Toast.LENGTH_LONG).show()
            }
        }

        LaunchedEffect(Unit) {
            if (!hasCameraPermission) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Handle model/device/tracker changes
        LaunchedEffect(modelPos, device) {
            if (modelPos == 2 && device == Device.GPU) {
                Toast.makeText(context, getString(R.string.tfe_pe_gpu_error), Toast.LENGTH_LONG).show()
            }
            createPoseEstimator(context, modelPos, device, cameraSource,
                onShowScore = { showScore = it },
                onShowTracker = { showTracker = it },
                onShowClassifier = { showClassifierOption = it }
            )
        }

        LaunchedEffect(trackerPos) {
            cameraSource?.setTracker(
                when (trackerPos) {
                    1 -> TrackerType.BOUNDING_BOX
                    2 -> TrackerType.KEYPOINTS
                    else -> TrackerType.OFF
                }
            )
        }

        LaunchedEffect(isClassifyPose) {
            cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(context) else null)
        }

        PoseEstimationContent(
            hasCameraPermission = hasCameraPermission,
            fps = fps,
            score = personScore,
            modelPos = modelPos,
            device = device,
            trackerPos = trackerPos,
            isClassifyPose = isClassifyPose,
            poseLabels = poseLabels,
            showScore = showScore,
            showTracker = showTracker,
            showClassifierOption = showClassifierOption,
            onModelChange = { modelPos = it },
            onDeviceChange = { device = it },
            onTrackerChange = { trackerPos = it },
            onClassificationChange = { isClassifyPose = it },
            onSurfaceReady = { sv ->
                surfaceViewRef = sv
                if (cameraSource == null) {
                    openCamera(
                        coroutineScope = lifecycleScope,
                        surfaceView = sv,
                        lensFacing = lensFacing,
                        currentCameraSource = cameraSource,
                        onCameraSourceCreated = { cameraSource = it },
                        onFPS = { fps = it },
                        onDetected = { score, labels ->
                            personScore = score ?: 0f
                            poseLabels = labels ?: emptyList()
                        }
                    )
                    configureCameraAfterOpen()
                }
            },
            onSwapCamera = {
                lensFacing = if (lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
                } else {
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
                }
                restartCamera()
            }
        )
    }
}
