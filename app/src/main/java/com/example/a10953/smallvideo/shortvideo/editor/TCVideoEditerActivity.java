package com.example.a10953.smallvideo.shortvideo.editor;

import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a10953.smallvideo.R;
import com.tencent.liteav.basic.log.TXCLog;
import com.example.a10953.smallvideo.common.activity.videoprevideo.TCVideoPreviewActivity;
import com.example.a10953.smallvideo.common.utils.TCConstants;
import com.example.a10953.smallvideo.common.utils.TCUtils;
import com.example.a10953.smallvideo.common.widget.VideoWorkProgressFragment;
import com.example.a10953.smallvideo.shortvideo.choose.TCVideoFileInfo;
import com.example.a10953.smallvideo.shortvideo.editor.bgm.TCBGMInfo;
import com.example.a10953.smallvideo.shortvideo.editor.word.TCWordEditorFragment;
import com.tencent.ugc.TXRecordCommon;
import com.tencent.ugc.TXVideoEditConstants;
import com.tencent.ugc.TXVideoEditer;
import com.tencent.ugc.TXVideoInfoReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * UGC短视频裁剪
 */
public class TCVideoEditerActivity extends FragmentActivity implements View.OnClickListener,
        TXVideoEditer.TXVideoGenerateListener, TXVideoInfoReader.OnSampleProgrocess, TXVideoEditer.TXVideoPreviewListener, TCWordEditorFragment.OnWordEditorListener,
        Edit.OnBGMChangeListener, Edit.OnCutChangeListener, Edit.OnFilterChangeListener, Edit.OnSpeedChangeListener, Edit.OnWordChangeListener {

    private static final String TAG = TCVideoEditerActivity.class.getSimpleName();
    public static final int VIDEO_SOURCE_RECORD = 1;
    private final int MSG_LOAD_VIDEO_INFO = 1000;
    private final int MSG_RET_VIDEO_INFO = 1001;

    private int mCurrentState = PlayState.STATE_NONE;

    private TextView mTvDone;
    private TextView mTvCurrent;
    private TextView mTvDuration;
    private ImageButton mBtnPlay;
    private FrameLayout mVideoView;
    private LinearLayout mLayoutEditer;
    private EditPannel mEditPannel;
    private ProgressBar mLoadProgress;
    private VideoWorkProgressFragment mWorkProgressDialog;
    private TCWordEditorFragment mTCWordEditorFragment;     // 添加字幕的Fragment
    /**************************SDK*****************************/
    private TXVideoEditer mTXVideoEditer;
    private TCVideoFileInfo mTCVideoFileInfo;
    private TXVideoInfoReader mTXVideoInfoReader;
    private int mVideoSource;

    private TXVideoEditConstants.TXVideoInfo mTXVideoInfo;
    private TXVideoEditConstants.TXGenerateResult mResult;
    private String mBGMPath;                                // BGM路径
    private String mVideoOutputPath;
    private float mSpeedLevel = 1.0f;                       // 加速速度
    private BackGroundHandler mHandler;
    private long mCutVideoDuration;//裁剪的视频时长

    private Bitmap mWaterMarkLogo;
    private boolean mIsStopManually;//标记是否手动停止

    private HandlerThread mBGHandlerThread;
    private boolean mConverting = false;

    private PhoneStateListener mPhoneListener = null;
    private Dialog mDialog;
    private int mVideoResolution;

    class BackGroundHandler extends Handler {

        public BackGroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_VIDEO_INFO:
                    if (mTXVideoInfoReader == null) {
                        mTXVideoInfoReader = TXVideoInfoReader.getInstance();
                    }
                    TXVideoEditConstants.TXVideoInfo videoInfo = mTXVideoInfoReader.getVideoFileInfo(mTCVideoFileInfo.getFilePath());
                    if (videoInfo == null) {
                        mLoadProgress.setVisibility(View.GONE);

                        showUnSupportDialog("暂不支持Android 4.3以下的系统");
                        return;
                    }
                    Message mainMsg = new Message();
                    mainMsg.what = MSG_RET_VIDEO_INFO;
                    mainMsg.obj = videoInfo;
                    mMainHandler.sendMessage(mainMsg);
                    break;
            }
        }
    }

    private int mRet = -1;
    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RET_VIDEO_INFO:
                    mTXVideoInfo = (TXVideoEditConstants.TXVideoInfo) msg.obj;

                    TXVideoEditConstants.TXPreviewParam param = new TXVideoEditConstants.TXPreviewParam();
                    param.videoView = mVideoView;
                    param.renderMode = TXVideoEditConstants.PREVIEW_RENDER_MODE_FILL_EDGE;
                    mRet = mTXVideoEditer.setVideoPath(mTCVideoFileInfo.getFilePath());
                    mTXVideoEditer.initWithPreview(param);
                    if (mRet == TXVideoEditConstants.ERR_UNSUPPORT_VIDEO_FORMAT || mRet == TXVideoEditConstants.ERR_UNFOUND_FILEINFO) {
                        showUnSupportDialog("本机型暂不支持此视频格式");
                        return;
                    } else if (mRet == TXVideoEditConstants.ERR_UNSUPPORT_LARGE_RESOLUTION) {
                        showConvertDialog("视频分辨率太高，需要进行转码，否则可能造成卡顿或音画不同步");
                        return;
                    }

                    handleOp(Action.DO_SEEK_VIDEO, 0, (int) mTXVideoInfo.duration);
                    mLoadProgress.setVisibility(View.GONE);
                    mEditPannel.setOnClickable(true);
                    mTvDone.setClickable(true);
                    mBtnPlay.setClickable(true);
                    mBtnPlay.setImageResource(R.drawable.ic_pause);
                    mEditPannel.setMediaFileInfo(mTXVideoInfo);
                    String duration = TCUtils.duration(mTXVideoInfo.duration);
                    String position = TCUtils.duration(0);

                    mTvCurrent.setText(position);
                    mTvDuration.setText(duration);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_video_editer);

        initViews();
        initData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initData();
    }

    @Override
    protected void onDestroy() {
        TelephonyManager tm = (TelephonyManager) this.getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);

        mBGHandlerThread.quit();
        handleOp(Action.DO_CANCEL_VIDEO, 0, 0);

        mTXVideoInfoReader.cancel();
        mTXVideoEditer.setTXVideoPreviewListener(null);
        mTXVideoEditer.setVideoGenerateListener(null);
        super.onDestroy();
    }

    private void initViews() {
        mEditPannel = (EditPannel) findViewById(R.id.edit_pannel);
        mEditPannel.setCutChangeListener(this);
        mEditPannel.setFilterChangeListener(this);
        mEditPannel.setBGMChangeListener(this);
        mEditPannel.setWordChangeListener(this);
        mEditPannel.setSpeedChangeListener(this);
        mEditPannel.setOnClickable(false);

        mTvCurrent = (TextView) findViewById(R.id.tv_current);
        mTvDuration = (TextView) findViewById(R.id.tv_duration);

        mVideoView = (FrameLayout) findViewById(R.id.video_view);

        mBtnPlay = (ImageButton) findViewById(R.id.btn_play);
        mBtnPlay.setOnClickListener(this);
        mBtnPlay.setClickable(false);

        LinearLayout backLL = (LinearLayout) findViewById(R.id.back_ll);
        backLL.setOnClickListener(this);

        mTvDone = (TextView) findViewById(R.id.btn_done);
        mTvDone.setOnClickListener(this);
        mTvDone.setClickable(false);

        mLayoutEditer = (LinearLayout) findViewById(R.id.layout_editer);
        mLayoutEditer.setEnabled(true);

        mLoadProgress = (ProgressBar) findViewById(R.id.progress_load);
        initWorkProgressPopWin();
    }

    private void initWorkProgressPopWin() {
        if (mWorkProgressDialog == null) {
            mWorkProgressDialog = new VideoWorkProgressFragment();
            mWorkProgressDialog.setOnClickStopListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTvDone.setClickable(true);
                    mTvDone.setEnabled(true);
                    mWorkProgressDialog.dismiss();
                    Toast.makeText(TCVideoEditerActivity.this, "取消视频生成", Toast.LENGTH_SHORT).show();
                    mWorkProgressDialog.setProgress(0);
                    mCurrentState = PlayState.STATE_NONE;
                    if (mTXVideoEditer != null) {
                        mTXVideoEditer.cancel();
                    }
                }
            });
        }
        mWorkProgressDialog.setProgress(0);
    }

    private synchronized boolean handleOp(int state, long startPlayTime, long endPlayTime) {
        switch (state) {
            case Action.DO_PLAY_VIDEO:
                if (mCurrentState == PlayState.STATE_NONE) {
                    mTXVideoEditer.startPlayFromTime(startPlayTime, endPlayTime);
                    mCurrentState = PlayState.STATE_PLAY;
                    return true;
                } else if (mCurrentState == PlayState.STATE_PAUSE) {
                    mTXVideoEditer.resumePlay();
                    mCurrentState = PlayState.STATE_PLAY;
                    return true;
                }
                break;
            case Action.DO_PAUSE_VIDEO:
                if (mCurrentState == PlayState.STATE_PLAY) {
                    mTXVideoEditer.pausePlay();
                    mCurrentState = PlayState.STATE_PAUSE;
                    return true;
                }
                break;
            case Action.DO_SEEK_VIDEO:
                if (mCurrentState == PlayState.STATE_CUT) {
                    return false;
                }
                if (mCurrentState == PlayState.STATE_PLAY || mCurrentState == PlayState.STATE_PAUSE) {
                    mTXVideoEditer.stopPlay();
                }
                mTXVideoEditer.startPlayFromTime(startPlayTime, endPlayTime);
                mCurrentState = PlayState.STATE_PLAY;
                return true;
            case Action.DO_CUT_VIDEO:
                if (mCurrentState == PlayState.STATE_PLAY || mCurrentState == PlayState.STATE_PAUSE) {
                    mTXVideoEditer.stopPlay();
                }
                startTranscode(true);
                mCurrentState = PlayState.STATE_CUT;
                return true;
            case Action.DO_CANCEL_VIDEO:
                if (mCurrentState == PlayState.STATE_PLAY || mCurrentState == PlayState.STATE_PAUSE) {
                    mTXVideoEditer.stopPlay();
                } else if (mCurrentState == PlayState.STATE_CUT) {
                    mTXVideoEditer.cancel();
                }
                mCurrentState = PlayState.STATE_NONE;
                return true;
        }
        return false;
    }

    private void initData() {
        //初始化后台Thread线程
        if (mBGHandlerThread == null) {
        mBGHandlerThread = new HandlerThread("LoadData");
        mBGHandlerThread.start();
        mHandler = new BackGroundHandler(mBGHandlerThread.getLooper());
        }

        mVideoResolution = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_RESOLUTION, -1);
        mTCVideoFileInfo = (TCVideoFileInfo) getIntent().getSerializableExtra(TCConstants.INTENT_KEY_SINGLE_CHOOSE);
        mVideoSource = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_EDIT);
        if(mVideoSource == TCConstants.VIDEO_RECORD_TYPE_UGC_RECORD){
            // 从录制界面传到预览再到编辑时，传的是path不是TCVideoFileInfo，在发布流服务器编译时PlayDemo中没有导入shortVideo包下的内容，所以不能传TCVideoFileInfo
            String videoPath = getIntent().getStringExtra(TCConstants.VIDEO_RECORD_VIDEPATH);
            mTCVideoFileInfo = new TCVideoFileInfo();
            mTCVideoFileInfo.setFilePath(videoPath);
        }
        //初始化SDK编辑
        if (mTXVideoEditer == null) {
        mTXVideoEditer = new TXVideoEditer(this);
        mTXVideoEditer.setTXVideoPreviewListener(this);
        }

        //加载视频基本信息
        mHandler.sendEmptyMessage(MSG_LOAD_VIDEO_INFO);

        if (mTXVideoInfoReader == null) {
            mTXVideoInfoReader = TXVideoInfoReader.getInstance();
        }
        //加载缩略图
        mTXVideoInfoReader.getSampleImages(TCConstants.THUMB_COUNT, mTCVideoFileInfo.getFilePath(), this);

        //导入水印
        mWaterMarkLogo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        //设置电话监听
        if (mPhoneListener == null) {
            mPhoneListener = new TXPhoneStateListener(this);
            TelephonyManager tm = (TelephonyManager) this.getApplicationContext().getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    }

    private void createThumbFile() {
        AsyncTask<Void, String, String> task = new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                File outputVideo = new File(mVideoOutputPath);
                if (outputVideo == null || !outputVideo.exists())
                    return null;
                Bitmap bitmap = mTXVideoInfoReader.getSampleImage(0, mVideoOutputPath);
                if (bitmap == null)
                    return null;
                String mediaFileName = outputVideo.getAbsolutePath();
                if (mediaFileName.lastIndexOf(".") != -1) {
                    mediaFileName = mediaFileName.substring(0, mediaFileName.lastIndexOf("."));
                }
                String folder = Environment.getExternalStorageDirectory() + File.separator + TCConstants.DEFAULT_MEDIA_PACK_FOLDER + File.separator + mediaFileName;
                File appDir = new File(folder);
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }

                String fileName = "thumbnail" + ".jpg";
                File file = new File(appDir, fileName);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                    mTCVideoFileInfo.setThumbPath(file.getAbsolutePath());
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if(mVideoSource == VIDEO_SOURCE_RECORD){
                    deleteFilePath(mTCVideoFileInfo.getFilePath());
                }
                startPreviewActivity(mResult);
            }
        };
        task.execute();
    }

    private void deleteFilePath(final String path){
        Thread thread = new Thread(){
            public void run(){
                File file = new File(path);
                if(file.exists()) {
                    file.delete();
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (mTCWordEditorFragment == null || mTCWordEditorFragment.isHidden()) {
        if (mCurrentState == PlayState.STATE_PAUSE && !mIsStopManually) {
            handleOp(Action.DO_PLAY_VIDEO, mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
            mBtnPlay.setImageResource(mCurrentState == PlayState.STATE_PLAY ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }
        }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        if (mCurrentState == PlayState.STATE_CUT) {
            handleOp(Action.DO_CANCEL_VIDEO, 0, 0);
            if (mWorkProgressDialog != null && mWorkProgressDialog.isAdded()) {
                mWorkProgressDialog.dismiss();
            }
        } else {
            mIsStopManually = false;
            handleOp(Action.DO_PAUSE_VIDEO, 0, 0);
            mBtnPlay.setImageResource(mCurrentState == PlayState.STATE_PLAY ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        mTvDone.setClickable(true);
        mTvDone.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_done:
                doTranscode();
                break;
            case R.id.back_ll:
                mTXVideoInfoReader.cancel();
                handleOp(Action.DO_CANCEL_VIDEO, 0, 0);
                mTXVideoEditer.setTXVideoPreviewListener(null);
                mTXVideoEditer.setVideoGenerateListener(null);
                finish();
                break;
            case R.id.btn_play:
                mIsStopManually = !mIsStopManually;
                playVideo();
                break;
        }
    }

    private void playVideo() {
        if (mCurrentState == PlayState.STATE_PLAY) {
            handleOp(Action.DO_PAUSE_VIDEO, 0, 0);
        } else {
            handleOp(Action.DO_PLAY_VIDEO, mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
        }
        mBtnPlay.setImageResource(mCurrentState == PlayState.STATE_PLAY ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    /**
     * 开始裁剪：
     * 经过相应的合法状态判定后，将会执行{@link TCVideoEditerActivity#startTranscode(boolean)}
     * 开始裁剪
     */
    private void doTranscode() {
        mTvDone.setEnabled(false);
        mTvDone.setClickable(false);

        mTXVideoInfoReader.cancel();
        mLayoutEditer.setEnabled(false);
        handleOp(Action.DO_CUT_VIDEO, 0, 0);
    }

    /**
     * 开始裁剪的具体执行方法
     */
    private void startTranscode(boolean canCancel) {
        mBtnPlay.setImageResource(R.drawable.ic_play);
        mCutVideoDuration = mEditPannel.getSegmentTo() - mEditPannel.getSegmentFrom();
        mWorkProgressDialog.setProgress(0);
        mWorkProgressDialog.setCancelable(false);
        mWorkProgressDialog.show(getFragmentManager(), "progress_dialog");

        Bitmap tailWaterMarkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tcloud_logo);

//        TXCLog.i(TAG, "mTXVideoInfo width = " + mTXVideoInfo.width + ", height = " + mTXVideoInfo.height);

        TXVideoEditConstants.TXRect txRect = new TXVideoEditConstants.TXRect();
        txRect.x = (mTXVideoInfo.width - tailWaterMarkBitmap.getWidth()) / (2f * mTXVideoInfo.width);
        txRect.y = (mTXVideoInfo.height - tailWaterMarkBitmap.getHeight()) / (2f * mTXVideoInfo.height);
        txRect.width = tailWaterMarkBitmap.getWidth() / (float) mTXVideoInfo.width;

        if (canCancel)  // 转码的不需要加片尾水印
            mTXVideoEditer.setTailWaterMark(tailWaterMarkBitmap, txRect, 3);

        mWorkProgressDialog.setCanCancel(canCancel);

        try {
            mTXVideoEditer.setCutFromTime(mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());

            String outputPath = Environment.getExternalStorageDirectory() + File.separator + TCConstants.DEFAULT_MEDIA_PACK_FOLDER;
            File outputFolder = new File(outputPath);

            if (!outputFolder.exists()) {
                outputFolder.mkdirs();
            }
            String current = String.valueOf(System.currentTimeMillis() / 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String time = sdf.format(new Date(Long.valueOf(current + "000")));
            String saveFileName = String.format("TXVideo_%s.mp4", time);
            mVideoOutputPath = outputFolder + "/" + saveFileName;
            mTXVideoEditer.setVideoGenerateListener(this);

            if(mVideoResolution == -1){
                mTXVideoEditer.generateVideo(TXVideoEditConstants.VIDEO_COMPRESSED_720P, mVideoOutputPath);
            }else if(mVideoResolution == TXRecordCommon.VIDEO_RESOLUTION_360_640){
                mTXVideoEditer.generateVideo(TXVideoEditConstants.VIDEO_COMPRESSED_480P, mVideoOutputPath);
            }else if(mVideoResolution == TXRecordCommon.VIDEO_RESOLUTION_540_960){
                mTXVideoEditer.generateVideo(TXVideoEditConstants.VIDEO_COMPRESSED_540P, mVideoOutputPath);
            }else if(mVideoResolution == TXRecordCommon.VIDEO_RESOLUTION_720_1280){
                mTXVideoEditer.generateVideo(TXVideoEditConstants.VIDEO_COMPRESSED_720P, mVideoOutputPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showConvertDialog(String text) {
        mDialog = new Dialog(TCVideoEditerActivity.this, R.style.ConfirmDialogStyle);
        View v = LayoutInflater.from(TCVideoEditerActivity.this).inflate(R.layout.dialog_ugc_convert, null);
        mDialog.setContentView(v);
        TextView title = (TextView) mDialog.findViewById(R.id.tv_title);
        TextView msg = (TextView) mDialog.findViewById(R.id.tv_msg);
        Button ok = (Button) mDialog.findViewById(R.id.btn_ok);
        Button cancel = (Button) mDialog.findViewById(R.id.btn_cancel);
        title.setText("视频编辑");
        msg.setText(text);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialog.dismiss();
                handleOp(Action.DO_SEEK_VIDEO, 0, (int) mTXVideoInfo.duration);
                mLoadProgress.setVisibility(View.GONE);
                mEditPannel.setOnClickable(true);
                mTvDone.setClickable(true);
                mBtnPlay.setClickable(true);
                mBtnPlay.setImageResource(R.drawable.ic_pause);
                mEditPannel.setMediaFileInfo(mTXVideoInfo);
                String duration = TCUtils.duration(mTXVideoInfo.duration);
                String position = TCUtils.duration(0);

                mTvCurrent.setText(position);
                mTvDuration.setText(duration);
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConverting = true;
                mLoadProgress.setVisibility(View.GONE);
                mTvDone.setEnabled(false);
                mTvDone.setClickable(false);

                mTXVideoInfoReader.cancel();
                mLayoutEditer.setEnabled(false);
                mDialog.dismiss();

                startTranscode(false);
            }
        });
        mDialog.show();
        mDialog.setCancelable(false);
    }

    /**
     * 错误框方法
     */
    private void showUnSupportDialog(String text) {
        final Dialog dialog = new Dialog(TCVideoEditerActivity.this, R.style.ConfirmDialogStyle);
        View v = LayoutInflater.from(TCVideoEditerActivity.this).inflate(R.layout.dialog_ugc_tip, null);
        dialog.setContentView(v);
        TextView title = (TextView) dialog.findViewById(R.id.tv_title);
        TextView msg = (TextView) dialog.findViewById(R.id.tv_msg);
        Button ok = (Button) dialog.findViewById(R.id.btn_ok);
        title.setText("视频编辑失败");
        msg.setText(text);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
    }
        });
        dialog.show();
    }

    /********************************************* SDK回调**************************************************/
    @Override
    public void onGenerateProgress(final float progress) {
        final int prog = (int) (progress * 100);
        mWorkProgressDialog.setProgress(prog);
    }

    @Override
    public void onGenerateComplete(TXVideoEditConstants.TXGenerateResult result) {
        if (result.retCode == TXVideoEditConstants.GENERATE_RESULT_OK) {
            if (mConverting) {
                Intent intent = new Intent(this, TCVideoEditerActivity.class);
                File file = new File(mVideoOutputPath);
                if (!file.exists()) {
                    showUnSupportDialog("视频文件不存在");
                    return;
                }
                mTCVideoFileInfo.setFilePath(mVideoOutputPath);
                intent.putExtra(TCConstants.INTENT_KEY_SINGLE_CHOOSE, mTCVideoFileInfo);
                startActivity(intent);
                mConverting = false;

                if (mWorkProgressDialog != null && mWorkProgressDialog.isAdded()) {
                    mWorkProgressDialog.dismiss();
                }
            } else {
            if (mTXVideoInfo != null) {
                mResult = result;
                createThumbFile();
            }
            finish();
            }
        } else {
            final TXVideoEditConstants.TXGenerateResult ret = result;
            Toast.makeText(TCVideoEditerActivity.this, ret.descMsg, Toast.LENGTH_SHORT).show();
            mTvDone.setEnabled(true);
            mTvDone.setClickable(true);
        }
        mCurrentState = PlayState.STATE_NONE;
    }

    private void startPreviewActivity(TXVideoEditConstants.TXGenerateResult result) {
        Intent intent = new Intent(getApplicationContext(), TCVideoPreviewActivity.class);
        intent.putExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_EDIT);
        intent.putExtra(TCConstants.VIDEO_RECORD_RESULT, result.retCode);
        intent.putExtra(TCConstants.VIDEO_RECORD_DESCMSG, result.descMsg);
        intent.putExtra(TCConstants.VIDEO_RECORD_VIDEPATH, mVideoOutputPath);
        intent.putExtra(TCConstants.VIDEO_RECORD_COVERPATH, mTCVideoFileInfo.getThumbPath());
        intent.putExtra(TCConstants.VIDEO_RECORD_DURATION, mCutVideoDuration);
        startActivity(intent);
        finish();
    }

    @Override
    public void sampleProcess(int number, Bitmap bitmap) {
        if (number == 0) {
            mEditPannel.clearAllBitmap();
        }
        int num = number;
        Bitmap bmp = bitmap;
        mEditPannel.addBitmap(num, bmp);
        TXCLog.d(TAG, "number = " + number + ",bmp = " + bitmap);
    }


    @Override
    public void onPreviewProgress(final int time) {
        if (mTvCurrent != null) {
            mTvCurrent.setText(TCUtils.duration((long) (time / 1000 * mSpeedLevel)));
        }
    }

    @Override
    public void onPreviewFinished() {
        TXCLog.d(TAG, "---------------onPreviewFinished-----------------");
        handleOp(Action.DO_SEEK_VIDEO, mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
    }

    /********************************************* 裁剪**************************************************/
    @Override
    public void onCutChangeKeyDown() {
        mBtnPlay.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onCutChangeKeyUp(int startTime, int endTime) {
        mBtnPlay.setImageResource(R.drawable.ic_pause);
        handleOp(Action.DO_SEEK_VIDEO, mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
    }

    /********************************************* 加速**************************************************/
    @Override //开启加速的回调
    public void onSpeedChange(float speed) {
        //开启变速时候，建议停止播放。 然后设置变速，再次重新播放，能够有效避免界面卡顿，引起体验不好的问题。
        mTXVideoEditer.stopPlay();
        mCurrentState = PlayState.STATE_CANCEL;

        mSpeedLevel = speed;
        mTXVideoEditer.setSpeedLevel(mSpeedLevel);

        mTXVideoEditer.startPlayFromTime(mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
        mCurrentState = PlayState.STATE_PLAY;
    }

    /********************************************* 滤镜**************************************************/
    @Override //选择了具体滤镜的回调
    public void onFilterChange(Bitmap bitmap) {
        mTXVideoEditer.setFilter(bitmap);
    }

    /********************************************* 背景音**************************************************/
    @Override //BGM的音量的改变回调
    public void onBGMSeekChange(float progress) {
        mTXVideoEditer.setBGMVolume(mEditPannel.getBGMVolumeProgress());
        mTXVideoEditer.setVideoVolume(1 - mEditPannel.getBGMVolumeProgress());
    }

    @Override //移除BGM回调
    public void onBGMDelete() {
        mTXVideoEditer.setBGM(null);
    }

    @Override //选中BGM的回调
    public boolean onBGMInfoSetting(TCBGMInfo info) {
        mTXVideoEditer.setBGMVolume(mEditPannel.getBGMVolumeProgress());
        mTXVideoEditer.setVideoVolume(1 - mEditPannel.getBGMVolumeProgress());
        mBGMPath = info.getPath();
        if (!TextUtils.isEmpty(mBGMPath)) {
            int result = mTXVideoEditer.setBGM(mBGMPath);
            if (result != 0) {
                showUnSupportDialog("背景音仅支持MP3格式或M4A音频");
            }
            return result == 0;//设置成功
        }
        return false;
    }

    @Override //开始滑动BGM区间的回调
    public void onBGMRangeKeyDown() {

    }

    @Override //BGM起止时间的回调
    public void onBGMRangeKeyUp(long startTime, long endTime) {
        if (!TextUtils.isEmpty(mBGMPath)) {
            mTXVideoEditer.setBGMStartTime(startTime, endTime);
        }
    }

    /********************************************* 字幕**************************************************/
    @Override //点击添加字幕的回调
    public void onWordClick() {
        if (mTCWordEditorFragment == null) {
            mTCWordEditorFragment = TCWordEditorFragment.newInstance(mTXVideoEditer,
                    mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
            mTCWordEditorFragment.setOnWordEditorListener(TCVideoEditerActivity.this);
            mTCWordEditorFragment.setSpeedLevel(mSpeedLevel);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.editer_fl_word_container, mTCWordEditorFragment, "editor_word_fragment")
                    .commit();
        } else {
            mTCWordEditorFragment.setVideoRangeTime(mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
            mTCWordEditorFragment.setSpeedLevel(mSpeedLevel);
            getSupportFragmentManager()
                    .beginTransaction()
                    .show(mTCWordEditorFragment)
                    .commit();
        }
    }


    /********************************************* 字幕Fragment回调**************************************************/

    @Override //从字幕的Fragment取消回来的回调
    public void onWordEditCancel() {
        removeWordEditorFragment();
        resetAndPlay();
    }

    @Override //从字幕的Fragment点击保存回来的hi掉
    public void onWordEditFinish() {
        removeWordEditorFragment();
        resetAndPlay();
    }

    private void removeWordEditorFragment() {
        if (mTCWordEditorFragment != null && mTCWordEditorFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().hide(mTCWordEditorFragment).commit();
        }
    }

    /**
     * 从字幕编辑回来之后，要重新设置Video的容器，以及监听进度回调
     */
    private void resetAndPlay() {
        mBtnPlay.setImageResource(R.drawable.ic_pause);
        mCurrentState = PlayState.STATE_PLAY;
        TXVideoEditConstants.TXPreviewParam param = new TXVideoEditConstants.TXPreviewParam();
        param.videoView = mVideoView;
        param.renderMode = TXVideoEditConstants.PREVIEW_RENDER_MODE_FILL_EDGE;
        mTXVideoEditer.initWithPreview(param);
        mTXVideoEditer.startPlayFromTime(mEditPannel.getSegmentFrom(), mEditPannel.getSegmentTo());
        mTXVideoEditer.setTXVideoPreviewListener(this);
    }

    public int getRet() {
        return mRet;
    }

    /*********************************************监听电话状态**************************************************/
    static class TXPhoneStateListener extends PhoneStateListener {
        WeakReference<TCVideoEditerActivity> mJoiner;

        public TXPhoneStateListener(TCVideoEditerActivity joiner) {
            mJoiner = new WeakReference<TCVideoEditerActivity>(joiner);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            TCVideoEditerActivity joiner = mJoiner.get();
            if (joiner == null) return;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:  //电话等待接听
                case TelephonyManager.CALL_STATE_OFFHOOK:  //电话接听
                    if (joiner.mCurrentState == PlayState.STATE_CUT) {
                        joiner.handleOp(Action.DO_CANCEL_VIDEO, 0, 0);
                        if (joiner.mWorkProgressDialog != null && joiner.mWorkProgressDialog.isAdded()) {
                            joiner.mWorkProgressDialog.dismiss();
                        }
                        joiner.mBtnPlay.setImageResource(R.drawable.ic_pause);
                    } else {
                        joiner.handleOp(Action.DO_PAUSE_VIDEO, 0, 0);
                        if (joiner.mBtnPlay != null) {
                            joiner.mBtnPlay.setImageResource(joiner.mCurrentState == PlayState.STATE_PLAY ? R.drawable.ic_pause : R.drawable.ic_play);
                        }
                    }
                    if (joiner.mTvDone != null) {
                        joiner.mTvDone.setClickable(true);
                        joiner.mTvDone.setEnabled(true);
                    }
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    joiner.mBtnPlay.setImageResource(R.drawable.ic_pause);
                    if (joiner.mTXVideoEditer != null && joiner.mEditPannel != null && joiner.getRet() == 0)
                        joiner.handleOp(Action.DO_PLAY_VIDEO, joiner.mEditPannel.getSegmentFrom(), joiner.mEditPannel.getSegmentTo());
                    break;
            }
        }
}
}
