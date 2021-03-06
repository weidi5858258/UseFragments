package com.weidi.usefragments;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.weidi.application.WeidiApplication;
import com.weidi.dbutil.DbUtils;
import com.weidi.dbutil.SimpleDao2;
import com.weidi.usefragments.business.medical_record.MedicalRecordBean;
import com.weidi.usefragments.business.video_player.exo.C;
import com.weidi.usefragments.business.video_player.exo.Util;
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.sql.DataSource;

import androidx.constraintlayout.solver.Cache;

/***
 Created by root on 18-12-13.
 */

public class MyApplication extends WeidiApplication {

    private static final String TAG =
            MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        userAgent = Util.getUserAgent(this, "ExoPlayerDemo");

        // 启动保活服务
        //KeepLive.startWork(this);

        /*//定义前台服务的默认样式。即标题、描述和图标
        ForegroundNotification foregroundNotification = new ForegroundNotification(
                "测试", "描述", R.mipmap.ic_launcher,
                //定义前台服务的通知点击事件
                new ForegroundNotificationClickListener() {
                    @Override
                    public void foregroundNotificationClick(Context context, Intent intent) {
                    }
                });
        //启动保活服务
        KeepLive.startWork(this, KeepLive.RunMode.ENERGY, foregroundNotification,
                //你需要保活的服务，如socket连接、定时任务等，建议不用匿名内部类的方式在这里写
                new KeepLiveService() {
                    @Override
                    public void onWorking() {
                        Intent localService =
                                new Intent(getApplicationContext(), MusicService.class);
                        startService(localService);
                    }

                    @Override
                    public void onStop() {

                    }
                }
        );*/

        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                MLog.d(TAG, "onCreate() start");
                Context context = getApplicationContext();
                Class[] beanClass = new Class[]{MedicalRecordBean.class};
                DbUtils.getInstance().createOrUpdateDBWithVersion(context, beanClass);

                // 数据库
                SimpleDao2.setContext(MyApplication.this);
                // 先调用一下,把对象给创建好
                SimpleDao2.getInstance();
                MLog.d(TAG, "onCreate() end");
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        /*String processName = this.getProcessName();
        // 判断进程名，保证只有主进程运行
        if (!TextUtils.isEmpty(processName) && processName.equals(this.getPackageName())) {
        }*/
    }

    public static String getProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";

    protected String userAgent;

    //private DatabaseProvider databaseProvider;
    private File downloadDirectory;
    private Cache downloadCache;

    /*public DataSource.Factory buildDataSourceFactory() {
        DefaultDataSourceFactory upstreamFactory =
                new DefaultDataSourceFactory(this, buildHttpDataSourceFactory());
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(userAgent);
    }

    public boolean useExtensionRenderers() {
        //return "withExtensions".equals(BuildConfig.FLAVOR);
        return true;
    }

    public RenderersFactory buildRenderersFactory(boolean preferExtensionRenderer) {
        @DefaultRenderersFactory.ExtensionRendererMode
        int extensionRendererMode =
                useExtensionRenderers()
                        ? (preferExtensionRenderer
                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new DefaultRenderersFactory(*//* context= *//* this)
                .setExtensionRendererMode(extensionRendererMode);
    }

    public MediaSource createLeafMediaSource(
            Uri uri, String extension, DrmSessionManager<?> drmSessionManager) {
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        @ContentType int type = Util.inferContentType(uri, extension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSource.Factory(),
                null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
    }

    private synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(),
                    DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(),
                            getDatabaseProvider());
        }
        return downloadCache;
    }

    private DatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider(this);
        }
        return databaseProvider;
    }

    private File getDownloadDirectory() {
        if (downloadDirectory == null) {
            downloadDirectory = getExternalFilesDir(null);
            if (downloadDirectory == null) {
                downloadDirectory = getFilesDir();
            }
        }
        return downloadDirectory;
    }*/

}
