package com.example.a10953.smallvideo.videorecord;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a10953.smallvideo.R;
import com.example.a10953.smallvideo.common.activity.videoprevideo.TCVideoPreviewActivity;
import com.example.a10953.smallvideo.common.utils.TCConstants;
import com.example.a10953.smallvideo.shortvideo.view.RecordProgressView;
import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.ugc.TXRecordCommon;
import com.tencent.ugc.TXUGCRecord;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UGC小视频录制界面
 */
public class TCVideoRecordActivity extends Activity implements View.OnClickListener,  TXRecordCommon.ITXVideoRecordListener, View.OnTouchListener, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "TCVideoRecordActivity";
    private static final String OUTPUT_DIR_NAME = "TXUGC";
    private boolean mRecording = false;
    private boolean mStartPreview = false;
    private boolean mFront = true;
    private TXUGCRecord mTXCameraRecord;
    private TXRecordCommon.TXRecordResult mTXRecordResult;
    private long mDuration; // 视频总时长


    private TXCloudVideoView mVideoView;
    private ImageView mIvConfirm;
    private TextView mProgressTime;
    private ProgressDialog mCompleteProgressDialog;
    private ImageView mIvTorch;
    private ComposeRecordBtn mComposeRecordBtn;
    private boolean mAspectSelectShow = false;

    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusListener;
    private boolean mPause = false;
    private int mCurrentAspectRatio;
    private FrameLayout mMaskLayout;
    private RecordProgressView mRecordProgressView;
    private ImageView mIvDeleteLastPart;
    private boolean isSelected = false; // 回删状态
    private long mLastClickTime;
    private boolean mIsTorchOpen = false; // 闪光灯的状态

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor;
    private float mLastScaleFactor;

    private int mRecommendQuality = TXRecordCommon.VIDEO_QUALITY_MEDIUM;
    private int mMinDuration;
    private int mMaxDuration;
    private int mAspectRatio; // 视频比例
    private int mRecordResolution; // 录制分辨率
    private int mBiteRate; // 码率
    private int mFps; // 帧率
    private int mGop; // 关键帧间隔
    private String mBGMPath;
    private String mBGMPlayingPath;
    private int mBGMDuration;
    private int mRecordSpeed = TXRecordCommon.RECORD_SPEED_NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Disable Screen Rotation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_video_record);

        LinearLayout backLL = (LinearLayout) findViewById(R.id.back_ll);
        backLL.setOnClickListener(this);

        initViews();

        getData();
    }

    private void getData() {
        Intent intent = getIntent();
        if (intent == null) {
            TXCLog.e(TAG, "intent is null");
            return;
        }
        mMinDuration = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_MIN_DURATION, 3 * 1000);
        mMaxDuration = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_MAX_DURATION, 10 * 1000);
        mAspectRatio = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_ASPECT_RATIO, TXRecordCommon.VIDEO_ASPECT_RATIO_9_16);
        mRecommendQuality = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_RECOMMEND_QUALITY, -1);

        mCurrentAspectRatio = mAspectRatio;

        mRecordProgressView.setMaxDuration(mMaxDuration);
        mRecordProgressView.setMinDuration(mMinDuration);

        if (mRecommendQuality != -1) {
            // 使用了推荐的视频质量设置，用TXUGCSimpleConfig
            TXCLog.i(TAG, "mRecommendQuality = " + mRecommendQuality);
            return;
        }
        // 自定义视频质量设置，用TXUGCCustomConfig
        mRecordResolution = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_RESOLUTION, TXRecordCommon.VIDEO_RESOLUTION_540_960);
        mBiteRate = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_BITE_RATE, 1800);
        mFps = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_FPS, 20);
        mGop = intent.getIntExtra(TCVideoSettingActivity.RECORD_CONFIG_GOP, 3);

        TXCLog.d(TAG, "mMinDuration = " + mMinDuration + ", mMaxDuration = " + mMaxDuration + ", mAspectRatio = " + mAspectRatio +
                ", mRecommendQuality = " + mRecommendQuality + ", mRecordResolution = " + mRecordResolution + ", mBiteRate = " + mBiteRate + ", mFps = " + mFps + ", mGop = " + mGop);
    }

    private void startCameraPreview() {
        if (mStartPreview) return;
        mStartPreview = true;

        mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
        mTXCameraRecord.setVideoRecordListener(this);
        // 推荐配置
        if (mRecommendQuality >= 0) {
            TXRecordCommon.TXUGCSimpleConfig simpleConfig = new TXRecordCommon.TXUGCSimpleConfig();
            simpleConfig.videoQuality = mRecommendQuality;
            simpleConfig.minDuration = mMinDuration;
            simpleConfig.maxDuration = mMaxDuration;
            simpleConfig.isFront = mFront;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                simpleConfig.mHomeOriention = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
            } else {
                simpleConfig.mHomeOriention = TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT;
            }
            mTXCameraRecord.setRecordSpeed(mRecordSpeed);
            mTXCameraRecord.startCameraSimplePreview(simpleConfig, mVideoView);
            mTXCameraRecord.setAspectRatio(mCurrentAspectRatio);
        } else {
            // 自定义配置
            TXRecordCommon.TXUGCCustomConfig customConfig = new TXRecordCommon.TXUGCCustomConfig();
            customConfig.videoResolution = mRecordResolution;
            customConfig.minDuration = mMinDuration;
            customConfig.maxDuration = mMaxDuration;
            customConfig.videoBitrate = mBiteRate;
            customConfig.videoGop = mGop;
            customConfig.videoFps = mFps;
            customConfig.isFront = mFront;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                customConfig.mHomeOriention = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
            } else {
                customConfig.mHomeOriention = TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT;
            }
            mTXCameraRecord.setRecordSpeed(mRecordSpeed);
            mTXCameraRecord.startCameraCustomPreview(customConfig, mVideoView);
            mTXCameraRecord.setAspectRatio(mCurrentAspectRatio);
        }
    }

    private void initViews() {
        mMaskLayout = (FrameLayout) findViewById(R.id.mask);
        mMaskLayout.setOnTouchListener(this);

        mIvConfirm = (ImageView) findViewById(R.id.btn_confirm);
        mIvConfirm.setOnClickListener(this);
        mIvConfirm.setImageResource(R.drawable.ugc_confirm_disable);
        mIvConfirm.setEnabled(false);

        mVideoView = (TXCloudVideoView) findViewById(R.id.video_view);
        mVideoView.enableHardwareDecode(true);

        mProgressTime = (TextView) findViewById(R.id.progress_time);
        mIvDeleteLastPart = (ImageView) findViewById(R.id.btn_delete_last_part);
        mIvDeleteLastPart.setOnClickListener(this);


        mRecordProgressView = (RecordProgressView) findViewById(R.id.record_progress_view);

        mGestureDetector = new GestureDetector(this, this);
        mScaleGestureDetector = new ScaleGestureDetector(this, this);

        mCompleteProgressDialog = new ProgressDialog(this);
        mCompleteProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条的形式为圆形转动的进度条
        mCompleteProgressDialog.setCancelable(false);// 设置是否可以通过点击Back键取消
        mCompleteProgressDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条

        mIvTorch = (ImageView) findViewById(R.id.btn_torch);
        mIvTorch.setOnClickListener(this);

        if (mFront) {
            mIvTorch.setImageResource(R.drawable.ugc_torch_disable);
            mIvTorch.setEnabled(false);
        } else {
            mIvTorch.setImageResource(R.drawable.selector_torch_close);
            mIvTorch.setEnabled(true);
        }

        mComposeRecordBtn = (ComposeRecordBtn) findViewById(R.id.compose_record_btn);
        mRecordSpeed = TXRecordCommon.RECORD_SPEED_NORMAL;

    }


    @Override
    protected void onStart() {
        super.onStart();

        if (hasPermission()) {
            startCameraPreview();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopCameraPreview();
            mStartPreview = false;
        }
        if (mRecording && !mPause) {
            pauseRecord();
        }
        if (mTXCameraRecord != null) {
            mTXCameraRecord.pauseBGM();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRecordProgressView != null) {
            mRecordProgressView.release();
        }

        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopBGM();
            mTXCameraRecord.stopCameraPreview();
            mTXCameraRecord.setVideoRecordListener(null);
            mTXCameraRecord.getPartsManager().deleteAllParts();
            mTXCameraRecord.release();
            mTXCameraRecord = null;
            mStartPreview = false;
        }
        abandonAudioFocus();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mTXCameraRecord.stopCameraPreview();
        if (mRecording && !mPause) {
            pauseRecord();
        }

        if (mTXCameraRecord != null) {
            mTXCameraRecord.pauseBGM();
        }

        mStartPreview = false;
        startCameraPreview();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_ll:
                back();
                break;
            case R.id.btn_switch_camera:
                mFront = !mFront;
                mIsTorchOpen = false;
                if (mFront) {
                    mIvTorch.setImageResource(R.drawable.ugc_torch_disable);
                    mIvTorch.setEnabled(false);
                } else {
                    mIvTorch.setImageResource(R.drawable.selector_torch_close);
                    mIvTorch.setEnabled(true);
                }
                if (mTXCameraRecord != null) {
                    TXCLog.i(TAG, "switchCamera = " + mFront);
                    mTXCameraRecord.switchCamera(mFront);
                }
                break;
            case R.id.compose_record_btn:
                if (mAspectSelectShow) {
                    mAspectSelectShow = !mAspectSelectShow;
                }
                switchRecord();
                break;
            case R.id.btn_confirm:
                mCompleteProgressDialog.show();
                stopRecord();
                break;
            case R.id.btn_delete_last_part:
                deleteLastPart();
                break;
            case R.id.btn_torch:
                toggleTorch();
                break;
            default:
                break;
        }
    }


    private void toggleTorch() {
        if (mIsTorchOpen) {
            mTXCameraRecord.toggleTorch(false);
            mIvTorch.setImageResource(R.drawable.selector_torch_close);
        } else {
            mTXCameraRecord.toggleTorch(true);
            mIvTorch.setImageResource(R.drawable.selector_torch_open);
        }
        mIsTorchOpen = !mIsTorchOpen;
    }



    private void deleteLastPart() {
        if (mRecording && !mPause) {
            return;
        }
        if (!isSelected) {
            isSelected = true;
            mRecordProgressView.selectLast();
        } else {
            isSelected = false;
            mRecordProgressView.deleteLast();
            mTXCameraRecord.getPartsManager().deleteLastPart();
            int timeSecond = mTXCameraRecord.getPartsManager().getDuration() / 1000;
            mProgressTime.setText(String.format(Locale.CHINA, "00:%02d", timeSecond));
            if (timeSecond < mMinDuration / 1000) {
                mIvConfirm.setImageResource(R.drawable.ugc_confirm_disable);
                mIvConfirm.setEnabled(false);
            } else {
                mIvConfirm.setImageResource(R.drawable.selector_record_confirm);
                mIvConfirm.setEnabled(true);
            }

        }
    }


    private void switchRecord() {
        long currentClickTime = System.currentTimeMillis();
        if (currentClickTime - mLastClickTime < 200) {
            return;
        }
        if (mRecording) {
            if (mPause) {
                if (mTXCameraRecord.getPartsManager().getPartsPathList().size() == 0) {
                    startRecord();
                } else {
                    resumeRecord();
                }
            } else {
                pauseRecord();
            }
        } else {
            startRecord();
        }
        mLastClickTime = currentClickTime;
    }

    private void resumeRecord() {
        mComposeRecordBtn.startRecord();
        mIvDeleteLastPart.setImageResource(R.drawable.ugc_delete_last_part_disable);
        mIvDeleteLastPart.setEnabled(false);

        mPause = false;
        isSelected = false;
        if (mTXCameraRecord != null) {
            mTXCameraRecord.resumeRecord();
            if (!TextUtils.isEmpty(mBGMPath)) {
                if (mBGMPlayingPath == null || !mBGMPath.equals(mBGMPlayingPath)) {
                    mTXCameraRecord.playBGMFromTime(0, mBGMDuration);
                    mBGMPlayingPath = mBGMPath;
                } else {
                    mTXCameraRecord.resumeBGM();
                }
            }
        }
        requestAudioFocus();

    }

    private void pauseRecord() {
        mComposeRecordBtn.pauseRecord();
        mPause = true;
        mIvDeleteLastPart.setImageResource(R.drawable.selector_delete_last_part);
        mIvDeleteLastPart.setEnabled(true);

        if (mTXCameraRecord != null) {
            if (!TextUtils.isEmpty(mBGMPlayingPath)) {
                mTXCameraRecord.pauseBGM();
            }
            mTXCameraRecord.pauseRecord();
        }
        abandonAudioFocus();

    }

    private void stopRecord() {
        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopBGM();
            mTXCameraRecord.stopRecord();
        }
        mRecording = false;
        mPause = false;
        abandonAudioFocus();

    }

    private void startRecord() {
        mComposeRecordBtn.startRecord();
        mIvDeleteLastPart.setImageResource(R.drawable.ugc_delete_last_part_disable);
        mIvDeleteLastPart.setEnabled(false);
        if (mTXCameraRecord == null) {
            mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
        }

        String customVideoPath = getCustomVideoOutputPath();
        String customCoverPath = customVideoPath.replace(".mp4", ".jpg");

        int result = mTXCameraRecord.startRecord(customVideoPath, customCoverPath);
        if (result != 0) {
            Toast.makeText(TCVideoRecordActivity.this.getApplicationContext(), "录制失败，错误码：" + result, Toast.LENGTH_SHORT).show();
            mTXCameraRecord.setVideoRecordListener(null);
            mTXCameraRecord.stopRecord();
            return;
        }
        if (!TextUtils.isEmpty(mBGMPath)) {
            mBGMDuration = mTXCameraRecord.setBGM(mBGMPath);
            mTXCameraRecord.playBGMFromTime(0, mBGMDuration);
            mBGMPlayingPath = mBGMPath;
            TXCLog.i(TAG, "music duration = " + mTXCameraRecord.getMusicDuration(mBGMPath));
        }

        mRecording = true;
        mPause = false;
    }

    private String getCustomVideoOutputPath() {
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
        String time = sdf.format(new Date(currentTime));
        String outputDir = Environment.getExternalStorageDirectory() + File.separator + OUTPUT_DIR_NAME;
        File outputFolder = new File(outputDir);
        if (!outputFolder.exists()) {
            outputFolder.mkdir();
        }
        String tempOutputPath = outputDir + File.separator + "BLACK_CARD_" + time + ".mp4";
        return tempOutputPath;
    }

    private void startPreview() {
        if (mTXRecordResult != null && (mTXRecordResult.retCode == TXRecordCommon.RECORD_RESULT_OK
                || mTXRecordResult.retCode == TXRecordCommon.RECORD_RESULT_OK_REACHED_MAXDURATION
                || mTXRecordResult.retCode == TXRecordCommon.RECORD_RESULT_OK_LESS_THAN_MINDURATION)) {
            Intent intent = new Intent(getApplicationContext(), TCVideoPreviewActivity.class);
            intent.putExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_UGC_RECORD);
            intent.putExtra(TCConstants.VIDEO_RECORD_RESULT, mTXRecordResult.retCode);
            intent.putExtra(TCConstants.VIDEO_RECORD_DESCMSG, mTXRecordResult.descMsg);
            intent.putExtra(TCConstants.VIDEO_RECORD_VIDEPATH, mTXRecordResult.videoPath);
            intent.putExtra(TCConstants.VIDEO_RECORD_COVERPATH, mTXRecordResult.coverPath);
            intent.putExtra(TCConstants.VIDEO_RECORD_DURATION, mDuration);
            if (mRecommendQuality == TXRecordCommon.VIDEO_QUALITY_LOW) {
                intent.putExtra(TCConstants.VIDEO_RECORD_RESOLUTION, TXRecordCommon.VIDEO_RESOLUTION_360_640);
            } else if (mRecommendQuality == TXRecordCommon.VIDEO_QUALITY_MEDIUM) {
                intent.putExtra(TCConstants.VIDEO_RECORD_RESOLUTION, TXRecordCommon.VIDEO_RESOLUTION_540_960);
            } else if (mRecommendQuality == TXRecordCommon.VIDEO_QUALITY_HIGH) {
                intent.putExtra(TCConstants.VIDEO_RECORD_RESOLUTION, TXRecordCommon.VIDEO_RESOLUTION_720_1280);
            } else {
                intent.putExtra(TCConstants.VIDEO_RECORD_RESOLUTION, mRecordResolution);
            }
            startActivity(intent);
            finish();
        }
    }


    @Override
    public void onRecordEvent(int event, Bundle param) {
        TXCLog.d(TAG, "onRecordEvent event id = " + event);
        if (event == TXRecordCommon.EVT_ID_PAUSE) {
            mRecordProgressView.clipComplete();
        } else if (event == TXRecordCommon.EVT_CAMERA_CANNOT_USE) {
            Toast.makeText(this, "摄像头打开失败，请检查权限", Toast.LENGTH_SHORT).show();
        } else if (event == TXRecordCommon.EVT_MIC_CANNOT_USE) {
            Toast.makeText(this, "麦克风打开失败，请检查权限", Toast.LENGTH_SHORT).show();
        } else if (event == TXRecordCommon.EVT_ID_RESUME) {

        }
    }

    @Override
    public void onRecordProgress(long milliSecond) {
        if (mRecordProgressView == null) {
            return;
        }
        mRecordProgressView.setProgress((int) milliSecond);
        float timeSecondFloat = milliSecond / 1000f;
        int timeSecond = Math.round(timeSecondFloat);
        mProgressTime.setText(String.format(Locale.CHINA, "00:%02d", timeSecond));
        if (timeSecondFloat < mMinDuration / 1000) {
            mIvConfirm.setImageResource(R.drawable.ugc_confirm_disable);
            mIvConfirm.setEnabled(false);
        } else {
            mIvConfirm.setImageResource(R.drawable.selector_record_confirm);
            mIvConfirm.setEnabled(true);
        }
    }


    @Override
    public void onRecordComplete(TXRecordCommon.TXRecordResult result) {
        mCompleteProgressDialog.dismiss();

        mTXRecordResult = result;

        TXCLog.i(TAG, "onRecordComplete, result retCode = " + result.retCode + ", descMsg = " + result.descMsg + ", videoPath + " + result.videoPath + ", coverPath = " + result.coverPath);
        if (mTXRecordResult.retCode < 0) {
            mRecording = false;

            int timeSecond = mTXCameraRecord.getPartsManager().getDuration() / 1000;
            mProgressTime.setText(String.format(Locale.CHINA, "00:%02d", timeSecond));
            Toast.makeText(TCVideoRecordActivity.this.getApplicationContext(), "录制失败，原因：" + mTXRecordResult.descMsg, Toast.LENGTH_SHORT).show();
        } else {

            mDuration = mTXCameraRecord.getPartsManager().getDuration();

            if (mTXCameraRecord != null) {
                mTXCameraRecord.getPartsManager().deleteAllParts();
            }

            startPreview();
        }
    }

    private void requestAudioFocus() {
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        }

        if (null == mOnAudioFocusListener) {
            mOnAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

                @Override
                public void onAudioFocusChange(int focusChange) {
                    try {
                        TXCLog.i(TAG, "requestAudioFocus, onAudioFocusChange focusChange = " + focusChange);

                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            pauseRecord();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            pauseRecord();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                        } else {
                            pauseRecord();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }
        try {
            mAudioManager.requestAudioFocus(mOnAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abandonAudioFocus() {
        try {
            if (null != mAudioManager && null != mOnAudioFocusListener) {
                mAudioManager.abandonAudioFocus(mOnAudioFocusListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                for (int ret : grantResults) {
                    if (ret != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                startCameraPreview();
                break;
            default:
                break;
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(this,
                        permissions.toArray(new String[0]),
                        100);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view == mMaskLayout) {
            if (motionEvent.getPointerCount() >= 2) {
                mScaleGestureDetector.onTouchEvent(motionEvent);
            } else if (motionEvent.getPointerCount() == 1) {
                mGestureDetector.onTouchEvent(motionEvent);
            }
        }
        return true;
    }

    // OnGestureListener回调start
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        int maxZoom = mTXCameraRecord.getMaxZoom();
        if (maxZoom == 0) {
            TXCLog.i(TAG, "camera not support zoom");
            return false;
        }

        float factorOffset = scaleGestureDetector.getScaleFactor() - mLastScaleFactor;

        mScaleFactor += factorOffset;
        mLastScaleFactor = scaleGestureDetector.getScaleFactor();
        if (mScaleFactor < 0) {
            mScaleFactor = 0;
        }
        if (mScaleFactor > 1) {
            mScaleFactor = 1;
        }

        int zoomValue = Math.round(mScaleFactor * maxZoom);
        mTXCameraRecord.setZoom(zoomValue);
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        mLastScaleFactor = scaleGestureDetector.getScaleFactor();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }


    private void back() {
        if (!mRecording) {
            finish();
        }
        if (mPause) {
            if (mTXCameraRecord != null) {
                mTXCameraRecord.getPartsManager().deleteAllParts();
            }
            finish();
        } else {
            pauseRecord();
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }
}
