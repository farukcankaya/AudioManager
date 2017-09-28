package com.farukcankaya.audiomanager;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.farukcankaya.audiomanager.cons.Type;
import com.farukcankaya.audiomanager.service.Constants;

/**
 * This class uses the BroadcastReceiver framework to detect and handle status messages from
 * the service that downloads URLs.
 */
class RecorderProgressReceiver extends BroadcastReceiver {
    private AudioManager audioManager;
    private AudioListener audioListener;

    protected RecorderProgressReceiver(AudioManager audioManager, AudioListener audioListener) {
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
        long millis = intent.getLongExtra(Constants.EXTENDED_DATA_PROGRESS, -1);
        audioListener.onProgress(Type.RECORD, millis);
    }
}