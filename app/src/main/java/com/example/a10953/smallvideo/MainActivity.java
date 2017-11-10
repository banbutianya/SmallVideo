package com.example.a10953.smallvideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.a10953.smallvideo.videorecord.TCVideoSettingActivity;
import com.tencent.rtmp.TXLiveBase;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button camera_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        String sdkver = TXLiveBase.getSDKVersionStr();
        Log.e("liteavsdk", "liteav sdk version is : " + sdkver);


        camera_button.setOnClickListener(this);

    }

    private void initViews() {
        camera_button = (Button) findViewById(R.id.camera_button);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.camera_button:
                Toast.makeText(this,"点击了按钮",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, TCVideoSettingActivity.class);
                startActivity(intent);
                break;
        }
    }

}
