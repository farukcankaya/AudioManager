package com.farukcankaya.audiomanager.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.farukcankaya.audiomanager.AudioManager;
import com.farukcankaya.audiomanager.R;
import com.farukcankaya.audiomanager.Util;
import com.farukcankaya.audiomanager.cons.State;
import com.farukcankaya.audiomanager.cons.Type;

import java.io.File;
import java.io.IOException;

import static android.R.attr.max;
import static com.farukcankaya.audiomanager.service.Constants.ACTION_CANCEL;
import static com.farukcankaya.audiomanager.service.Constants.ACTION_INITIALIZE;
import static com.farukcankaya.audiomanager.service.Constants.ACTION_PLAY;
import static com.farukcankaya.audiomanager.service.Constants.ACTION_RECORD;
import static com.farukcankaya.audiomanager.service.Constants.ACTION_SEEK;
import static com.farukcankaya.audiomanager.service.Constants.ACTION_STOP;
import static com.farukcankaya.audiomanager.service.Constants.KEY_AUDIO_PROGRESS;
import static com.farukcankaya.audiomanager.service.Constants.KEY_FILE_PATH;
import static com.farukcankaya.audiomanager.service.Constants.STATE_ACTION_CANCELED;
import static com.farukcankaya.audiomanager.service.Constants.STATE_ACTION_PLAYING;
import static com.farukcankaya.audiomanager.service.Constants.STATE_ACTION_RECORDING;
import static com.farukcankaya.audiomanager.service.Constants.STATE_ACTION_STOPPED;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

public class AudioManagerService extends Service {
    public static final String TAG = "AudioRecorderService";
    private Handler mHandler;
    private NotificationManager mNotificationManager;

    private BroadcastNotifier mBroadcaster;

    private Type type;
    private State state;
    private MediaRecorder recorder;
    private String filePath;
    private long duration = 0;
    private long startTime = 0;
    private long currentTime = 0;
    private long oldTime = 0;
    private Thread thread;

    private MediaPlayer player;

    public static void init(Context context) {
        Intent serviceIntent = new Intent(context, AudioManagerService.class);
        serviceIntent.setAction(ACTION_INITIALIZE);
        context.startService(serviceIntent);
    }

