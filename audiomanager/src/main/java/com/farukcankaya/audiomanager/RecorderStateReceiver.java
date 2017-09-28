package com.farukcankaya.audiomanager;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.farukcankaya.audiomanager.cons.Type;
import com.farukcankaya.audiomanager.service.Constants;

import static com.farukcankaya.audiomanager.service.Constants.EXTENDED_DATA_PROGRESS;
import static com.farukcankaya.audiomanager.service.Constants.EXTENDED_DATA_TYPE;

/**
 * This class uses the BroadcastReceiver framework to detect and handle status messages from
 * the service that downloads URLs.
 */
class RecorderStateReceiver extends BroadcastReceiver {

    private AudioListener audioListener;
    private AudioManager audioManager;

    protected RecorderStateReceiver(AudioManager audioManager, AudioListener audioListener) {
        this.audioManager = audioManager;
        this.audioListener = audioListener;

        // prevents instantiation by other packages.
    }

    /**
     * This method is called by the system when a broadcast Intent is matched by this class'
     * intent filters
     *
     * @param context An Android context
     * @param intent  The incoming broadcast Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (audioListener == null) {
            return;
        }

        /**
         * Gets the status from the Intent's extended data, and chooses the appropriate action
         */
        Type type = Type.valueOf(intent.getStringExtra(EXTENDED_DATA_TYPE));
        switch (intent.getIntExtra(Constants.EXTENDED_DATA_STATUS, Constants.STATE_ACTION_IDLE)) {
            case Constants.STATE_ACTION_IDLE:
                long millis = intent.getLongExtra(EXTENDED_DATA_PROGRESS, 0);
                audioListener.onReady(type, millis);
                break;

            case Constants.STATE_ACTION_RECORDING:
                audioListener.onStarted(type);
                break;

            case Constants.STATE_ACTION_PLAYING:
                audioListener.onStarted(type);
                break;

            case Constants.STATE_ACTION_STOPPED:
                long duration = intent.getLongExtra(EXTENDED_DATA_PROGRESS, 0);
                audioListener.onStopped(type, duration);
                audioManager.destroy();
                audioManager = null;
                audioListener = null;
                break;

            case Constants.STATE_ACTION_CANCELED:
                audioListener.onCanceled(type);
                audioManager.destroy();
                audioManager = null;
                audioListener = null;
                break;
            default:
                break;
        }
    }
}