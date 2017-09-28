package com.farukcankaya.audiomanager;

import com.farukcankaya.audiomanager.cons.Type;

/**
 * Created by Faruk Cankaya on 11/18/16.
 */

public interface AudioListener {
    void onReady(Type type, long millis);

    void onStarted(Type type);

    void onProgress(Type type, long millis);

    void onStopped(Type type, long duration);

    void onCanceled(Type type);
}
