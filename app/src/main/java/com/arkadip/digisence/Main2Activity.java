/*
 * Copyright 2020 Arkadip Bhattacharya
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arkadip.digisence;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main2Activity extends AppCompatActivity {

    ExecutorService cameraExecutor;
    Classifier classifier;

    PreviewView previewView;
    TextView textView;
    ProcessCameraProvider cameraProvider;
    Camera camera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        cameraExecutor = Executors.newSingleThreadExecutor();
        classifier = new Classifier(Utils.assetFilePath(this, "digimodel.pt"));

        previewView = findViewById(R.id.preview_view);
        textView = findViewById(R.id.textView);

        // Wait for the views to be properly laid out
        previewView.post(this::setUpCamera);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    /**
     * Initialize CameraX, and prepare to bind the camera use cases
     */
    private void setUpCamera() {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderListenableFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            try {
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() throws ExecutionException, InterruptedException {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(displayMetrics);
        Log.d("DISPLAY", "Screen metrics: " + displayMetrics.widthPixels
                + " x " + displayMetrics.heightPixels);

        int aspectRatio = aspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels);
        Log.d("DISPLAY", "Preview aspect ratio: " + aspectRatio);

        int rotation = previewView.getDisplay().getRotation();

        // CameraProvider
        cameraProvider = cameraProviderListenableFuture.get();

        // CameraSelector
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .build();

        // ImageAnalysis
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void analyze(@NonNull ImageProxy image) {
                Log.d("IMAGE", "Image got");
                int rotation = image.getImageInfo().getRotationDegrees();
                int res = classifier.predict(image.getImage(), rotation);
                runOnUiThread(() -> {
                    textView.setText(String.valueOf(res));
                });
                image.close();
            }
        });

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
    }

    /**
     * Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     * of preview ratio to one of the provided values.
     *
     * @param width  - preview width
     * @param height - preview height
     * @return suitable aspect ratio
     */
    private int aspectRatio(int width, int height) {
        double RATIO_4_3_VALUE = 4.0 / 3.0;
        double RATIO_16_9_VALUE = 16.0 / 9.0;

        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        } else {
            return AspectRatio.RATIO_16_9;
        }
    }
}
