package com.weidi.usefragments.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.business.contents.JniPlayerActivity;

/***
 Created by root on 19-7-2.
 这个广播需要两个地方注册才有效
 1.AndroidManifest.xml
 2.SimpleAudioPlayer(AudioManager)
 */

public class MediaButtonReceiver extends BroadcastReceiver {

    private static final String TAG =
            MediaButtonReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        /*if (DEBUG)
            MLog.d(TAG, "MediaButtonReceiver " + intentAction);*/
        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        EventBusUtils.post(
                                JniPlayerActivity.class, KeyEvent.KEYCODE_HEADSETHOOK, null);
                        /*EventBusUtils.post(
                                SimpleAudioPlayer.class, KeyEvent.KEYCODE_HEADSETHOOK, null);
                        EventBusUtils.post(
                                SimpleVideoPlayer7.class, KeyEvent.KEYCODE_HEADSETHOOK, null);*/
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    break;
                default:
                    break;
            }
        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            // 耳机插拔
        }
    }

}
