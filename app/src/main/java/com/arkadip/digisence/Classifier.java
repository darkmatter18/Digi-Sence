package com.arkadip.digisence;

import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

class Classifier {
    private Module model;

    private float[] mean = {0.5f, 0.5f, 0.5f};
    private float[] std = {0.5f, 0.5f, 0.5f};

    Classifier(String modelPath) {
        model = Module.load(modelPath);
    }

    private Tensor preprocessor(Bitmap bitmap) {
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap, this.mean, this.std);
    }

    private int argMax(float[] inputs) {
        int maxIndex = -1;
        float maxvalue = 0.0f;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] > maxvalue) {
                maxIndex = i;
                maxvalue = inputs[i];
            }
        }
        return maxIndex;
    }

    int predict(Bitmap bitmap) {
        Tensor tensor = preprocessor(bitmap);
        Tensor output = model.forward(IValue.from(tensor)).toTensor();
        float[] scores = output.getDataAsFloatArray();
        return argMax(scores);
    }
}
