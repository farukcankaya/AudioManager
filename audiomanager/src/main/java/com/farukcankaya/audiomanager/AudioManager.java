package com.farukcankaya.audiomanager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.farukcankaya.audiomanager.cons.State;
import com.farukcankaya.audiomanager.cons.Type;
import com.farukcankaya.audiomanager.service.AudioManagerService;
import com.farukcankaya.audiomanager.service.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

public class AudioManager implements ServiceInitListener {
    private AudioListener audioListener;

    private Context context;
    private String filePath;

    public static Class intentClass;
    public static String PUSH_FLAG = "PUSH_FLAG";

    private long time;

    RecorderStateReceiver mRecorderStateReceiver;
    RecorderProgressReceiver mRecorderProgressReceiver;
    ServiceInitReceiver mServiceInitReceiver;

    Type lastType = Type.RECORD;

    /**
     * @param context
     * @param filePath absolute path of the file
     */
    public AudioManager(@NonNull Context context, @NonNull String filePath) {
        this(context, filePath, null);
    }

    public AudioManager(@NonNull Context context, @NonNull String filePath,
                        @Nullable final AudioListener audioListener) {
        this.context = context;
        this.filePath = filePath;
        this.audioListener = audioListener;

        mServiceInitReceiver = new ServiceInitReceiver(this);
        IntentFilter intentFilter = new IntentFilter(
                Constants.BROADCAST_STATE);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(context).registerReceiver(
                mServiceInitReceiver,
                intentFilter);
        AudioManagerService.init(context);
    }

    @Override
    public void ready(String filePath, Type type, State progress) {
        if (audioListener == null) {
            return;
        }
        /**
         * Service is already running at the background.
         */
        long millis = 0;
        File file = null;
        if (AudioManager.this.filePath != null) {
            file = new File(AudioManager.this.filePath);
            if (file != null && file.exists()) {
                MediaPlayer player;
                try {
                    player = new MediaPlayer();
                    player.setDataSource(this.filePath);
                    player.prepare();
                    millis = player.getDuration();
                } catch (IOException e) {

                }
            }
        }

        if (progress == State.PROGRESS && AudioManager.this.filePath != null && AudioManager.this.filePath.equals(filePath)) {
            audioListener.onReady(type, millis);
            audioListener.onStarted(type);
            setListeners();
        } else {
            if (file != null && file.exists()) {
                audioListener.onReady(Type.PLAY, millis);
            } else {
                audioListener.onReady(Type.RECORD, 0);
            }
        }
    }

    private void setListeners() {
        // Instantiates a new RecorderStateReceiver
        if (mRecorderStateReceiver == null) {
            mRecorderStateReceiver = new RecorderStateReceiver(this, this.audioListener);

            // The filter's action is BROADCAST_ACTION
            IntentFilter statusIntentFilter = new IntentFilter(
                    Constants.BROADCAST_ACTION);
            // Sets the filter's category to DEFAULT
            statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            // Registers the RecorderStateReceiver and its intent filters
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    mRecorderStateReceiver,
                    statusIntentFilter);
        }

        if (mRecorderProgressReceiver == null) {
            mRecorderProgressReceiver = new RecorderProgressReceiver(this, this.audioListener);

            // The filter's action is BROADCAST_ACTION
            IntentFilter progressIntentFilter = new IntentFilter(
                    Constants.BROADCAST_PROGRESS);
            // Sets the filter's category to DEFAULT
            progressIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            // Registers the RecorderStateReceiver and its intent filters
            LocalBroadcastManager.getInstance(context).registerReceiver(
                    mRecorderProgressReceiver,
                    progressIntentFilter);
        }
    }

    /**
     *
     */
    public void record() {
        if (System.currentTimeMillis() - time < 1000) {
            return;
        }
        time = System.currentTimeMillis();
        if (isServiceRunning(context, AudioManagerService.class)) {
            AudioManagerService.stop(context);
            return;
        }
        lastType = Type.RECORD;
        setListeners();
        AudioManagerService.start(context, filePath);
    }

    /**
     *
     */
    public void play() {
        if (isServiceRunning(context, AudioManagerService.class)) {
            //AudioManagerService.stop(context);
            return;
        }
        lastType = Type.PLAY;
        setListeners();
        AudioManagerService.play(context, filePath);
    }

    /**
     * @param millis stating point of the audio
     */
    public void seekTo(long millis) {
        if (!isServiceRunning(context, AudioManagerService.class)) {
            return;
        }
        AudioManagerService.seek(context, millis);
    }

    /**
     *
     */
    public void stop() {
        AudioManagerService.stop(context);
    }

    /**
     *
     */
    public void cancel() {
        this.filePath = null;
        AudioManagerService.cancel(context);
    }

    /**
     *
     */
    public void stopPlaying() {
        if (lastType == Type.PLAY) {
            AudioManagerService.stop(context);
        }
    }

    public void destroy() {
        if (mServiceInitReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mServiceInitReceiver);
            mServiceInitReceiver = null;
        }

        if (mRecorderStateReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mRecorderStateReceiver);
            mRecorderStateReceiver = null;
        }

        if (mRecorderProgressReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mRecorderProgressReceiver);
            mRecorderProgressReceiver = null;
        }
    }

    public void setAudioListener(AudioListener audioListener) {
        this.audioListener = audioListener;
    }

    public static void setIntentClass(Class intentClass) {
        AudioManager.intentClass = intentClass;
    }

    private boolean isServiceRunning(Context context, Class serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            String c = serviceName.getCanonicalName();
            String d = service.service.getClassName();
            if (c.equals(d)) {
                return true;
            }
        }
        return false;
    }
}