package com.fernapps.nautaim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

public class SendEmailServce extends Service {

    DatabaseHelper helpperDb;
    SQLiteDatabase db;
    String sendingId = "";
    String mailTo = "";

    public SendEmailServce() {
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

    public String getPrefValue(String key, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences("com.fernapps.nautaim_preferences", 0);
        return sharedPref.getString(key, defaultValue);
    }

    public void setPrefValue(String key, String value) {
        Intent prefIntent = new Intent(SendEmailServce.this, PreferencesHelperService.class);
        prefIntent.putExtra("key", key);
        prefIntent.putExtra("value", value);
        startService(prefIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(SendEmailServce.this);
        mBuilder.setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Enviando, espere...")
                .setProgress(0, 0, true);
        startForeground(9808, mBuilder.build());
        try {
            new SendEmail().execute(intent.getStringExtra("colaid"));
            return START_STICKY;
        } catch (Exception e) {
            stopSelf();
        }
        return START_STICKY;
    }

    public String getUserIdFromMail(String email) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + email + "'", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String id = c.getString(0);
            c.close();
            return id;
        }
        return "1";
    }



    //////////////SEND EMAILLLLLLL


    private class SendEmail extends AsyncTask<String, String, String> {
        protected String doInBackground(String... subject) {


            sendingId = "";
            Properties props = new Properties();
            String host = getPrefValue("serverOut", "181.225.231.12");
            String port = getPrefValue("serverOutPort", "25");
            String cifrado = getPrefValue("pref_cifrado", "0");
            String timeout = getPrefValue("timeout", "30");
            int timetime = 0;
            if (timeout.isEmpty()) {
                timeout = "20";
            }
            try {
                timetime = Integer.parseInt(timeout);
                if (timetime < 15) {
                    timeout = "15";
                }
            } catch (Exception e) {
                timeout = "20";
            }
            if (host.isEmpty()) {
                host = "181.225.231.12";
            }
            if (port.isEmpty()) {
                port = "25";
            }
            timeout = "" + (Integer.parseInt(timeout) * 1000);
            props.put("mail.smtp.host", host); //RELEASE smtp.nauta.cu
            props.put("mail.smtp.timeout", timeout);
            props.put("mail.smtp.quitwait", "false");
            props.put("mail.smtp.sendpartial", "false");
            props.put("mail.smtp.localhost", "localhost");
            //props.put("mail.smtp.host", "192.168.137.1"); //DEBUG
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            if (cifrado.equals("2")) {
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            } else if (cifrado.equals("1")) {
                props.put("mail.smtp.starttls.enable", "true");
            } else {
                props.put("mail.imap.starttls.enable", "false");
            }

            final String fromEmail = getPrefValue("username_mail", "ERROR");
            final String password = getPrefValue("pass_email", "ERROR");
            final String fromName = getPrefValue("name_email", "Usuario Nauta IM");
            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            };
            Session session = Session.getInstance(props, auth);
            session.setDebug(true);
            Cursor c = db.rawQuery("SELECT * FROM cola WHERE _id = " + subject[0], null);
            c.moveToFirst();
            if (c.getCount() > 0) {
                String toEmail = c.getString(1);
                mailTo = toEmail;
                String msgid = c.getString(3);
                String asunto = "FernappsNautaChatMensajeNuevo" + msgid;
                String emailBody = c.getString(2) + "||--||";
                String colaId = c.getString(0);
                ContentValues cvA = new ContentValues();
                cvA.put("state", "sending");
                db.update("mensajes", cvA, "state LIKE ?", new String[]{"error"});
                sendBroadcast(new Intent("updateConversations"));
                String[] result = EmailUtil.sendEmail(session, fromEmail, toEmail, asunto, emailBody, SendEmailServce.this, fromName);
                String tipo = result[0];
                String error = result[1];
                if (tipo != null && tipo.equals("Ok")) {
                    ContentValues cv = new ContentValues();
                    cv.put("state", "ok");
                    cv.put("date", "" + System.currentTimeMillis());
                    db.update("mensajes", cv, "_id = ?", new String[]{msgid});
                    sendBroadcast(new Intent("updateConversations"));
                    db.delete("cola", "_id = ?", new String[]{colaId});
                    if (c.getString(2).equals("TWUgdm95IGRlIGVzdGUgZ3J1cG8h"))
                    {
                        Cursor cursorMsg = db.rawQuery("SELECT * FROM mensajes WHERE _id = " + c.getString(3), null);
                        if (cursorMsg.getCount() > 0)
                        {
                            cursorMsg.moveToFirst();
                            ContentValues cvX = new ContentValues();
                            Log.e("NAUTAIM", "Saliendo del grupo, mensaje ID: " + c.getString(3));
                            Log.e("NAUTAIM", "Usuarios: " + cursorMsg.getString(1));
                            Log.e("NAUTAIM", "Ahora: " + cursorMsg.getString(1).replace(fromEmail, "").trim());
                            Log.e("NAUTAIM", "Id del grupo: " + getUserIdFromMail(cursorMsg.getString(1)));
                            cvX.put("email", cursorMsg.getString(1).replace(fromEmail, "").trim());
                            db.update("usuarios", cvX, "_id = ?", new String[]{getUserIdFromMail(cursorMsg.getString(1))});
                        }

                    }
                } else {
                    ContentValues cv = new ContentValues();
                    cv.put("state", "error");
                    db.update("mensajes", cv, "state LIKE ?", new String[]{"sending"});
                    sendBroadcast(new Intent("updateConversations"));
                    return (error);
                }
            }
            c.close();
            return "1";


        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        protected void onPostExecute(String result) {
            stopService(new Intent(SendEmailServce.this, SendMailHelperService.class));
            if (result != null) {
                if (result.equals("1")) {
                    Cursor c = db.rawQuery("SELECT * FROM cola", null);
                    if (c.getCount() > 0) {
                        sendBroadcast(new Intent("updateConversations"));
                        startService(new Intent(SendEmailServce.this, SendMailHelperService.class));
                        stopSelf();
                    } else {
                        try {
                            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            Uri sound = Uri.parse(getPrefValue("sound_success", uri.getPath()));
                            MediaPlayer mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                            mediaPlayer.setDataSource(getApplicationContext(), sound);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                @Override
                                public boolean onError(MediaPlayer mp, int what, int extra) {
                                    return false;
                                }
                            });
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mp.release();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        sendBroadcast(new Intent("updateConversations"));
                        stopSelf();
                    }
                } else {
                    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Uri sound = Uri.parse(getPrefValue("sound_fail", uri.getPath()));
                    String error = result;
                    Notification.Builder mBuilder = new Notification.Builder(SendEmailServce.this);
                    mBuilder.setSmallIcon(android.R.drawable.ic_dialog_email)
                            .setContentTitle("Error. Toque para reintentar...")
                            .setSound(sound)
                            .setAutoCancel(true)
                            .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                            .setContentText("Deslice hacia abajo para ver el error")
                            .setSubText(error);
                    Intent intent = new Intent(SendEmailServce.this, SendMailHelperService.class);
                    PendingIntent pendingIntent = PendingIntent.getService(SendEmailServce.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(pendingIntent);
                    mBuilder.setAutoCancel(true);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    Notification.BigTextStyle big = new Notification.BigTextStyle(mBuilder);
                    big.bigText(error)
                            .setBigContentTitle("Error:");
                    notificationManager.notify(78, big.build());
                    sendBroadcast(new Intent("updateConversations"));
                    stopForeground(true);
                    stopSelf();
                }
            } else {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Uri sound = Uri.parse(getPrefValue("sound_fail", uri.getPath()));
                Notification.Builder mBuilder = new Notification.Builder(SendEmailServce.this);
                mBuilder.setSmallIcon(android.R.drawable.ic_dialog_email)
                        .setContentTitle("Error. Toque para reintentar...")
                        .setSound(sound)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                        .setContentText("Deslice hacia abajo para ver el error")
                        .setSubText("Error no especificado, casi siempre causado por una mala estabilidad en la conexión de red.");
                Intent intent = new Intent(SendEmailServce.this, SendMailHelperService.class);
                PendingIntent pendingIntent = PendingIntent.getService(SendEmailServce.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pendingIntent);
                mBuilder.setAutoCancel(true);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification.BigTextStyle big = new Notification.BigTextStyle(mBuilder);
                big.bigText("Error no especificado, casi siempre causado por una mala estabilidad en la conexión de red.")
                        .setBigContentTitle("Error:");
                notificationManager.notify(78, big.build());
                sendBroadcast(new Intent("updateConversations"));
                stopForeground(true);
                stopSelf();
            }


        }
    }


    /////FINNNN


}
