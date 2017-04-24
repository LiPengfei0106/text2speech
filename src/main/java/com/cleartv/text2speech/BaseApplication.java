package com.cleartv.text2speech;

import android.app.Application;
import android.os.Looper;
import android.widget.Toast;

import com.cleartv.text2speech.utils.Utils;

/**
 * Created by Lipengfei on 2017/4/20.
 */

public class BaseApplication extends Application {

    public static  Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(getApplicationContext(), "系统错误，稍后重启", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }.start();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                Utils.reStartApp();
            }
        });
    }

}
