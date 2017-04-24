package com.cleartv.text2speech.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cleartv.text2speech.MainActivity;

public class BootBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "BootBroadcastReceiver";
    
    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "receiver action: " + action);
        
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(
                    Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1)
                    .get(0).topActivity;
            if (cn.getPackageName().equalsIgnoreCase(context.getPackageName())) {
                Log.d(TAG, "already boot, ignore BootBroadcast");
                return;
            }
        } catch (Exception e) {
            
        }
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent ootStartIntent = new Intent(context, MainActivity.class);
            ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(ootStartIntent);
        }
        
    }

}
