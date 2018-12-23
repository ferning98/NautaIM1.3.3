package com.fernapps.nautaim;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Created by FeRN@NDeZ on 05/04/2017.
 */

public class ConfirmationProcessor {
    Context context;
    DatabaseHelper helpperDb;
    SQLiteDatabase db;


    public ConfirmationProcessor(Context c) {
        context = c;
        helpperDb = new DatabaseHelper(context);
        db = helpperDb.getWritableDatabase();
    }

    public String processMessage(Message mensaje) {
        try {
            Date sentdate = mensaje.getSentDate();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sentdate);
            long dateMillis = calendar.getTimeInMillis();
            Address[] fromss = mensaje.getFrom();
            String fromOne = fromss[0].toString();
            String fromMail = fromOne;
            if (fromOne.contains("<") && fromOne.contains(">")) {
                int indexOfStart = fromOne.indexOf("<");
                int indexOfEnd = fromOne.indexOf(">");
                fromMail = fromOne.substring((indexOfStart + 1), indexOfEnd);
            }

            String subject = mensaje.getSubject().toLowerCase();
            Log.e("NAUTAIM", "ASUNTO CONFIRM RECIBIDO: " + subject);
            String msgId = subject.replace("fernappsnautachatconfirmacion:", "");

            ContentValues cv = new ContentValues();
            cv.put("state", "read");
            try {
                db.update("mensajes", cv, "_id = ?", new String[]{msgId});
            } catch (Exception e) {
                Toast t = Toast.makeText(context, "Ha recibido una confirmación de un mensaje eliminado", Toast.LENGTH_LONG);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            }

            mensaje.setFlag(Flags.Flag.DELETED, true);
            context.sendBroadcast(new Intent("updateConversations"));


        } catch (MessagingException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return "Ok";
    }

    public void addNewUserToDb(String nombre, String user) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + user + "'", null);

        if (c.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put("email", user);
            cv.put("nombre", nombre);
            db.insert("usuarios", "nombre", cv);
        }
        c.close();
    }

    public String getPrefValue(String key, String defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(key, defaultValue);
    }

    public void setPrefValue(String key, String value) {
        Intent prefIntent = new Intent(context, PreferencesHelperService.class);
        prefIntent.putExtra("key", key);
        prefIntent.putExtra("value", value);
        context.startService(prefIntent);
    }

}