    public static void start(Context context, String filePath) {
        Intent serviceIntent = new Intent(context, AudioManagerService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        serviceIntent.setAction(ACTION_RECORD);
        serviceIntent.putExtra(KEY_FILE_PATH, filePath);
        context.startService(serviceIntent);
    }

    public static void play(Context context, String filePath) {
        Intent serviceIntent = new Intent(context, AudioManagerService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        serviceIntent.setAction(ACTION_PLAY);
        serviceIntent.putExtra(KEY_FILE_PATH, filePath);
        context.startService(serviceIntent);
    }

    public static void seek(Context context, long millis) {
        Intent serviceIntent = new Intent(context, AudioManagerService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        serviceIntent.setAction(ACTION_SEEK);
        serviceIntent.putExtra(KEY_AUDIO_PROGRESS, millis);
        context.startService(serviceIntent);
    }

    public static void stop(Context context) {
        Intent serviceIntent = new Intent(context, AudioManagerService.class);
        serviceIntent.setAction(ACTION_STOP);
        context.startService(serviceIntent);
    }

    public static void cancel(Context context) {
        Intent serviceIntent = new Intent(context, AudioManagerService.class);
        serviceIntent.setAction(ACTION_CANCEL);
        context.startService(serviceIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        // Defines and instantiates an object for handling status updates.
        mBroadcaster = new BroadcastNotifier(getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved");
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onRebind");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (intent == null || intent.getAction() == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        if (intent.getAction() == ACTION_INITIALIZE) {
            mBroadcaster.broadcastState(this.filePath, type, state);
            if (this.filePath == null) {
                stopSelf();
                stopForeground(true);
            }
        } else if (intent.getAction() == ACTION_RECORD) {
            initialize(intent.getStringExtra(KEY_FILE_PATH));
        } else if (intent.getAction() == ACTION_PLAY) {
            play(intent.getStringExtra(KEY_FILE_PATH));
        } else if (intent.getAction() == ACTION_SEEK) {
            seekTo(intent.getLongExtra(KEY_AUDIO_PROGRESS, 0));
        } else if (intent.getAction() == ACTION_STOP) {
            stop(true);
        } else if (intent.getAction() == ACTION_CANCEL) {
            stop(false);
        } else if (intent.getAction().equals(Constants.ACTION.STOP_ACTION)) {
            stop(true);
        } else if (intent.getAction().equals(Constants.ACTION.CANCEL_ACTION)) {
            stop(false);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        type = null;
        state = null;
        stopForeground(true);
        super.onDestroy();
    }

    private void initialize(String filePath) {
        if (this.filePath != null) {
            stop(true);
        }

        this.filePath = filePath;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
    }

    private void record() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(filePath);
        recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                stop(true);
            }
        });

        try {
            recorder.prepare();
            recorder.start();
            type = Type.RECORD;
            state = State.PROGRESS;
            startTime = System.currentTimeMillis();
            mBroadcaster.broadcastState(this.filePath, Type.RECORD, State.PROGRESS);
            mBroadcaster.broadcastAction(STATE_ACTION_RECORDING, type, 0);
            thread = new Thread() {
                @Override
                public void run() {
                    long oldTime = System.currentTimeMillis();
                    while (state == com.farukcankaya.audiomanager.cons.State.PROGRESS) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - oldTime >= 1000) {
                            oldTime = currentTime;
                            duration = (((currentTime - startTime)));
                            mBroadcaster.notifyProgress(duration);
                            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, createNotification(duration, duration));
                        }
                    }
                }
            };
            thread.start();

            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, createNotification(0, 0));
        } catch (IOException e) {
            if (e != null) {
                Log.e(TAG, e.getLocalizedMessage());
                stop(true);
            }
        }
    }

    public void seekTo(long millis) {
        if (player != null) {
            player.seekTo((int) millis);
        }
    }

    public void play(String filePath) {
        if (this.filePath != null) {
            stop(true);
        }

        this.filePath = filePath;

        if (player != null) {
            player.release();
            player = null;
        }

        try {
            player = new MediaPlayer();
            player.setDataSource(filePath);
            player.prepare();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    AudioManagerService.stop(getApplicationContext());
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    AudioManagerService.stop(getApplicationContext());
                    return false;
                }
            });
            player.start();
        } catch (IOException e) {
            stop(false);
            Log.e(TAG, "prepare() failed");
        }

        type = Type.PLAY;
        state = State.PROGRESS;
        startTime = System.currentTimeMillis();
        mBroadcaster.broadcastAction(STATE_ACTION_PLAYING, type, player.getDuration());

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player != null) {
                    duration = player.getDuration();
                    mBroadcaster.notifyProgress(player.getCurrentPosition());
                    mHandler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    public void stop(boolean isSave) {
        if (type == Type.RECORD) {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        } else if (type == Type.PLAY) {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
        }

        state = null;

        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        if (isSave) {
            mBroadcaster.broadcastState(this.filePath, Type.PLAY, State.IDLE);
            mBroadcaster.broadcastAction(STATE_ACTION_STOPPED, Type.PLAY, duration);
        } else {
            // TODO: test! it could throw exception
            try {
                File audioFile = new File(filePath);
                if (audioFile.exists()) {
                    audioFile.delete();
                }
            } catch (Exception e) {
            }

            this.filePath = null;
            mBroadcaster.broadcastState(this.filePath, Type.RECORD, State.IDLE);
            mBroadcaster.broadcastAction(STATE_ACTION_CANCELED, Type.RECORD, 0);
        }
        stopForeground(true);
        stopSelf();
    }

    private Notification createNotification(long currentDuration, long duration) {
        Intent stopIntent = new Intent(this, AudioManagerService.class);
        stopIntent.setAction(Constants.ACTION.STOP_ACTION);
        PendingIntent pStopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);

        Intent cancelIntent = new Intent(this, AudioManagerService.class);
        cancelIntent.setAction(Constants.ACTION.CANCEL_ACTION);
        PendingIntent pCancelIntent = PendingIntent.getService(this, 0,
                cancelIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher);

        RemoteViews remoteview =
                new RemoteViews(getPackageName(), R.layout.fc_audio_manager_notification_layout);
        remoteview.setImageViewResource(R.id.notification_img, R.drawable.stop);
        remoteview.setTextViewText(R.id.duration, Util.getFormattedTime(currentDuration));
        remoteview.setProgressBar(R.id.notification_progress_bar, (int) duration, (int) currentDuration, true);

        // Intent
        Intent intent = new Intent(getApplicationContext(), AudioManager.intentClass);
        intent.setAction("PUSH_ACTION");
        intent.putExtra("PUSH_FLAG", true);

        PendingIntent pendingIntent = null;
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

        }

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notification_recording)
                .setContentTitle(getResources().getString(R.string.fc_audio_manager_notification_recording))
                .setOngoing(true)
                .setLargeIcon(icon)
                .setTicker(getResources().getString(R.string.fc_audio_manager_notification_recording))
                //.setCustomContentView(remoteview)
                //.setStyle(new Notification.DecoratedCustomViewStyle())
                .addAction(android.R.drawable.ic_media_pause,
                        getResources().getString(R.string.fc_audio_manager_notification_stop),
                        pStopIntent);
                /*.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getResources().getString(R.string.fc_audio_manager_notification_cancel),
                        pCancelIntent)*/
        if (pendingIntent != null) {
            notificationCompatBuilder.setContentIntent(pendingIntent);
        }
        if (max > 0) {
            notificationCompatBuilder.setProgress((int) duration, (int) currentDuration, true);
            notificationCompatBuilder.setContentText(Util.getFormattedTime(currentDuration));
        }
        return notificationCompatBuilder.build();
    }
}