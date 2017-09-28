package com.farukcankaya.audiomanager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.Hashtable;

/**
 * Created by Faruk Cankaya on 21/08/15.
 */
public class FontUtil {
    public static void setCustomFont(TextView textview, Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingAudioView);
        String font = a.getString(R.styleable.FloatingAudioView_fab_record_text_font);
        setCustomFont(textview, font, context);
        a.recycle();
    }

    public static void setCustomFont(View view, String font, Context context) {
        if (font == null) {
            return;
        }
        Typeface tf = get(font, context);
        if (tf != null) {
            ((TextView) view).setTypeface(tf);
        }
    }

    private static Hashtable<String, Typeface> fontCache = new Hashtable<>();

    private static Typeface get(String name, Context context) {
        Typeface typeface = fontCache.get(name);
        if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(context.getAssets(), name);
            } catch (Exception e) {
                return null;
            }
            fontCache.put(name, typeface);
        }
        return typeface;
    }
}
