package com.fernapps.nautaim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SubjectTerm;

public class ReceiveMailsWithoutPush extends Service {

    Properties props;
    Session session;
    Store store;
    Folder inbox;
    String errorString = "";
    boolean isReceiving = false;

    public ReceiveMailsWithoutPush() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            String port = getPrefValue("serverInPort", "143");
            if (port.isEmpty()) {
                port = "143";
            }
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
            timeout = "" + (Integer.parseInt(timeout) * 1000);
            props = System.getProperties();
            props.put("mail.imap.servertimeout", timeout);
            props.put("mail.imap.timeout", timeout);
            props.put("mail.imap.port", "" + port);
            String cifrado = getPrefValue("pref_cifrado", "0");
            if (cifrado.equals("2")) {
                props.put("mail.imap.starttls.enable", "false");
                props.put("mail.imap.socketFactory.port", port);
                props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            } else if (cifrado.equals("1")) {
                props.put("mail.imap.starttls.enable", "true");
            } else {
                props.put("mail.imap.starttls.enable", "false");
            }
            props.put("mail.imap.partialfetch", "false");
            props.put("mail.imap.clienttimeout", timeout);
            session = Session.getInstance(props, null);
            session.setDebug(true);
            store = session.getStore("imap");

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            inbox.close(false);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            store.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!isReceiving) {
            Notification.Builder mBuilder =
                    new Notification.Builder(ReceiveMailsWithoutPush.this)
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle("Nauta IM")
                            .setTicker("Recibiendo Correos")
                            .setContentText("Buscando Correos.");
            startForeground(415, mBuilder.build());
            new DoCheck().execute();
        }

        return START_STICKY;
    }

    public String getPrefValue(String key, String defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(key, defaultValue);
    }

    public void setPrefValue(String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void updateNotif(String newTitle, String newText, boolean showRetry) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(ReceiveMailsWithoutPush.this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(newTitle)
                        .setTicker(newText)
                        .setContentText(newText);

        if (showRetry) {
            mBuilder.setAutoCancel(true);
            Intent restartIntent = new Intent(ReceiveMailsWithoutPush.this, ReceiveMailsWithoutPush.class);
            PendingIntent restartPendingIntent = PendingIntent.getService(ReceiveMailsWithoutPush.this, 111, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(restartPendingIntent);
            Notification.BigTextStyle big = new Notification.BigTextStyle(mBuilder);
            big.bigText(errorString)
                    .setBigContentTitle("Error:");
            notificationManager.notify(762, big.build());
        } else {
            notificationManager.notify(415, mBuilder.build());
        }


    }

    /////RECIBIR CORREOS


    public class DoCheck extends AsyncTask<String, String, String[]> {


        protected String[] doInBackground(String... datos) {
            final String fromEmail = getPrefValue("username_mail", "ERROR");
            final String password = getPrefValue("pass_email", "ERROR");
            try {
                updateNotif("Nauta IM", "Conectando...", false);
                String host = getPrefValue("serverIn", "181.225.231.14");
                if (host.isEmpty()) {
                    host = "181.225.231.14";
                }
                store.connect(host, fromEmail, password);
                updateNotif("Nauta IM", "Abriendo...", false);
                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE);
                //AQUI REVISAR LOS COREREOS
                updateNotif("Nauta IM", "Buscando Mensajes...", false);
                SubjectTerm subjectTerm = new SubjectTerm("FernappsNautaChatMensajeNuevo");
                Message[] messages = inbox.search(subjectTerm);
                updateNotif("Nauta IM", "Procesando " + messages.length + " mensajes...", false);
                MessageProcessor messageProcessor = new MessageProcessor(ReceiveMailsWithoutPush.this);
                for (int i = 0; i < messages.length; i++) {
                    if (messages[i].getSubject().startsWith("FernappsNautaChatMensajeNuevo")) {
                        messageProcessor.processMessage(messages[i]);
                        inbox.expunge();
                        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Uri sound = Uri.parse(getPrefValue("sound_newMsg", uri.getPath()));
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ReceiveMailsWithoutPush.this);
                        mBuilder.setSmallIcon(R.drawable.icon)
                                .setContentTitle("Nuevos Mensajes")
                                .setTicker("Nuevos Mensajes!")
                                .setSound(sound)
                                .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                .setContentText("Tienes mensajes en NautaIM");
                        Intent intent = new Intent(ReceiveMailsWithoutPush.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(ReceiveMailsWithoutPush.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(pendingIntent);
                        mBuilder.setAutoCancel(true);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(15, mBuilder.build());
                    }

                }

                try {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ReceiveMailsWithoutPush.this);
                    boolean confirmationsData = sharedPref.getBoolean("confirmations_data", false);
                    boolean confirmationsWifi = sharedPref.getBoolean("confirmations_wifi", false);
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    if (networkInfo != null) {
                        if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && confirmationsData) {
                            SubjectTerm subjectTermConfirmations = new SubjectTerm("FernappsNautaChatConfirmacion:");
                            Message[] confirmations = inbox.search(subjectTermConfirmations);
                            updateNotif("Nauta IM", "Procesando " + messages.length + " mensajes...", false);
                            ConfirmationProcessor confirmationProcessor = new ConfirmationProcessor(ReceiveMailsWithoutPush.this);
                            for (int i = 0; i < confirmations.length; i++) {
                                confirmationProcessor.processMessage(confirmations[i]);
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Uri sound = Uri.parse(getPrefValue("sound_newConfirmation", uri.getPath()));
                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ReceiveMailsWithoutPush.this);
                                mBuilder.setSmallIcon(R.drawable.icon)
                                        .setContentTitle("Nuevas confirmaciones")
                                        .setTicker("Nuevas confirmaciones!")
                                        .setSound(sound)
                                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                        .setContentText("Tienes mensajes en NautaIM");
                                Intent intent = new Intent(ReceiveMailsWithoutPush.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(ReceiveMailsWithoutPush.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                mBuilder.setContentIntent(pendingIntent);
                                mBuilder.setAutoCancel(true);
                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                notificationManager.notify(15, mBuilder.build());
                            }
                        } else if (networkInfo.isConnected() && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE && confirmationsWifi) {
                            SubjectTerm subjectTermConfirmations = new SubjectTerm("FernappsNautaChatConfirmacion:");
                            Message[] confirmations = inbox.search(subjectTermConfirmations);
                            updateNotif("Nauta IM", "Procesando " + messages.length + " mensajes...", false);
                            ConfirmationProcessor confirmationProcessor = new ConfirmationProcessor(ReceiveMailsWithoutPush.this);
                            for (int i = 0; i < confirmations.length; i++) {
                                confirmationProcessor.processMessage(confirmations[i]);
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Uri sound = Uri.parse(getPrefValue("sound_newConfirmation", uri.getPath()));
                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ReceiveMailsWithoutPush.this);
                                mBuilder.setSmallIcon(R.drawable.icon)
                                        .setContentTitle("Nuevas confirmaciones")
                                        .setTicker("Nuevas confirmaciones!")
                                        .setSound(sound)
                                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                        .setContentText("Tienes mensajes en NautaIM");
                                Intent intent = new Intent(ReceiveMailsWithoutPush.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(ReceiveMailsWithoutPush.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                mBuilder.setContentIntent(pendingIntent);
                                mBuilder.setAutoCancel(true);
                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                notificationManager.notify(15, mBuilder.build());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                inbox.close(true);
                store.close();

                //AQUI EN FIN DE REVISAR CORREOS
            } catch (NoClassDefFoundError e) {
                e.printStackTrace();
                errorString = e.getMessage();
                return new String[]{"Error", errorString};
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
                errorString = e.getMessage();
                return new String[]{"Error", errorString};
            } catch (MessagingException e) {
                e.printStackTrace();
                errorString = e.getMessage();
                return new String[]{"Error", errorString};
            } catch (NullPointerException e) {
                e.printStackTrace();
                errorString = e.getMessage();
                return new String[]{"Error", e + errorString};
            } catch (Exception e) {
                errorString = e.getMessage();
                e.printStackTrace();
                return new String[]{"Error", errorString};
            }
            return new String[]{"Ok", "Ok"};
        }


        protected void onProgressUpdate(String... data) {


        }


        @Override
        protected void onCancelled() {
            try {
                inbox.close(false);

            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                store.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        protected void onPostExecute(String[] result) {
            isReceiving = false;
            if (result[0].equals("Ok")) {
                stopForeground(true);
                stopSelf();
            } else {
                stopForeground(true);
                updateNotif("Toque para reintentar", "Deslice hacia abajo para ver el error", true);
                stopSelf();
            }


        }

    }


    //FIN RECIBIR CORREOS
}
