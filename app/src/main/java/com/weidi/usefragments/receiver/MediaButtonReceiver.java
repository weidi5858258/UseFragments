package com.weidi.usefragments.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

import com.weidi.usefragments.tool.MLog;

/**
 * Created by root on 19-7-2.
 * 没有效果
 */

public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG =
            MediaButtonReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            if (DEBUG)
                MLog.d(TAG, "MediaButtonReceiver " + intentAction);
            KeyEvent event =
                    (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            int action = event.getAction();
            if (DEBUG)
                MLog.d(TAG, "MediaButtonReceiver action: " + action +
                        " " + event.getKeyCode());
            //long eventtime = event.getEventTime();

            // single quick press: pause/resume.
            // double press: next track
            // long press: start auto-shuffle mode.

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
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
            if (DEBUG)
                MLog.d(TAG, "MediaButtonReceiver " + intentAction);
        }
    }
}
