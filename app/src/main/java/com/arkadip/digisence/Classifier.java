package com.arkadip.digisence;

import android.media.Image;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class Classifier {
    Module model;

    private float[] mean = {0.5f, };
    private float[] std = {0.5f, };

    public Classifier(String modelPath){
        model = Module.load(modelPath);
    }

    public void setMeanAndStd(float[] mean, float[] std) {
        this.mean = mean;
        this.std = std;
    }

    private Tensor preprocess(Image image){
        return TensorImageUtils.imageYUV420CenterCropToFloat32Tensor(image, 0,
                28, 28,this.mean, this.std);
    }

    private int argMax(float[] inputs){
        int maxIndex = -1;
        float maxvalue = 0.0f;
        for (int i = 0; i < inputs.length; i++){
            if(inputs[i] > maxvalue) {
                maxIndex = i;
                maxvalue = inputs[i];
            }
        }
        return maxIndex;
    }
    public int predict(Image image){
        Tensor imageTensor = preprocess(image);
        Tensor output = model.forward(IValue.from(imageTensor)).toTensor();
        float[] scores = output.getDataAsFloatArray();

        return argMax(scores);
    }

}
