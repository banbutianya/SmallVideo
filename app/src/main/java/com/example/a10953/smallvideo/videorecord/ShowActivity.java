package com.example.a10953.smallvideo.videorecord;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

import com.example.a10953.smallvideo.R;

public class ShowActivity extends AppCompatActivity {

    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        initView();
    }

    private void initView(){
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
    }
}
