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
import android.media.Image;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.Arrays;

class Classifier {
    private Module model;

    Classifier(String modelPath) {
        model = Module.load(modelPath);
    }

    private Tensor preprocessor(Image image, int rotation) {
        return TensorImageUtils.imageYUV420CenterCropToFloat32Tensor(image, rotation, 28,
                28, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
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

    int predict(Image image, int rotation) {
        Tensor tensor = preprocessor(image, rotation);
        Tensor output = model.forward(IValue.from(tensor)).toTensor();
        float[] scores = output.getDataAsFloatArray();
        Log.d("CLASSIFIER", "Scores: "+ Arrays.toString(scores));
        return argMax(scores);
    }
}
