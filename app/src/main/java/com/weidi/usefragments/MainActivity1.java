package com.weidi.usefragments;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.business.audio_player.JniMusicService;
import com.weidi.usefragments.business.back_stack.ShowTitleDialogFragment;
import com.weidi.usefragments.business.contents.ContentsFragment;
import com.weidi.usefragments.business.media.AudioFragment;
import com.weidi.usefragments.business.media.Camera2Fragment;
import com.weidi.usefragments.business.media.CameraPreviewFragment;
import com.weidi.usefragments.business.media.DecodeAudioFragment;
import com.weidi.usefragments.business.media.DecodePlayFragment;
import com.weidi.usefragments.business.media.DecodeVideoFragment;
import com.weidi.usefragments.business.media.RecordScreenFragment;
import com.weidi.usefragments.business.media.ThrowingScreenFragment;
import com.weidi.usefragments.business.media.VideoLiveBroadcastingFragment;
import com.weidi.usefragments.business.medical_record.MedicalRecordFragment;
import com.weidi.usefragments.business.test_horizontal_card.HorizontalCardFragment;
import com.weidi.usefragments.business.video_player.PlayerService;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.javabean.Person;
import com.weidi.usefragments.business.audio_player.MusicService;
import com.weidi.usefragments.service.DownloadFileService;
import com.weidi.usefragments.test_fragment.scene2.A2Fragment;
import com.weidi.usefragments.test_fragment.scene2.B2Fragment;
import com.weidi.usefragments.test_fragment.scene2.C2Fragment;
import com.weidi.usefragments.test_fragment.scene2.D2Fragment;
import com.weidi.usefragments.test_fragment.scene2.E2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main1Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main3Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main4Fragment;
import com.weidi.usefragments.test_fragment.scene2.TestMotionEventFragment;
import com.weidi.usefragments.test_fragment.scene2.ViewPagerFragment;
import com.weidi.usefragments.tool.JniUtils;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.business.audio_player.SimpleAudioPlayer;
import com.weidi.usefragments.tool.SimpleVideoPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 场景:
 一个MainActivity,预告加载4个Fragment.
 这4个Fragment相互之间是show或者hide,
 一直依附在MainActivity上,当MainActivity退出时
 4个Fragment跟着退出.不然一直不退出.
 从4个Fragment中可以开启其他任意的Fragment.
 总之一句话,只有一个Activity,其他都是Fragment.

 框架总结:

 */
