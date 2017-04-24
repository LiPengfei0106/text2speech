package com.cleartv.text2speech;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cleartv.text2speech.server.VodServer;
import com.cleartv.text2speech.utils.Utils;

import static com.cleartv.text2speech.R.id.msg;
import static com.cleartv.text2speech.utils.Utils.getTime;

public class MainActivity extends AppCompatActivity {

    public static final String REBOOT_CODE = "110";
    public static final String RESTART_CODE = "120";
    public static final String SETTING_CODE = "119";

    private Context context;

    private LinearLayout msgll;
    private ScrollView sv_msg;

    private String code = "";
    public static String ipAddress;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int c = event.getKeyCode();
            Log.e("Lipengfei", c + "");
            if (c <= KeyEvent.KEYCODE_9 && c >= KeyEvent.KEYCODE_0) {
                c = c - KeyEvent.KEYCODE_0;
                code += c;
                if (code.length() > 9) {
                    code = code.substring(code.length() - 6);
                }
                Log.e("Lipengfei", code);
                if (code.endsWith(RESTART_CODE)) {
                    Utils.reStartApp();
                }
                if (code.endsWith(REBOOT_CODE)) {
                    logInfo("3秒后稍后重启机顶盒...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Utils.doReboot();
                        }
                    }).start();
                }
                if (code.endsWith(SETTING_CODE)) {
                    Utils.startAppByPackageName(context, "com.android.settings");
                }
            }else if(c == KeyEvent.KEYCODE_DPAD_DOWN || c == KeyEvent.KEYCODE_DPAD_UP){
                return super.dispatchKeyEvent(event);
            }
        }
        return true;
    }

    public Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        msgll = (LinearLayout) findViewById(msg);
        sv_msg = (ScrollView) findViewById(R.id.sv_msg);
        LogListener Speechlistener = new LogListener() {
            @Override
            public void logInfo(String msg, int color) {
                MainActivity.this.logInfo(msg, color);
            }

            @Override
            public void logInfo(String msg) {
                MainActivity.this.logInfo(msg, Color.GREEN);
            }
        };
        LogListener Serverlistener = new LogListener() {
            @Override
            public void logInfo(String msg, int color) {
                MainActivity.this.logInfo(msg, color);
            }

            @Override
            public void logInfo(String msg) {
                MainActivity.this.logInfo(msg, Color.BLUE);
            }
        };
        SpeechManager.getInstance().initialEnv(Speechlistener,this);
        VodServer.instance().setListener(Serverlistener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        logInfo("<---按“" + SETTING_CODE + "”进入系统设置--->");
        logInfo("<---按“" + RESTART_CODE + "”重启APP--->");
        logInfo("<---按“" + REBOOT_CODE + "”重启机顶盒--->");
        checkServer();
        setIP();
    }

    public void setIP(){
        ipAddress = "http://" + Utils.getLocalIPAddres() + ":" + VodServer.port;
        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle(ipAddress);
    }

    private void checkServer() {
        if (!VodServer.instance().isAlive()) {
            if (startServer()) {
                SpeechManager.getInstance().startTTS();
            }
        } else {
            logInfo("本地服务器已启动，端口号：" + VodServer.port);
            if(SpeechManager.getInstance().isAuthSuccess){
                logInfo("volume:音量(0-9)");
                logInfo("speed:语速(0-9)");
                logInfo("pitch:声调(0-9)");
                logInfo("speaker:音色(0.普通女声；1.普通男声；2.特别男声；3.情感男声；4.情感儿童声)");
                logInfo("--------------------------语音合成启动认证成功-----------------------------------\n");
            }else{
                SpeechManager.getInstance().startTTS();
            }
        }
    }

    private boolean startServer() {
        logInfo("正在启动本地服务器...");
        VodServer.instance().startWork();
        while (!VodServer.instance().wasStarted()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logInfo("本地服务器已启动，端口号：" + VodServer.port);
        return true;
    }

    private void logInfo(String msg) {
        logInfo(msg, Color.DKGRAY);
    }

    private void logInfo(final String msg, final int color) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                TextView view = new TextView(context);
                view.setText(getTime() + "： " + msg);
                view.setTextSize(16);
                view.setTextColor(color);
                msgll.addView(view);
                if (msgll.getChildCount() > 500)
                    msgll.removeViews(0, 300);

                sv_msg.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

}
