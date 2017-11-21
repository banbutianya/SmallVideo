package com.example.a10953.smallvideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.example.a10953.smallvideo.common.utils.TCConstants;
import com.example.a10953.smallvideo.videorecord.TCVideoRecordActivity;
import com.tencent.rtmp.TXLiveBase;

import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";

    private Button camera_button;
    private Button upload;
    private TextView videopath;
    private TextView imagepath;
    private String videopath_text;
    private String imagepath_text;

    private String imageUrl;
    private String videoUrl;
    private String loadImageUrl;
    private String loadVideoUrl;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        Intent i = getIntent();
        if((i.getStringExtra(TCConstants.VIDEO_RECORD_VIDEPATH ) != null && (i.getStringExtra(TCConstants.VIDEO_RECORD_COVERPATH ) != null))) {
            videopath_text = i.getStringExtra(TCConstants.VIDEO_RECORD_VIDEPATH);
            imagepath_text = i.getStringExtra(TCConstants.VIDEO_RECORD_COVERPATH);
            videopath.setText(videopath_text);
            imagepath.setText(imagepath_text);
        }



        String sdkver = TXLiveBase.getSDKVersionStr();
        Log.e(TAG,"liteavsdk , liteav sdk version is : " + sdkver);

        ButtonListener btn = new ButtonListener();

        camera_button.setOnClickListener(btn);
        camera_button.setOnLongClickListener(btn);
        camera_button.setOnTouchListener(btn);
        upload.setOnClickListener(this);

    }

    private void initViews() {
        camera_button = (Button) findViewById(R.id.camera_button);
        upload = (Button) findViewById(R.id.upload);
        videopath = (TextView) findViewById(R.id.videopath);
        imagepath = (TextView) findViewById(R.id.imagepath);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.upload:
                Toast.makeText(MainActivity.this,"上传",Toast.LENGTH_SHORT).show();
                upLoadData();
                break;
        }
    }

    class ButtonListener implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {

        public void onClick(View view) {
            if(view.getId() == R.id.camera_button){
                switch (view.getId()){
                    case R.id.camera_button:
                        Log.e(TAG,"test , 点击了按钮");
                        Intent intent = new Intent(MainActivity.this, TCVideoRecordActivity.class);
                        startActivity(intent);
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if((motionEvent.getEventTime() - motionEvent.getDownTime())> 1000){
                if(view.getId() == R.id.camera_button){
                    if(motionEvent.getAction() == MotionEvent.ACTION_UP){

                        Log.e(TAG,"test , cansal button ---> up");
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onLongClick(View view) {
            switch (view.getId()) {
                case R.id.camera_button:
                    Log.e(TAG,"test , 长按了按钮");
                    break;
            }
            return true;
        }
    }

    private void upLoadData() {

        videoUrl = "BlackCard/SmallVideo/Video/" + getObjectKey("Black_card") + ".mp4";
        //上传以后，外网获取的URL地址
        loadVideoUrl = MainApplication.OSS_BUCKET + ".oss-cn-beijing.aliyuncs.com/" + videoUrl;
        Log.e(TAG,loadVideoUrl);
        PutObjectRequest videoPut = new PutObjectRequest(MainApplication.OSS_BUCKET,videoUrl,videopath_text);


        // 异步上传时可以设置进度回调
        videoPut.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.e(TAG + "PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
                float jindu = currentSize/totalSize;
                Log.e(TAG,"进度是" + jindu);

            }
        });

        MainApplication.oss.asyncPutObject(videoPut, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(final PutObjectRequest request, PutObjectResult result) {
                Log.e("PutObject", "UploadSuccess");
                Log.e("ETag", result.getETag());
                Log.e("RequestId", result.getRequestId());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }

            }
        });

        //imageUrl = "BlackCard/SmallVideo/Cover sheet/" + getObjectKey("Black_card") + ".jpg";
        imageUrl = videoUrl.replace(".mp4",".jpg");
        loadImageUrl = MainApplication.OSS_BUCKET + ".oss-cn-beijing.aliyuncs.com/" + imageUrl;
        Log.e(TAG,loadImageUrl);
        PutObjectRequest imagePut = new PutObjectRequest(MainApplication.OSS_BUCKET, imageUrl, imagepath_text);

        // 异步上传时可以设置进度回调
        imagePut.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.e(TAG + "PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
                long jindu = currentSize/totalSize;

            }
        });

        MainApplication.oss.asyncPutObject(imagePut, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(final PutObjectRequest request, PutObjectResult result) {
                Log.e("PutObject", "UploadSuccess");
                Log.e("ETag", result.getETag());
                Log.e("RequestId", result.getRequestId());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }

            }
        });



    }

    //通过UserCode 加上日期组装 OSS路径
    private String getObjectKey(String strUserCode){
        Date date = new Date();
        return new SimpleDateFormat("yyyy-M-d").format(date)+"/"+strUserCode+new SimpleDateFormat("yyyyMMddssSSS").format(date);
    }
}
