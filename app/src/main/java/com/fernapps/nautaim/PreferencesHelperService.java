package com.fernapps.nautaim;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

public class PreferencesHelperService extends Service {
    public PreferencesHelperService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            String key = intent.getStringExtra("key");
            String value = intent.getStringExtra("value");
            if (key != null && value != null) {
                setPrefValue(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    public void setPrefValue(final String key, final String value) {
        try {
            SharedPreferences sharedPref = getSharedPreferences("com.fernapps.nautaim_preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, value);
            editor.apply();
        } catch (Exception e) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    setPrefValue(key, value);
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 1000);
        }

    }

}
