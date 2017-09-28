package com.farukcankaya.audiomanager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Faruk Cankaya on 11/24/16.
 */

public class Util {
    public static String getFormattedTime(long millis) {
        java.text.DateFormat formatter = new SimpleDateFormat("mm:ss");
        Date time = new Date(millis);
        return formatter.format(time);
    }
}
