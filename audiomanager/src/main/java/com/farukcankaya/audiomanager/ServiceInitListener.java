package com.farukcankaya.audiomanager;

import com.farukcankaya.audiomanager.cons.State;
import com.farukcankaya.audiomanager.cons.Type;

/**
 * Created by Faruk Cankaya on 11/18/16.
 */

public interface ServiceInitListener {
    void ready(String filePath, Type type, State progress);
}
