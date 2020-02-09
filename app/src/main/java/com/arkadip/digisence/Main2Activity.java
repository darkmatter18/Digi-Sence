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


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main2Activity extends AppCompatActivity {

    Executor executor;
    Classifier classifier;

    PreviewView previewView;
    TextView textView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        executor = Executors.newSingleThreadExecutor();
        classifier = new Classifier(Utils.assetFilePath(this, "digimodel.pt"));

        previewView = findViewById(R.id.preview_view);
        textView = findViewById(R.id.textView);

        startCamera();
    }

    private void startCamera() {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                Preview preview = bindPreview();
                ImageCapture imageCapture = bindCapture();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                //No error Should be there
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private ImageCapture bindCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setTargetResolution(new Size(28, 28))
                .build();

        previewView.setOnClickListener(v -> {
            Log.d("Image", "Clicked");
            imageCapture.takePicture(executor,
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            //super.onCaptureSuccess(image);
                            Log.d("Image", "Captured Successfully");
                            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[byteBuffer.remaining()];
                            byteBuffer.get(bytes);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
                                    bytes.length, null);

                            Matrix matrix = new Matrix();
                            matrix.preRotate(90.0f);

                            Bitmap bitmap1 = Bitmap.createBitmap(bitmap,
                                    bitmap.getWidth() / 2 - 14, bitmap.getHeight() / 2 - 14,
                                    28, 28, matrix, true);

                            int res = classifier.predict(bitmap1);
                            runOnUiThread(() -> textView.setText(String.valueOf(res)));
                            Log.i("CLASSIFIER", "Result " + res);
                            image.close();
                        }

                        @Override
                        public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            super.onError(imageCaptureError, message, cause);
                            Log.e("Image", message);
                        }
                    });
        });

        return imageCapture;
    }

    private Preview bindPreview() {
        Preview preview = new Preview.Builder()
                .setTargetName("Preview")
                .build();
        preview.setPreviewSurfaceProvider(previewView.getPreviewSurfaceProvider());
        return preview;
    }
}
