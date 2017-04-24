package com.cleartv.text2speech;

import android.graphics.Color;

/**
 * Created by Lipengfei on 2017/4/20.
 */

public abstract class LogListener {

    public void logInfo(String msg){
        logInfo(msg, Color.DKGRAY);
    }

    public abstract void logInfo(String msg, int color);
}
