package com.farukcankaya.audiomanager;

/**
 * Created by Faruk Cankaya on 11/17/16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.farukcankaya.audiomanager.cons.State;
import com.farukcankaya.audiomanager.cons.Type;

import static com.farukcankaya.audiomanager.service.Constants.EXTENDED_DATA_FILE_PATH;
import static com.farukcankaya.audiomanager.service.Constants.EXTENDED_DATA_STATE;
import static com.farukcankaya.audiomanager.service.Constants.EXTENDED_DATA_TYPE;

/**
 * This class uses the BroadcastReceiver framework to detect and handle status messages from
 * the service that downloads URLs.
 */
class ServiceInitReceiver extends BroadcastReceiver {
    private ServiceInitListener serviceInitListener;

    protected ServiceInitReceiver(ServiceInitListener serviceInitListener) {
        this.serviceInitListener = serviceInitListener;
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
        if (serviceInitListener == null) {
            return;
        }
        Type type = Type.RECORD;
        State state = State.IDLE;
        String filePath = null;

        try {
            String typeStr = intent.getStringExtra(EXTENDED_DATA_TYPE);
            String stateStr = intent.getStringExtra(EXTENDED_DATA_STATE);
            if (typeStr != null) {
                type = Type.valueOf(typeStr);
            }
            if (stateStr != null) {
                state = State.valueOf(stateStr);
            }
            filePath = intent.getStringExtra(EXTENDED_DATA_FILE_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }

        serviceInitListener.ready(filePath, type, state);
    }
}