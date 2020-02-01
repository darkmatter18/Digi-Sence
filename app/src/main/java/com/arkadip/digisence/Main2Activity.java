package com.arkadip.digisence;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;

public class Main2Activity extends AppCompatActivity {
    private static String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }
}
