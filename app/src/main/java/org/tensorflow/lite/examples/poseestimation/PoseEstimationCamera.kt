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

import android.content.Context
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.camera.CameraSource
import org.tensorflow.lite.examples.poseestimation.data.Device
import org.tensorflow.lite.examples.poseestimation.ml.ModelType
import org.tensorflow.lite.examples.poseestimation.ml.MoveNet
import org.tensorflow.lite.examples.poseestimation.ml.MoveNetMultiPose
import org.tensorflow.lite.examples.poseestimation.ml.PoseDetector
import org.tensorflow.lite.examples.poseestimation.ml.PoseNet
import org.tensorflow.lite.examples.poseestimation.ml.Type

fun openCamera(
    coroutineScope: CoroutineScope,
    surfaceView: SurfaceView,
    lensFacing: Int,
    currentCameraSource: CameraSource?,
    onCameraSourceCreated: (CameraSource) -> Unit,
    onFPS: (Int) -> Unit,
    onDetected: (Float?, List<Pair<String, Float>>?) -> Unit
) {
    if (currentCameraSource != null) {
        return
    }
    val cameraSource = CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
        override fun onFPSListener(fps: Int) {
            coroutineScope.launch(Dispatchers.Main) {
                onFPS(fps)
            }
        }

        override fun onDetectedInfo(
            personScore: Float?,
            poseLabels: List<Pair<String, Float>>?
        ) {
            onDetected(personScore, poseLabels)
        }
    }).apply {
        prepareCamera(lensFacing)
    }
    onCameraSourceCreated(cameraSource)
    coroutineScope.launch(Dispatchers.Main) {
        cameraSource.resume()
        cameraSource.initCamera()
    }
}

fun createPoseEstimator(
    context: Context,
    modelPos: Int,
    device: Device,
    cameraSource: CameraSource?,
    onShowScore: (Boolean) -> Unit,
    onShowTracker: (Boolean) -> Unit,
    onShowClassifier: (Boolean) -> Unit
) {
    val poseDetector: PoseDetector? = when (modelPos) {
        0 -> {
            // MoveNet Lightning (SinglePose)
            onShowClassifier(true)
            onShowScore(true)
            onShowTracker(false)
            MoveNet.create(context, device, ModelType.Lightning)
        }
        1 -> {
            // MoveNet Thunder (SinglePose)
            onShowClassifier(true)
            onShowScore(true)
            onShowTracker(false)
            MoveNet.create(context, device, ModelType.Thunder)
        }
        2 -> {
            // MoveNet (Lightning) MultiPose
            onShowClassifier(false)
            onShowScore(false)
            onShowTracker(true)
            if (device == Device.GPU) {
                // Handled by caller with a toast.
            }
            MoveNetMultiPose.create(context, device, Type.Dynamic)
        }
        3 -> {
            // PoseNet (SinglePose)
            onShowClassifier(true)
            onShowScore(true)
            onShowTracker(false)
            PoseNet.create(context, device)
        }
        else -> null
    }
    poseDetector?.let { detector ->
        cameraSource?.setDetector(detector)
    }
}
