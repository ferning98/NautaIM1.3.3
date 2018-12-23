package com.fernapps.nautaim;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

public class SendConfirmationService extends Service {

    DatabaseHelper helpperDb;
    SQLiteDatabase db;
    String sendingId = "";
    String mailTo = "";

    public SendConfirmationService() {
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
        Intent prefIntent = new Intent(SendConfirmationService.this, PreferencesHelperService.class);
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


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(SendConfirmationService.this);
        mBuilder.setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Enviando, espere...")
                .setProgress(0, 0, true);
        startForeground(9808, mBuilder.build());
        new SendConfirmationService.SendEmail().execute(intent.getStringExtra("colaid"));
        return START_STICKY;
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
            //props.put("mail.smtp.host", "192.168.137.1"); //DEBUG
            props.put("mail.smtp.port", "" + port);
            props.put("mail.smtp.localhost", "localhost");
            props.put("mail.smtp.auth", "true");
            if (cifrado.equals("2")) {
                props.put("mail.smtp.socketFactory.port", port);
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
            String toEmail = c.getString(1);
            mailTo = toEmail;
            String colaId = c.getString(0);
            Cursor c2 = db.rawQuery("SELECT * FROM mensajes WHERE _id = " + c.getString(3), null);
            c2.moveToFirst();
            String msgid = c2.getString(8);
            c2.close();
            String emailBody = "Esto es una confirmación de lectura del mensaje: " + msgid;
            String asunto = "FernappsNautaChatConfirmacion:" + msgid;
            Log.e("NAUTAIM", "ASUNTO CONFIRM ENVIADO: " + asunto);
            String[] result = EmailUtil.sendEmail(session, fromEmail, toEmail, asunto, emailBody, SendConfirmationService.this, fromName);
            String tipo = result[0];
            String error = result[1];
            if (tipo != null && tipo.equals("Ok")) {
                sendBroadcast(new Intent("updateConversations"));
                db.delete("cola", "_id = ?", new String[]{colaId});
            } else {
                sendBroadcast(new Intent("updateConversations"));
                return (error);
            }

            c.close();
            return "1";


        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        protected void onPostExecute(String result) {
            if (result != null) {
                if (result.equals("1")) {
                    stopService(new Intent(SendConfirmationService.this, SendMailHelperService.class));
                    Cursor c = db.rawQuery("SELECT * FROM cola", null);
                    if (c.getCount() > 0) {
                        sendBroadcast(new Intent("updateConversations"));
                        startService(new Intent(SendConfirmationService.this, SendMailHelperService.class));
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
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(SendConfirmationService.this);
                    mBuilder.setSmallIcon(android.R.drawable.ic_dialog_email)
                            .setContentTitle("Error. Toque para reintentar...")
                            .setProgress(0, 0, true)
                            .setSound(sound)
                            .setAutoCancel(true)
                            .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                            .setContentText(error);
                    Intent intent = new Intent(SendConfirmationService.this, SendMailHelperService.class);
                    PendingIntent pendingIntent = PendingIntent.getService(SendConfirmationService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(pendingIntent);
                    mBuilder.setAutoCancel(true);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(78, mBuilder.build());
                    sendBroadcast(new Intent("updateConversations"));
                    stopForeground(true);
                    stopSelf();
                }
            } else {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Uri sound = Uri.parse(getPrefValue("sound_fail", uri.getPath()));
                String error = result;
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(SendConfirmationService.this);
                mBuilder.setSmallIcon(android.R.drawable.ic_dialog_email)
                        .setContentTitle("Error. Toque para reintentar...")
                        .setProgress(0, 0, true)
                        .setSound(sound)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                        .setContentText(error);
                Intent intent = new Intent(SendConfirmationService.this, SendMailHelperService.class);
                PendingIntent pendingIntent = PendingIntent.getService(SendConfirmationService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pendingIntent);
                mBuilder.setAutoCancel(true);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(78, mBuilder.build());
                sendBroadcast(new Intent("updateConversations"));
                stopForeground(true);
                stopSelf();
            }


        }
    }


    /////FINNNN


}
