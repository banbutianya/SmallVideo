package com.example.a10953.smallvideo;

import android.app.Application;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;

/**
 * Created by 10953 on 2017/11/21.
 */

public class MainApplication extends Application{
    //OSS的Bucket
    public static final String OSS_BUCKET = "black-card";
    //设置OSS数据中心域名或者cname域名
    public static final String OSS_BUCKET_HOST_ID = "oss-cn-beijing.aliyuncs.com";
    //Key
    private static final String accessKey = "****";
    private static final String screctKey = "*****";

    public static OSS oss;
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化OSS配置
        initOSSConfig();
    }

    private void initOSSConfig(){
        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKey, screctKey);

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        if(BuildConfig.DEBUG){
            OSSLog.enableLog();
        }
        oss = new OSSClient(getApplicationContext(), MainApplication.OSS_BUCKET_HOST_ID, credentialProvider, conf);
    }

}
