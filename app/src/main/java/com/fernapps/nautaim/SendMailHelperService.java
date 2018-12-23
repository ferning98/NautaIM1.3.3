package com.fernapps.nautaim;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

public class SendMailHelperService extends Service {
    boolean isSending = false;
    DatabaseHelper helpperDb;
    SQLiteDatabase db;

    public SendMailHelperService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        helpperDb = new DatabaseHelper(this);
        db = helpperDb.getWritableDatabase();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isSending) {
            Cursor c = db.rawQuery("SELECT * FROM cola ORDER BY _id", null);
            if (c.getCount() > 0) {
                isSending = true;
                c.moveToFirst();
                Cursor msgCursor = db.rawQuery("SELECT * FROM mensajes WHERE _id = " + c.getString(3), null);
                Log.e("NAUTAIM", msgCursor.toString());
                if (msgCursor.getCount() > 0)
                {
                    msgCursor.moveToFirst();
                    String tipo = msgCursor.getString(6);
                    String conf = c.getString(4);
                    if (conf != null) {
                        Intent startSendingMailIntent = new Intent(SendMailHelperService.this, SendConfirmationService.class);
                        startSendingMailIntent.putExtra("colaid", c.getString(0));
                        startService(startSendingMailIntent);
                    } else if (tipo.equals("image") || tipo.equals("audio") || tipo.equals("other")) {
                        Intent startSendingMailIntent = new Intent(SendMailHelperService.this, SendImageService.class);
                        startSendingMailIntent.putExtra("colaid", c.getString(0));
                        startService(startSendingMailIntent);
                    } else if (tipo.equals("text")) {
                        Intent startSendingMailIntent = new Intent(SendMailHelperService.this, SendEmailServce.class);
                        startSendingMailIntent.putExtra("colaid", c.getString(0));
                        startService(startSendingMailIntent);
                    }

                }
                else
                {
                    db.delete("cola", "_id = ?", new String[]{c.getString(0)});
                }

                c.close();
                msgCursor.close();
            } else {
                c.close();
                isSending = false;
                stopSelf();
            }
        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isSending = false;
    }


    public String getPrefValue(String key, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences("com.fernapps.nautaim_preferences", 0);
        return sharedPref.getString(key, defaultValue);
    }

    public void setPrefValue(String key, String value) {
        Intent prefIntent = new Intent(SendMailHelperService.this, PreferencesHelperService.class);
        prefIntent.putExtra("key", key);
        prefIntent.putExtra("value", value);
        startService(prefIntent);
    }
}