public class MainActivity1 extends BaseActivity
        implements BaseFragment.BackHandlerInterface {

    private static final String TAG =
            MainActivity1.class.getSimpleName();
    private static final boolean DEBUG = true;

    private BaseFragment mBaseFragment;
    private Fragment mCurShowMainFragment;
    private Fragment mPreShowMainFragment;

    private Fragment main1Fragment;
    private Fragment main2Fragment;
    private Fragment main3Fragment;
    private Fragment main4Fragment;

    private View mRootView;

    // Used to load the 'native-lib' library on application startup.
    static {
        try {
            System.loadLibrary("native-lib");
        } catch (java.lang.UnsatisfiedLinkError error) {
            error.printStackTrace();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private native String stringFromJNI();

    private static HashMap<String, Integer> sFragmentBackTypeSMap;

    static {
        // 如果有MainFragment(MainActivity中使用MainFragment,其他Fragment从MainFragment中被开启),
        // 那么不要把MainFragment加入Map中.
        sFragmentBackTypeSMap = new HashMap<String, Integer>();
        sFragmentBackTypeSMap.clear();
        sFragmentBackTypeSMap.put(
                A2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                B2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                C2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                D2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                E2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                Camera2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                CameraPreviewFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                AudioFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                DecodeAudioFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                DecodePlayFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                DecodeVideoFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                RecordScreenFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                TestMotionEventFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                ViewPagerFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                ThrowingScreenFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                VideoLiveBroadcastingFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                ContentsFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                MedicalRecordFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                HorizontalCardFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 不允许截屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        // Volume change should always affect media volume_normal
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mRootView = View.inflate(this, R.layout.activity_main, null);
        FrameLayout contentLayout = getContentLayout(this);
        if (contentLayout != null) {
            contentLayout.addView(mRootView);
        } else {
            finish();
            return;
        }
        // setContentView(R.layout.activity_main);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
        FragOperManager.getInstance().addActivity2(this, R.id.content_layout);
        if (savedInstanceState != null) {

        } else {

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            // 判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if (!hasIgnored) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }

            if (!isRunService(this, "com.weidi.usefragments.business.video_player.PlayerService")) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 0);
                } else {
                    startService(new Intent(this, PlayerService.class));
                }
            }
        }

        // start service
        //startService(new Intent(this, DownloadFileService.class));
        /*if (!isRunService(this, "com.weidi.usefragments.business.audio_player.MusicService")) {
            startService(new Intent(this, MusicService.class));
        }*/
        /*if (!isAccessibilitySettingsOn(getApplicationContext())) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }*/
        /*if (!isRunService(this, "com.weidi.usefragments.business.audio_player.JniMusicService")) {
            startService(new Intent(this, JniMusicService.class));
        }*/

        main1Fragment = new Main1Fragment();
        main2Fragment = new Main2Fragment();
        main3Fragment = new Main3Fragment();
        main4Fragment = new Main4Fragment();

        mCurShowMainFragment = main1Fragment;
        mPreShowMainFragment = main1Fragment;
        //        mCurShowMainFragment = main4Fragment;
        //        mPreShowMainFragment = main4Fragment;

        FragOperManager.getInstance().setCurUsedFragment(
                mCurShowMainFragment);

        FragOperManager.getInstance().enter2(main4Fragment);
        FragOperManager.getInstance().enter2(main3Fragment);
        FragOperManager.getInstance().enter2(main2Fragment);
        FragOperManager.getInstance().enter2(main1Fragment);
        //        FragOperManager.getInstance().enter2(main1Fragment);
        //        FragOperManager.getInstance().enter2(main2Fragment);
        //        FragOperManager.getInstance().enter2(main3Fragment);
        //        FragOperManager.getInstance().enter2(main4Fragment);

        findViewById(R.id.main1_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.main2_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.main3_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.main4_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.debug_test_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.back_btn).setOnClickListener(mViewOnClickListener);

        findViewById(R.id.main1_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_green_light));
        //        findViewById(R.id.main4_btn).setBackgroundColor(
        //                getResources().getColor(android.R.color.holo_green_light));

        if (mHomeWatcherReceiver == null) {
            mHomeWatcherReceiver = new HomeWatcherReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.setPriority(2147483647);
            intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            getContext().registerReceiver(mHomeWatcherReceiver, intentFilter);
        }

        // test
        Log.d(TAG, "onCreate() stringFromJNI(): " + stringFromJNI());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //test();
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (DEBUG)
            Log.d(TAG, "onRestart()");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            Log.d(TAG, "onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            Log.d(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            Log.d(TAG, "onDestroy()");
        FragOperManager.getInstance().removeActivity(this);
        if (mHomeWatcherReceiver != null) {
            getContext().unregisterReceiver(mHomeWatcherReceiver);
            mHomeWatcherReceiver = null;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            Log.d(TAG, "onLowMemory()");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            Log.d(TAG, "onTrimMemory() level: " + level);
    }

    /***
     * 如果多个Fragment以底部Tab方式呈现的话,
     * 那么这些Fragment中的onBackPressed()方法最好返回true.
     * 这样就不需要在MainActivityController中处理onResume()方法了.
     * 如果不是以这种方式呈现,那么这些Fragment中的onBackPressed()方法最好返回false.
     * 然后需要在MainActivityController中处理onResume()方法了.
     */
    @Override
    public void onBackPressed() {
        if (DEBUG)
            Log.d(TAG, "onBackPressed()");

        if (mBaseFragment == null
                || mBaseFragment.onBackPressed()) {
            this.finish();
            // this.exitActivity();
            return;
        }

        // 实现后退功能(把当前Fragment进行pop或者hide)
        final String fragmentName = mBaseFragment.getClass().getSimpleName();
        if (DEBUG)
            Log.d(TAG, "onBackPressed() fragmentName: " + fragmentName);
        for (String key : sFragmentBackTypeSMap.keySet()) {
            if (key.equals(fragmentName)) {
                FragOperManager.getInstance().onEvent(
                        sFragmentBackTypeSMap.get(key),
                        new Object[]{mBaseFragment});
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult() requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
        if (requestCode == 0
                && resultCode == Activity.RESULT_OK
                && null != data) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startService(new Intent(this, PlayerService.class));
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            Log.d(TAG, "onSaveInstanceState() outState: " + outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onRestoreInstanceState() savedInstanceState: " + savedInstanceState);
    }

    /**
     * 当配置发生变化时，不会重新启动Activity.
     * 但是会回调此方法，用户自行进行对屏幕旋转后进行处理.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w640dp h336dp 320dpi nrml long land
        // finger -keyb/v/h -nav/h s.264}
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w360dp h616dp 320dpi nrml long port
        // finger -keyb/v/h -nav/h s.265}
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged() newConfig: " + newConfig);

        handleConfigurationChangedEvent();
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment, String fragmentTag) {
        if (DEBUG)
            Log.d(TAG, "setSelectedFragment() selectedFragment: "
                    + selectedFragment.getClass().getSimpleName());
        mBaseFragment = selectedFragment;
    }

    private void handleConfigurationChangedEvent() {
        // 得到系统提供的用于显示用户界面的ContentLayout
        FrameLayout contentLayout = getContentLayout(this);
        if (contentLayout != null) {
            // 得到用于显示Fragment内容的容器布局
            FrameLayout fragmentContentLayout = mRootView.findViewById(R.id.content_layout);

            Map<Fragment, List<Fragment>> mapTemp =
                    FragOperManager.getInstance().getMainFragmentsMap();
            for (Map.Entry<Fragment, List<Fragment>> map : mapTemp.entrySet()) {
                String fragmentName = map.getKey().getClass().getSimpleName();
                if (TextUtils.isEmpty(fragmentName)) {
                    continue;
                }
                // remove mainFragment
                fragmentContentLayout.removeView(map.getKey().getView());
                List<Fragment> mainChildFragmentsList = map.getValue();
                if (mainChildFragmentsList == null
                        || mainChildFragmentsList.isEmpty()) {
                    continue;
                }
                for (Fragment mainChildFragment : mainChildFragmentsList) {
                    if (mainChildFragment == null
                            || fragmentName.equals(
                            mainChildFragment.getClass().getSimpleName())) {
                        continue;
                    }
                    fragmentContentLayout.removeView(mainChildFragment.getView());
                }
            }
            contentLayout.removeView(mRootView);

            // 当前Activity的新布局
            mRootView = View.inflate(this, R.layout.activity_main, null);
            fragmentContentLayout = mRootView.findViewById(R.id.content_layout);
            for (Map.Entry<Fragment, List<Fragment>> map : mapTemp.entrySet()) {
                String fragmentName = map.getKey().getClass().getSimpleName();
                if (TextUtils.isEmpty(fragmentName)) {
                    continue;
                }
                fragmentContentLayout.addView(map.getKey().getView());
                List<Fragment> mainChildFragmentsList = map.getValue();
                if (mainChildFragmentsList == null
                        || mainChildFragmentsList.isEmpty()) {
                    continue;
                }
                for (Fragment mainChildFragment : mainChildFragmentsList) {
                    if (mainChildFragment == null
                            || fragmentName.equals(
                            mainChildFragment.getClass().getSimpleName())) {
                        continue;
                    }
                    fragmentContentLayout.addView(mainChildFragment.getView());
                }
            }
            contentLayout.addView(mRootView);

            findViewById(R.id.main1_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.main2_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.main3_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.main4_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.debug_test_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.back_btn).setOnClickListener(mViewOnClickListener);

            if (mCurShowMainFragment == null) {
                return;
            }
            String mainFragmentName = mCurShowMainFragment.getClass().getSimpleName();
            if (TextUtils.isEmpty(mainFragmentName)) {
                mCurShowMainFragment = main1Fragment;
                findViewById(R.id.main1_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
                mPreShowMainFragment = mCurShowMainFragment;
                FragOperManager.getInstance().setCurUsedFragment(mCurShowMainFragment);
                FragOperManager.getInstance().changeTab();
                return;
            }
            if (mainFragmentName.equals(Main1Fragment.class.getSimpleName())) {
                findViewById(R.id.main1_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            } else if (mainFragmentName.equals(Main2Fragment.class.getSimpleName())) {
                findViewById(R.id.main2_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            } else if (mainFragmentName.equals(Main3Fragment.class.getSimpleName())) {
                findViewById(R.id.main3_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            } else if (mainFragmentName.equals(Main4Fragment.class.getSimpleName())) {
                findViewById(R.id.main4_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            }
        } else {
            finish();
            return;
        }
    }

    private void restoreBackgroundColor() {
        findViewById(R.id.main1_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
        findViewById(R.id.main2_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
        findViewById(R.id.main3_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
        findViewById(R.id.main4_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
    }

    private ShowTitleDialogFragment mShowTitleDialogFragment;

    private void testChangeFragment() {
        mShowTitleDialogFragment = new ShowTitleDialogFragment();
        mShowTitleDialogFragment.show(getFragmentManager(),
                ShowTitleDialogFragment.class.getSimpleName());
    }

    private View.OnClickListener mViewOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.back_btn) {
                        onBackPressed();

                        // test
                        /*FrameLayout fragmentLayout = mRootView.findViewById(R.id.content_layout);
                        FrameLayout fragmentLayoutTest = mRootView.findViewById(R.id
                        .content_layout_test);
                        LinearLayout fragmentRootLayout = mRootView.findViewById(R.id.root_layout);
                        fragmentLayout.removeView(fragmentRootLayout);
                        fragmentLayoutTest.addView(fragmentRootLayout);
                        fragmentRootLayout.invalidate();
                        fragmentLayoutTest.invalidate();*/
                        return;
                    }

                    if (v.getId() == R.id.debug_test_btn) {
                        testChangeFragment();
                        return;
                    }

                    restoreBackgroundColor();
                    switch (v.getId()) {
                        case R.id.main1_btn:
                            mCurShowMainFragment = main1Fragment;
                            findViewById(R.id.main1_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        case R.id.main2_btn:
                            mCurShowMainFragment = main2Fragment;
                            findViewById(R.id.main2_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        case R.id.main3_btn:
                            mCurShowMainFragment = main3Fragment;
                            findViewById(R.id.main3_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        case R.id.main4_btn:
                            mCurShowMainFragment = main4Fragment;
                            findViewById(R.id.main4_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        default:
                    }
                    if (mPreShowMainFragment == null
                            || mCurShowMainFragment == null) {
                        return;
                    }
                    if (mPreShowMainFragment != null
                            && mCurShowMainFragment != null
                            && mPreShowMainFragment == mCurShowMainFragment) {
                        return;
                    }
                    mPreShowMainFragment = mCurShowMainFragment;
                    FragOperManager.getInstance().setCurUsedFragment(mCurShowMainFragment);
                    FragOperManager.getInstance().changeTab();
                }
            };

    private HomeWatcherReceiver mHomeWatcherReceiver;

    private class HomeWatcherReceiver extends BroadcastReceiver {

        private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
        private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";

        //        IntentFilter mIntentFilter = new IntentFilter();
        //        mIntentFilter.setPriority(2147483647);
        //        mIntentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //        mAppsLockActivity.registerReceiver(mHomeWatcherReceiver, intentFilter);

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            if (DEBUG)
                Log.d(TAG, "onReceive() intent: " + intent);
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                // android.intent.action.CLOSE_SYSTEM_DIALOGS
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (TextUtils.isEmpty(reason)) {
                    return;
                }

                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    // 短按Home键
                    if (DEBUG)
                        Log.d(TAG, "onReceive() Home");
                    /*Intent intent1 = new Intent();
                    intent1.putExtra("TEST", "alexander");
                    MainActivity1.this.setResult(Activity.RESULT_OK, intent1);
                    MainActivity1.this.finish();*/
                } else if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                    // startSelf();
                    // 长按Menu键 或者 activity切换键
                    if (DEBUG)
                        Log.d(TAG, "onReceive() long press Menu key or switch Activity");
                }
                // 下面两个接收不到
                else if (SYSTEM_DIALOG_REASON_LOCK.equals(reason)) {
                    // 锁屏
                    if (DEBUG)
                        Log.d(TAG, "onReceive() lock");
                } else if (SYSTEM_DIALOG_REASON_ASSIST.equals(reason)) {
                    // samsung 长按Home键
                    if (DEBUG)
                        Log.d(TAG, "onReceive() assist");
                }
            }
        }
    }

    /***
     action=ACTION_DOWN, keyCode=KEYCODE_HEADSETHOOK, scanCode=226, metaState=0, flags=0x8,
     repeatCount=0, eventTime=300767594, downTime=300767594, deviceId=9, source=0x101
     action=ACTION_UP, keyCode=KEYCODE_HEADSETHOOK, scanCode=226, metaState=0, flags=0x8,
     repeatCount=0, eventTime=300767770, downTime=300767594, deviceId=9, source=0x101
     */
    /*@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (DEBUG)
            Log.d(TAG, "dispatchKeyEvent() event: " + event);
        return super.dispatchKeyEvent(event);
    }*/

    private SimpleAudioPlayer mSimpleAudioPlayer;
    private SimpleVideoPlayer mSimpleVideoPlayer;

    public void setSampleAudioPlayer(SimpleAudioPlayer simpleAudioPlayer) {
        mSimpleAudioPlayer = simpleAudioPlayer;
    }

    public void setSampleVideoPlayer(SimpleVideoPlayer simpleVideoPlayer) {
        mSimpleVideoPlayer = simpleVideoPlayer;
    }

    /***
     锁屏后不会被回调
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (mSimpleVideoPlayer != null) {
            return mSimpleVideoPlayer.onKeyDown(keyCode, event);
        }*/
        if (mBaseFragment != null
                && mBaseFragment instanceof DecodeVideoFragment) {
            DecodeVideoFragment decodeVideoFragment = (DecodeVideoFragment) mBaseFragment;
            return decodeVideoFragment.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isAccessibilitySettingsOn(Context context) {
        int accessibilityEnabled = 0;
        final String service =
                "com.weidi.usefragments/com.weidi.usefragments.service.AccessibilityImpl";
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter =
                new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "***ACCESSIBILIY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();

                    Log.v(TAG, "-------------- > accessabilityService :: " + accessabilityService);
                    if (accessabilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "We've found the correct setting - accessibility is switched " +
                                "on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "***ACCESSIBILIY IS DISABLED***");
        }

        return accessibilityFound;
    }

    /**
     * 判断无障碍服务是否开启
     *
     * @param context
     * @return
     */
    private boolean isStartAccessibilityServiceEnable(Context context) {
        AccessibilityManager accessibilityManager =
                (AccessibilityManager)
                        context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        assert accessibilityManager != null;
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().contains(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRunService(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo
                service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void test() {
        // 传递基本数据类型
        MLog.i(TAG, "from C String: " + JniUtils.getStringFromC("王力伟weidi5858258"));
        MLog.i(TAG, "from C int: " + JniUtils.getIntFromC(100));
        MLog.i(TAG, "from C byte: " + JniUtils.getByteFromC((byte) 128));
        MLog.i(TAG, "from C boolean: " + JniUtils.getBooleanFromC(true));
        MLog.i(TAG, "from C char: " + JniUtils.getCharFromC('a'));
        MLog.i(TAG, "from C short: " + JniUtils.getShortFromC((short) 1000));
        MLog.i(TAG, "from C long: " + JniUtils.getLongFromC(99999999L));
        MLog.i(TAG, "from C float: " + JniUtils.getFloatFromC(1000.00F));
        MLog.i(TAG, "from C double: " + JniUtils.getDoubleFromC(10000000000D));

        // 传递基本数据类型的数组
        String[] test = {"hahah", "ehehe", "哈哈", "中", "今天天气很好"};
        String[] str = JniUtils.getStringArrayFromC(test);
        for (int i = 0; i < str.length; i++) {
            MLog.i(TAG, "" + str[i]);
        }

        int[] in_ = {10, 20, 30, 40, 50};
        int[] in_2 = JniUtils.getIntArrayFromC(in_);
        for (int i = 0; i < in_2.length; i++) {
            MLog.i(TAG, "" + in_2[i]);
        }

        byte[] bt_ = {(byte) 128, (byte) 129, (byte) 130, (byte) -129, (byte) -130};
        byte[] bt_2 = JniUtils.getByteArrayFromC(bt_);
        for (int i = 0; i < bt_2.length; i++) {
            MLog.i(TAG, "" + bt_2[i]);
        }

        try {
            char[] char_ = {'伟', 'B', 'C', 'D', 'E'};
            char[] char_2 = JniUtils.getCharArrayFromC(char_);
            for (int i = 0; i < char_2.length; i++) {
                // 中文显示还是有问题
                MLog.i(TAG, new String(
                        String.valueOf(char_2[i]).getBytes("ISO-8859-1"), "UTF-8"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean[] boolean_ = {true, false, true, false, true};
        boolean[] boolean_2 = JniUtils.getBooleanArrayFromC(boolean_);
        for (int i = 0; i < boolean_2.length; i++) {
            MLog.i(TAG, "" + boolean_2[i]);
        }

        // 传递java端的对象
        Person person_ = new Person();
        person_.setName("伟弟");
        person_.setAge(30);
        Person person_2 = JniUtils.getObjectFromC(person_);
        if (person_2 != null) {
            MLog.i(TAG, "" + person_2.toString());
        }

        Person person_3 = new Person();
        person_3.setName("张三");
        person_3.setAge(31);
        Person person_4 = new Person();
        person_4.setName("李四");
        person_4.setAge(32);
        Person person_5 = new Person();
        person_5.setName("王五");
        person_5.setAge(33);
        Person[] persons_ = {person_3, person_4, person_5};
        Person[] persons_2 = JniUtils.getObjectArrayFromC(persons_);
        if (persons_2 != null) {
            for (int i = 0; i < persons_2.length; i++) {
                MLog.i(TAG, "" + persons_2[i].toString());
            }
        }

        /***
         {@link android.os.Environment#DIRECTORY_MUSIC}
         {@link android.os.Environment#DIRECTORY_MOVIES}
         {@link android.os.Environment#DIRECTORY_PICTURES}
         {@link android.os.Environment#DIRECTORY_PODCASTS}
         {@link android.os.Environment#DIRECTORY_RINGTONES}
         {@link android.os.Environment#DIRECTORY_ALARMS}
         {@link android.os.Environment#DIRECTORY_NOTIFICATIONS}

         访问的还是手机本身的存储卡,不是外置的SD卡
         getFilesDir            :
         /data/user/0/com.weidi.usefragments/files

         getCacheDir            :
         /data/user/0/com.weidi.usefragments/cache

         getExternalCacheDir    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/cache

         DIRECTORY_MUSIC        :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Music

         DIRECTORY_MOVIES       :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Movies

         DIRECTORY_PICTURES     :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Pictures

         DIRECTORY_PODCASTS     :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Podcasts

         DIRECTORY_RINGTONES    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Ringtones

         DIRECTORY_ALARMS       :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Alarms

         DIRECTORY_NOTIFICATIONS:
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Notifications
         */
        MLog.i(TAG, "getFilesDir            : " + getFilesDir().getAbsolutePath());
        MLog.i(TAG, "getCacheDir            : " + getCacheDir().getAbsolutePath());
        MLog.i(TAG, "getExternalCacheDir    : " + getExternalCacheDir().getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_MUSIC        : " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_MUSIC).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_MOVIES       : " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_MOVIES).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_PICTURES     : " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_PICTURES).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_PODCASTS     : " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_PODCASTS).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_RINGTONES    : " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_RINGTONES).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_ALARMS       : " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_ALARMS).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_NOTIFICATIONS: " + getExternalFilesDir(android.os.Environment
                .DIRECTORY_NOTIFICATIONS).getAbsolutePath());
        MLog.i(TAG, "DIRECTORY_MOVIES       : " + Environment.getExternalStoragePublicDirectory
                (android.os.Environment.DIRECTORY_MOVIES).getAbsolutePath());

        File file = new File(getFilesDir().getAbsolutePath());
        MLog.i(TAG, "canWrite1              : " + file.canWrite());
        file = new File(getCacheDir().getAbsolutePath());
        MLog.i(TAG, "canWrite2              : " + file.canWrite());
        file = new File(getExternalCacheDir().getAbsolutePath());
        MLog.i(TAG, "canWrite3              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite4              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite5              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite6              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PODCASTS)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite7              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_RINGTONES)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite8              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_RINGTONES)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite9              : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_ALARMS)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite10             : " + file.canWrite());
        file = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_NOTIFICATIONS)
                .getAbsolutePath());
        MLog.i(TAG, "canWrite11             : " + file.canWrite());
        file = new File(Environment.getExternalStoragePublicDirectory(android.os.Environment
                .DIRECTORY_MOVIES).getAbsolutePath());
        MLog.i(TAG, "canWrite12             : " + file.canWrite());// false

        /***
         Environment.DIRECTORY_MOVIES:
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Movies
         canWrite                    : true

         Environment.DIRECTORY_MOVIES:
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies
         canWrite                    : true

         Environment.MEDIA_MOUNTED   :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/mounted
         canWrite                    : true

         Environment.MEDIA_MOUNTED   :
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/mounted
         canWrite                    : true

         Environment.MEDIA_SHARED    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/shared
         canWrite                    : true

         Environment.MEDIA_SHARED    :
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/shared
         canWrite                    : true
         */
        File[] files;
        files = getExternalFilesDirs(Environment.DIRECTORY_MOVIES);
        for (File f : files) {
            MLog.i(TAG, "Environment.DIRECTORY_MOVIES: " + f.getAbsolutePath());
            MLog.i(TAG, "canWrite                    : " + f.canWrite());
        }
        files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
        for (File f : files) {
            MLog.i(TAG, "Environment.MEDIA_MOUNTED   : " + f.getAbsolutePath());
            MLog.i(TAG, "canWrite                    : " + f.canWrite());
        }
        files = getExternalFilesDirs(Environment.MEDIA_SHARED);
        for (File f : files) {
            MLog.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
            MLog.i(TAG, "canWrite                    : " + f.canWrite());
        }

        // com.eg.android.AlipayGphone com.tencent.mm tv.danmaku.bili
        // 判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        /*hasIgnored = powerManager.isIgnoringBatteryOptimizations("tv.danmaku.bili");
        if (!hasIgnored) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:tv.danmaku.bili"));
            startActivity(intent);
        }*/
    }

}