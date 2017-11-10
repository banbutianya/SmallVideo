package com.example.a10953.smallvideo;

import android.app.Application;

import com.tencent.rtmp.TXLiveBase;


/**
 * Created by 10953 on 2017/11/7.
 */

public class DemoApplication extends Application{
    private static DemoApplication instance;

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        TXLiveBase.setConsoleEnabled(true);
//        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
//        strategy.setAppVersion(TXLiveBase.getSDKVersionStr());
//        CrashReport.initCrashReport(getApplicationContext(),strategy);

    }

    public static DemoApplication getApplication() {
        return instance;
    }
}
