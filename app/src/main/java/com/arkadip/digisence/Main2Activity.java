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


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
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

    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    ProcessCameraProvider cameraProvider;
    Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        cameraExecutor = Executors.newSingleThreadExecutor();
        classifier = new Classifier(Utils.assetFilePath(this, "digimodel.pt"));

        previewView = findViewById(R.id.preview_view);
        textView = findViewById(R.id.textView);

        // Wait for the views to be properly laid out
        previewView.post(() -> setUpCamera());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    /**
     * Initialize CameraX, and prepare to bind the camera use cases
     */
    private void setUpCamera(){
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
        Log.d("DISPLAY", "Screen metrics: "+displayMetrics.widthPixels
                + " x "+ displayMetrics.heightPixels);

        int aspectRatio = aspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels);
        Log.d("DISPLAY", "Preview aspect ratio: "+aspectRatio);

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
        imageAnalysis.setAnalyzer(cameraExecutor, new DLAnalyzer());

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        preview.setPreviewSurfaceProvider(previewView);
    }

    /**
     * Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     * of preview ratio to one of the provided values.
     * @param width - preview width
     * @param height - preview height
     * @return suitable aspect ratio
     */
    private int aspectRatio(int width, int height){
        double RATIO_4_3_VALUE = 4.0/3.0;
        double RATIO_16_9_VALUE = 16.0 / 9.0;

        double previewRatio = (double)Math.max(width, height)/ Math.min(width, height);
        if(Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)){
            return AspectRatio.RATIO_4_3;
        }
        else {
            return AspectRatio.RATIO_16_9;
        }
    }

//    private ImageCapture bindCapture() {
//        ImageCapture imageCapture = new ImageCapture.Builder()
//                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
//                .setTargetResolution(new Size(28, 28))
//                .build();
//
//        previewView.setOnClickListener(v -> {
//            Log.d("Image", "Clicked");
//            imageCapture.takePicture(cameraExecutor,
//                    new ImageCapture.OnImageCapturedCallback() {
//                        @Override
//                        public void onCaptureSuccess(@NonNull ImageProxy image) {
//                            //super.onCaptureSuccess(image);
//                            Log.d("Image", "Captured Successfully");
//                            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
//                            byte[] bytes = new byte[byteBuffer.remaining()];
//                            byteBuffer.get(bytes);
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
//                                    bytes.length, null);
//
//                            Matrix matrix = new Matrix();
//                            matrix.preRotate(90.0f);
//
//                            Bitmap bitmap1 = Bitmap.createBitmap(bitmap,
//                                    bitmap.getWidth() / 2 - 14, bitmap.getHeight() / 2 - 14,
//                                    28, 28, matrix, true);
//
//                            int res = classifier.predict(bitmap1);
//                            runOnUiThread(() -> textView.setText(String.valueOf(res)));
//                            Log.i("CLASSIFIER", "Result " + res);
//                            image.close();
//                        }
//
//                        @Override
//                        public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
//                            super.onError(imageCaptureError, message, cause);
//                            Log.e("Image", message);
//                        }
//                    });
//        });
//
//        return imageCapture;
//    }
}
