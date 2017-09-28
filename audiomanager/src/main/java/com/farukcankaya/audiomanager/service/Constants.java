package com.farukcankaya.audiomanager.service;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

public class Constants {
    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "com.farukcankaya.audiomanager.BROADCAST.ACTION";
    // Defines a custom Intent action
    public static final String BROADCAST_PROGRESS = "com.farukcankaya.audiomanager.BROADCAST.PROGRESS";
    // Defines a custom Intent action
    public static final String BROADCAST_STATE = "com.farukcankaya.audiomanager.BROADCAST.STATE";


    public static final String EXTENDED_DATA_FILE_PATH = "com.farukcankaya.audiomanager.FILE_PATH";
    public static final String EXTENDED_DATA_TYPE = "com.farukcankaya.audiomanager.TYPE";
    public static final String EXTENDED_DATA_STATE = "com.farukcankaya.audiomanager.STATE";


    public static final String EXTENDED_DATA_STATUS = "com.farukcankaya.audiomanager.STATUS";
    public static final String EXTENDED_DATA_PROGRESS = "com.farukcankaya.audiomanager.PROGRESS";

    public static final String KEY_FILE_PATH = "com.farukcankaya.audiomanager.file.path";
    public static final String KEY_AUDIO_PROGRESS = "com.farukcankaya.audiomanager.audio.progress";

    // Defines a user action
    public static final String ACTION_INITIALIZE = "com.farukcankaya.audiomanager.initialize";
    public static final String ACTION_RECORD = "com.farukcankaya.audiomanager.record";
    public static final String ACTION_PLAY = "com.farukcankaya.audiomanager.play";
    public static final String ACTION_SEEK = "com.farukcankaya.audiomanager.seek";
    public static final String ACTION_STOP = "com.farukcankaya.audiomanager.stop";
    public static final String ACTION_CANCEL = "com.farukcankaya.audiomanager.cancel";

    public static final int STATE_ACTION_IDLE = 0;
    public static final int STATE_ACTION_RECORDING = 1;
    public static final int STATE_ACTION_PLAYING = 2;
    public static final int STATE_ACTION_STOPPED = 3;
    public static final int STATE_ACTION_CANCELED = 4;


    public interface ACTION {
        public static String MAIN_ACTION = "com.farukcankaya.audiomanager.action.main";
        public static String INIT_ACTION = "com.farukcankaya.audiomanager.action.init";
        public static String STOP_ACTION = "com.farukcankaya.audiomanager.action.prev";
        public static String CANCEL_ACTION = "com.farukcankaya.audiomanager.action.play";
        public static String NEXT_ACTION = "com.farukcankaya.audiomanager.action.next";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 373;
    }
}
