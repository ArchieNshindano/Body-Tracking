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

package org.tensorflow.lite.examples.poseestimation.ui

import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.tensorflow.lite.examples.poseestimation.R
import org.tensorflow.lite.examples.poseestimation.data.Device
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseEstimationContent(
    hasCameraPermission: Boolean,
    fps: Int,
    score: Float,
    modelPos: Int,
    device: Device,
    trackerPos: Int,
    isClassifyPose: Boolean,
    poseLabels: List<Pair<String, Float>>,
    showScore: Boolean,
    showTracker: Boolean,
    showClassifierOption: Boolean,
    onModelChange: (Int) -> Unit,
    onDeviceChange: (Device) -> Unit,
    onTrackerChange: (Int) -> Unit,
    onClassificationChange: (Boolean) -> Unit,
    onSurfaceReady: (SurfaceView) -> Unit,
    onSwapCamera: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetContainerColor = Color.White,
        sheetContentColor = Color.Black,
        sheetContent = {
            ControlPanel(
                fps = fps,
                score = score,
                modelPos = modelPos,
                device = device,
                trackerPos = trackerPos,
                isClassifyPose = isClassifyPose,
                poseLabels = poseLabels,
                showScore = showScore,
                showTracker = showTracker,
                showClassifierOption = showClassifierOption,
                onModelChange = onModelChange,
                onDeviceChange = onDeviceChange,
                onTrackerChange = onTrackerChange,
                onClassificationChange = onClassificationChange
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx)
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = onSurfaceReady
                )
            }

            // Toolbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.tfl2_logo),
                        contentDescription = null
                    )
                    TextButton(
                        onClick = onSwapCamera,
                        enabled = hasCameraPermission
                    ) {
                        Text(text = "Swap")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanel(
    fps: Int,
    score: Float,
    modelPos: Int,
    device: Device,
    trackerPos: Int,
    isClassifyPose: Boolean,
    poseLabels: List<Pair<String, Float>>,
    showScore: Boolean,
    showTracker: Boolean,
    showClassifierOption: Boolean,
    onModelChange: (Int) -> Unit,
    onDeviceChange: (Device) -> Unit,
    onTrackerChange: (Int) -> Unit,
    onClassificationChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(text = stringResource(id = R.string.tfe_pe_tv_fps, fps))
        if (showScore) {
            Text(text = stringResource(id = R.string.tfe_pe_tv_score, score))
        }

        // Device Spinner
        SpinnerRow(
            label = stringResource(id = R.string.tfe_pe_tv_device),
            options = stringArrayResource(id = R.array.tfe_pe_device_name).toList(),
            selectedOption = when (device) {
                Device.CPU -> 0
                Device.GPU -> 1
                Device.NNAPI -> 2
            },
            onOptionSelected = {
                val targetDevice = when (it) {
                    0 -> Device.CPU
                    1 -> Device.GPU
                    else -> Device.NNAPI
                }
                onDeviceChange(targetDevice)
            }
        )

        // Model Spinner
        SpinnerRow(
            label = stringResource(id = R.string.tfe_pe_tv_model),
            options = stringArrayResource(id = R.array.tfe_pe_models_array).toList(),
            selectedOption = modelPos,
            onOptionSelected = onModelChange
        )

        // Tracker Spinner
        if (showTracker) {
            SpinnerRow(
                label = stringResource(id = R.string.tfe_pe_tv_tracking),
                options = stringArrayResource(id = R.array.tfe_pe_tracker_array).toList(),
                selectedOption = trackerPos,
                onOptionSelected = onTrackerChange
            )
        }

        // Classification Switch
        if (showClassifierOption) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(id = R.string.tfe_pe_tv_pose_classification))
                Switch(checked = isClassifyPose, onCheckedChange = onClassificationChange)
            }

            if (isClassifyPose) {
                poseLabels.sortedByDescending { it.second }.take(3).forEach { label ->
                    Text(
                        text = stringResource(
                            id = R.string.tfe_pe_tv_classification_value,
                            "${label.first} (${String.format(Locale.US, "%.2f", label.second)})"
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerRow(
    label: String,
    options: List<String>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Box {
            Text(
                text = options[selectedOption],
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(8.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            onOptionSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
