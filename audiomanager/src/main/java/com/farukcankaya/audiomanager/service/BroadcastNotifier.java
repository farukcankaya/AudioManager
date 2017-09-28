package com.farukcankaya.audiomanager.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.farukcankaya.audiomanager.cons.State;
import com.farukcankaya.audiomanager.cons.Type;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

public class BroadcastNotifier {
    private LocalBroadcastManager mBroadcaster;
    private Context context;

    public BroadcastNotifier(Context context) {
        this.context = context;
        mBroadcaster = LocalBroadcastManager.getInstance(context);

    }

    public void broadcastAction(int state, Type type, long millis) {

        Intent localIntent = new Intent();

        // The Intent contains the custom broadcast action for this app
        localIntent.setAction(Constants.BROADCAST_ACTION);
        localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, state);
        if (type != null) {
            localIntent.putExtra(Constants.EXTENDED_DATA_TYPE, type.toString());
        }
        localIntent.putExtra(Constants.EXTENDED_DATA_PROGRESS, millis);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        // Broadcasts the Intent
        if (context != null) {
            mBroadcaster.sendBroadcast(localIntent);
        }
    }

    public void notifyProgress(long millis) {

        Intent localIntent = new Intent();

        // The Intent contains the custom broadcast action for this app
        localIntent.setAction(Constants.BROADCAST_PROGRESS);

        // Puts log data into the Intent
        localIntent.putExtra(Constants.EXTENDED_DATA_PROGRESS, millis);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        if (context != null) {
            mBroadcaster.sendBroadcast(localIntent);
        }
    }

    public void broadcastState(String filePath, Type type, State state) {

        Intent localIntent = new Intent();
        localIntent.setAction(Constants.BROADCAST_STATE);
        if (filePath != null) {
            localIntent.putExtra(Constants.EXTENDED_DATA_FILE_PATH, filePath);
        }

        if (type != null) {
            localIntent.putExtra(Constants.EXTENDED_DATA_TYPE, type.toString());
        }

        if (state != null) {
            localIntent.putExtra(Constants.EXTENDED_DATA_STATE, state.toString());
        }
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        if (context != null) {
            mBroadcaster.sendBroadcast(localIntent);
        }
    }

}
