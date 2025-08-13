package com.example.myapplication.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.slam.ArCoreManager
import com.example.myapplication.slam.FeaturePoint
import kotlinx.coroutines.delay
import org.koin.androidx.compose.getKoin
import timber.log.Timber
import kotlin.math.min

/**
 * A simple SLAM camera screen that shows a live camera preview (CameraX) with
 * an overlay of feature points from ArCoreManager. This keeps room for future
 * Wi‑Fi hovering anchoring by showing Wi‑Fi status in the header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlamCameraScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val koin = getKoin()
    val arCoreManager = koin.get<ArCoreManager>()

    var cameraReady by remember { mutableStateOf(false) }
    val featurePointsState = remember { mutableStateOf<List<FeaturePoint>>(emptyList()) }

    // Camera preview view holder
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }

    // Hardware and permission checks
    val pm = context.packageManager
    val hasCameraHardware = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    // Permission check (MainActivity requests already; here we just gate UI)
    val hasCameraPerm = if (hasCameraHardware) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    } else true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SLAM Camera") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Placeholder Wi‑Fi status indicator (room for hovering extension)
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "Wi‑Fi",
                        tint = Color(0xFF4CAF50)
                    )
                }
            )
        }
    ) { padding ->
        if (!hasCameraHardware || !hasCameraPerm) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val msg = if (!hasCameraHardware) {
                    "Camera hardware not available. Showing SLAM without camera."
                } else {
                    "Camera permission is not granted. Showing SLAM without camera."
                }
                Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                SlamOverlay(featurePointsState)
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val pv = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }
                    previewViewState.value = pv
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .build()
                            preview.setSurfaceProvider(pv.surfaceProvider)

                            val selector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                            cameraReady = true
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to start CameraX preview")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    pv
                },
                modifier = Modifier.fillMaxSize()
            )

            // SLAM overlay
            SlamOverlay(featurePointsState)

            // Simple HUD text
            androidx.compose.material3.Text(
                text = "Features: ${featurePointsState.value.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0x66000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    // Poll ARCore for motion and features and update overlay
    LaunchedEffect(Unit) {
        while (true) {
            try {
                // Update motion (returns null if session paused/not ready)
                arCoreManager.update()
                // Get feature points (mocked if ARCore not yet active)
                val points = arCoreManager.getFeaturePoints()
                featurePointsState.value = points
            } catch (t: Throwable) {
                Timber.w(t, "SLAM update loop warning")
            }
            delay(100L)
        }
    }
}

@Composable
private fun SlamOverlay(featurePointsState: MutableState<List<FeaturePoint>>) {
    // Draw simple points as white circles over camera feed
    val points by featurePointsState
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val radius = min(w, h) * 0.004f
        // Map synthetic feature (x,y in approx meters) to screen space center
        val cx = w / 2f
        val cy = h / 2f
        val scale = min(w, h) * 0.3f // arbitrary visualization scale
        for (p in points) {
            val sx = cx + p.x * scale
            val sy = cy - p.y * scale
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = radius, center = androidx.compose.ui.geometry.Offset(sx, sy))
        }
    }
}
