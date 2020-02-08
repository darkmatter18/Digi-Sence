package com.arkadip.digisence;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

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
    PreviewView previewView;
    Classifier classifier;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        executor = Executors.newSingleThreadExecutor();
        classifier = new Classifier(Utils.assetFilePath(this, "digimodel.pt"));

        previewView = findViewById(R.id.preview_view);

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
                //ImageAnalysis imageAnalysis = bindAnalysis();
                ImageCapture imageCapture = bindCapture();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                //No error Should be there
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private ImageCapture bindCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
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
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                            Bitmap bitmap1 = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - 14,
                                    bitmap.getHeight() / 2 - 14, 28, 28);
                            Log.d("IMAGE", "BITMAP width " + bitmap1.getWidth());
                            Log.d("IMAGE", "BITMAP height " + bitmap1.getHeight());
                            int res = classifier.predict(bitmap1);
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
