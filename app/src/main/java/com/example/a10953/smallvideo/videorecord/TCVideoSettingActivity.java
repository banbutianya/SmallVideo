//package com.example.a10953.smallvideo.videorecord;
//
//import android.content.Intent;
//import android.content.pm.ActivityInfo;
//import android.content.res.Configuration;
//import android.os.Bundle;
//import android.support.annotation.IdRes;
//import android.support.annotation.Nullable;
//import android.support.v7.app.AppCompatActivity;
//import android.text.TextUtils;
//import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//import android.widget.TextView;
//
//import com.example.a10953.smallvideo.R;
//import com.tencent.liteav.basic.log.TXCLog;
//import com.tencent.ugc.TXRecordCommon;
//
//public class TCVideoSettingActivity extends AppCompatActivity implements View.OnClickListener{
//    private static final String TAG = "TCVideoSettingActivity";
//
//    public static final String RECORD_CONFIG_MAX_DURATION       = "record_config_max_duration";
//    public static final String RECORD_CONFIG_MIN_DURATION       = "record_config_min_duration";
//    public static final String RECORD_CONFIG_ASPECT_RATIO       = "record_config_aspect_ratio";
//    public static final String RECORD_CONFIG_RECOMMEND_QUALITY  = "record_config_recommend_quality";
//    public static final String RECORD_CONFIG_RESOLUTION         = "record_config_resolution";
//    public static final String RECORD_CONFIG_BITE_RATE          = "record_config_bite_rate";
//    public static final String RECORD_CONFIG_FPS                = "record_config_fps";
//    public static final String RECORD_CONFIG_GOP                = "record_config_gop";
//
//    private LinearLayout llBack;
//    private View rlBiterate, rlFps, rlGop;
//    private EditText etBitrate, etGop, etFps;
//    private RadioGroup rgVideoQuality, rgVideoResolution, rgVideoAspectRatio;
//    private RadioButton rbVideoQualitySD, rbVideoQualityHD, rbVideoQualitySSD, rbVideoQulityCustom,
//            rbVideoResolution360p, rbVideoResolution540p, rbVideoResolution720p,
//            rbVideoAspectRatio11, rbVideoAspectRatio34, rbVideoAspectRatio916;
//    private TextView tvRecommendResolution, tvRecommendBitrate, tvRecommendFps, tvRecommendGop;
//    private Button btnOK;
//
//    private int mRecommendQuality = -1;
//    private int mAspectRatio; // 视频比例
//    private int mRecordResolution; // 录制分辨率
//    private int mBiteRate = 2400; // 码率
//    private int mFps = 20; // 帧率
//    private int mGop = 3; // 关键帧间隔
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//
//        //禁用屏幕旋转
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        } else {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
//
//        setContentView(R.layout.activity_video_settings);
//
//        TXCLog.init();
//
//        initData();
//
//        initView();
//
//        initListener();
//
//        initViewStatus();
//    }
//
//    private void initData(){
//        mRecommendQuality = -1;
//    }
//
//    private void initView(){
//        llBack = (LinearLayout) findViewById(R.id.back_ll);
//
//        rlBiterate = findViewById(R.id.rl_bite_rate);
//        rlFps = findViewById(R.id.rl_fps);
//        rlGop = findViewById(R.id.rl_gop);
//
////        码率，不需要，默认的就好
//        etBitrate = (EditText) findViewById(R.id.et_biterate);
////        帧率，fps，不需要
//        etFps = (EditText) findViewById(R.id.et_fps);
////        关键帧间隔，不需要
//        etGop = (EditText) findViewById(R.id.et_gop);
//
//        rgVideoQuality = (RadioGroup) findViewById(R.id.rg_video_quality);
//        rgVideoResolution = (RadioGroup) findViewById(R.id.rg_video_resolution);
//        rgVideoAspectRatio = (RadioGroup) findViewById(R.id.rg_video_aspect_ratio);
//
//        //标清，不需要
//        rbVideoQualitySD = (RadioButton) findViewById(R.id.rb_video_quality_sd);
//        //高清，待定
//        rbVideoQualityHD = (RadioButton) findViewById(R.id.rb_video_quality_hd);
//        //超清
//        rbVideoQualitySSD = (RadioButton) findViewById(R.id.rb_video_quality_super);
//        rbVideoQulityCustom = (RadioButton) findViewById(R.id.rb_video_quality_custom);
//
//        //分辨率，360p，不需要
//        rbVideoResolution360p = (RadioButton) findViewById(R.id.rb_video_resolution_360p);
//        //分辨率，540p
//        rbVideoResolution540p = (RadioButton) findViewById(R.id.rb_video_resolution_540p);
//        //分辨率，720p
//        rbVideoResolution720p = (RadioButton) findViewById(R.id.rb_video_resolution_720p);
//
//        //视频比例，1:1
//        rbVideoAspectRatio11 = (RadioButton) findViewById(R.id.rb_video_aspect_ratio_1_1);
//        //视频比例，3:4
//        rbVideoAspectRatio34 = (RadioButton) findViewById(R.id.rb_video_aspect_ratio_3_4);
//        //视频比例，16:9
//        rbVideoAspectRatio916 = (RadioButton) findViewById(R.id.rb_video_aspect_ratio_9_16);
//
//        tvRecommendResolution = (TextView) findViewById(R.id.tv_recommend_resolution);
//        tvRecommendBitrate = (TextView) findViewById(R.id.tv_recommend_bitrate);
//        tvRecommendFps = (TextView) findViewById(R.id.tv_recommend_fps);
//        tvRecommendGop = (TextView) findViewById(R.id.tv_recommend_gop);
//
//        btnOK = (Button) findViewById(R.id.btn_ok);
//    }
//
//    private void initListener(){
//        llBack.setOnClickListener(this);
//        btnOK.setOnClickListener(this);
//
//
//        rgVideoAspectRatio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
//                if(i == rbVideoAspectRatio11.getId()){
//                    mAspectRatio = TXRecordCommon.VIDEO_ASPECT_RATIO_1_1;
//                }else if(i == rbVideoAspectRatio34.getId()){
//                    mAspectRatio = TXRecordCommon.VIDEO_ASPECT_RATIO_3_4;
//                }else{
//                    mAspectRatio = TXRecordCommon.VIDEO_ASPECT_RATIO_9_16;
//                }
//            }
//        });
//
//        rgVideoQuality.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
//                if(i == rbVideoQualitySD.getId()){
//                    mRecommendQuality = TXRecordCommon.VIDEO_QUALITY_LOW;
//                    showRecommendQualitySet();
//                    recommendQualitySD();
//                    clearCustomBg();
//                }else if(i == rbVideoQualityHD.getId()){
//                    mRecommendQuality = TXRecordCommon.VIDEO_QUALITY_MEDIUM;
//                    showRecommendQualitySet();
//                    recommendQualityHD();
//                    clearCustomBg();
//                }else if(i == rbVideoQualitySSD.getId()){
//                    mRecommendQuality = TXRecordCommon.VIDEO_QUALITY_HIGH;
//                    showRecommendQualitySet();
//                    recommendQualitySSD();
//                    clearCustomBg();
//                }else{
//                    // 自定义
//                    mRecommendQuality = -1;
//                    showCustomQualitySet();
//                }
//            }
//        });
//
//        rgVideoResolution.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
//                if(i == rbVideoResolution360p.getId()){
//                    mRecordResolution = TXRecordCommon.VIDEO_RESOLUTION_360_640;
//                }else if(i == rbVideoResolution540p.getId()){
//                    mRecordResolution = TXRecordCommon.VIDEO_RESOLUTION_540_960;
//                }else{
//                    mRecordResolution = TXRecordCommon.VIDEO_RESOLUTION_720_1280;
//                }
//            }
//        });
//    }
//
//    private void initViewStatus(){
//        rbVideoResolution720p.setChecked(true);
//        rbVideoAspectRatio916.setChecked(true);
//
//        rbVideoQualitySSD.setChecked(true);
//    }
//
//    private void recommendQualitySD(){
//        tvRecommendResolution.setText("360p");
//        tvRecommendBitrate.setText("1200");
//        tvRecommendFps.setText("20");
//        tvRecommendGop.setText("3");
//
//        rbVideoResolution360p.setChecked(true);
//    }
//
//    private void recommendQualityHD(){
//        tvRecommendResolution.setText("540p");
//        tvRecommendBitrate.setText("2400");
//        tvRecommendFps.setText("20");
//        tvRecommendGop.setText("3");
//
//        rbVideoResolution540p.setChecked(true);
//    }
//
//    private void recommendQualitySSD(){
//        tvRecommendResolution.setText("720p");
//        tvRecommendBitrate.setText("3600");
//        tvRecommendFps.setText("20");
//        tvRecommendGop.setText("3");
//
//        rbVideoResolution720p.setChecked(true);
//    }
//
//    private void showCustomQualitySet(){
//        rgVideoResolution.setVisibility(View.VISIBLE);
//        etBitrate.setVisibility(View.VISIBLE);
//        etFps.setVisibility(View.VISIBLE);
//        etGop.setVisibility(View.VISIBLE);
//
//        tvRecommendGop.setVisibility(View.GONE);
//        tvRecommendResolution.setVisibility(View.GONE);
//        tvRecommendBitrate.setVisibility(View.GONE);
//        tvRecommendFps.setVisibility(View.GONE);
//    }
//
//    private void showRecommendQualitySet(){
//        rgVideoResolution.setVisibility(View.GONE);
//        etBitrate.setVisibility(View.GONE);
//        etFps.setVisibility(View.GONE);
//        etGop.setVisibility(View.GONE);
//
//        tvRecommendGop.setVisibility(View.VISIBLE);
//        tvRecommendResolution.setVisibility(View.VISIBLE);
//        tvRecommendBitrate.setVisibility(View.VISIBLE);
//        tvRecommendFps.setVisibility(View.VISIBLE);
//    }
//
//    @Override
//    public void onClick(View view) {
//        switch (view.getId()){
//            case R.id.back_ll:
//                finish();
//                break;
//            case R.id.btn_ok:
//                getConfigData();
//                startVideoRecordActivity();
//                break;
//        }
//    }
//
//    private void clearCustomBg(){
//        rlBiterate.setBackgroundResource(R.drawable.rect_bg_gray);
//        rlFps.setBackgroundResource(R.drawable.rect_bg_gray);
//        rlGop.setBackgroundResource(R.drawable.rect_bg_gray);
//    }
//
//    private void getConfigData(){
//        // 使用提供的三挡质量设置，不需要传以下参数，sdk内部已定义
//        if(mRecommendQuality != -1){
//            return;
//        }
//
//        String fps = etFps.getText().toString();
//        String gop = etGop.getText().toString();
//        String bitrate = etBitrate.getText().toString();
//
//        if( !TextUtils.isEmpty(bitrate) ){
//            try {
//                mBiteRate = Integer.parseInt(bitrate);
//                if(mBiteRate < 600){
//                    mBiteRate = 600;
//                }else if(mBiteRate > 4800){
//                    mBiteRate = 2400;
//                }
//            } catch (NumberFormatException e) {
//                TXCLog.e(TAG, "NumberFormatException");
//            }
//        }else{
//            mBiteRate = 2400;
//        }
//
//        if( !TextUtils.isEmpty(fps) ){
//            try {
//                mFps = Integer.parseInt(fps);
//                if(mFps < 15){
//                    mFps = 15;
//                }else if(mFps > 30){
//                    mFps = 20;
//                }
//            } catch (NumberFormatException e) {
//                TXCLog.e(TAG, "NumberFormatException");
//            }
//        }else{
//            mFps = 20;
//        }
//
//        if( !TextUtils.isEmpty(gop) ){
//            try {
//                mGop = Integer.parseInt(gop);
//                if(mGop < 1){
//                    mGop = 1;
//                }else if(mGop > 10){
//                    mGop = 3;
//                }
//            } catch (NumberFormatException e) {
//                TXCLog.e(TAG, "NumberFormatException");
//            }
//        }else{
//            mGop = 3;
//        }
//    }
//
//    private void startVideoRecordActivity(){
//        // 传过去三个参数，
//        // 最短时间，3 * 1000ms
//        // 最长时间，10 * 1000ms
//        // 视频比例，public static final int VIDEO_ASPECT_RATIO_9_16 = 0;默认是9_16
//        //           public static final int VIDEO_ASPECT_RATIO_3_4 = 1;
//        //           public static final int VIDEO_ASPECT_RATIO_1_1 = 2;
//        // 默认设置，清晰度，帧率，码率，关键帧间隔，public static final int VIDEO_QUALITY_MEDIUM = 1;超清默认的
//        //                                           public static final int VIDEO_QUALITY_HIGH = 2;高清待定
//        //                                           public static final int RECORD_RESULT_OK = 0;标清用不上
//
//
//        Intent intent = new Intent(this, TCVideoRecordActivity.class);
//        intent.putExtra(RECORD_CONFIG_MIN_DURATION, 3 * 1000);
//        intent.putExtra(RECORD_CONFIG_MAX_DURATION, 10 * 1000);
//        intent.putExtra(RECORD_CONFIG_ASPECT_RATIO, mAspectRatio);
//        if(mRecommendQuality != -1){
//            // 提供的三挡设置
//            intent.putExtra(RECORD_CONFIG_RECOMMEND_QUALITY, mRecommendQuality);
//        }else{
//            // 自定义设置
//            intent.putExtra(RECORD_CONFIG_RESOLUTION, mRecordResolution);
//            intent.putExtra(RECORD_CONFIG_BITE_RATE, mBiteRate);
//            intent.putExtra(RECORD_CONFIG_FPS, mFps);
//            intent.putExtra(RECORD_CONFIG_GOP, mGop);
//        }
//        startActivity(intent);
//    }
//
//}
