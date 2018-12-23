package com.fernapps.nautaim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.search.SubjectTerm;

public class ImapPushService extends Service {

    Properties props;
    Session session;
    IMAPStore store;
    IMAPFolder inbox;
    String errorString = "";
    BubblesManager bubblesManager;
    boolean isShowingBuuble = false;
    BubbleLayout bubbleView;

    public ImapPushService() {
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
            props.put("mail.imap.servertimeout", "180000");
            props.put("mail.imap.timeout", "180000");
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
            props.put("mail.imap.clienttimeout", "180000");
            session = Session.getInstance(props, null);
            session.setDebug(true);
            store = (IMAPStore) session.getStore("imap");
            // store = session.getStore("imap");


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

        Notification.Builder mBuilder =
                new Notification.Builder(ImapPushService.this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Nauta IM")
                        .setTicker("Recibiendo Correos")
                        .setContentText("Buscando Correos.");
        initializeBubblesManager();
        new DoCheck().execute();
        startForeground(415, mBuilder.build());
        return START_STICKY;
    }

    public String getPrefValue(String key, String defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(key, defaultValue);
    }

    public void setPrefValue(String key, String value) {
        Intent prefIntent = new Intent(ImapPushService.this, PreferencesHelperService.class);
        prefIntent.putExtra("key", key);
        prefIntent.putExtra("value", value);
        startService(prefIntent);
    }

    public void updateNotif(String newTitle, String newText, boolean showRetry) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(ImapPushService.this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(newTitle)
                        .setTicker(newText)
                        .setContentText(newText);

        if (showRetry) {
            Intent restartIntent = new Intent(ImapPushService.this, ImapPushService.class);
            PendingIntent restartPendingIntent = PendingIntent.getService(ImapPushService.this, 111, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(restartPendingIntent);
            notificationManager.notify(762, mBuilder.build());
        } else {
            notificationManager.notify(415, mBuilder.build());
        }


    }


    private void initializeBubblesManager() {
        try {
            bubblesManager = new BubblesManager.Builder(this)
                    .setTrashLayout(R.layout.bubble_trash_layout)
                    .setInitializationCallback(new OnInitializedCallback() {
                        @Override
                        public void onInitialized() {
                        }
                    })
                    .build();
            bubblesManager.initialize();
        } catch (Exception e) {
            e.printStackTrace();
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
                inbox = (IMAPFolder) store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE);
                //AQUI REVISAR LOS COREREOS
                updateNotif("Nauta IM", "Buscando Mensajes...", false);
                final SubjectTerm subjectTerm = new SubjectTerm("FernappsNautaChatMensajeNuevo");
                Message[] messages = inbox.search(subjectTerm);
                updateNotif("Nauta IM", "Procesando " + messages.length + " mensajes...", false);
                final MessageProcessor messageProcessor = new MessageProcessor(ImapPushService.this);
                for (int i = 0; i < messages.length; i++) {
                    IMAPMessage mm = (IMAPMessage) messages[i];
                    if (mm.getSubject().startsWith("FernappsNautaChatMensajeNuevo")) {
                        messageProcessor.processMessage(mm);
                        inbox.expunge();
                        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Uri sound = Uri.parse(getPrefValue("sound_newMsg", uri.getPath()));
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ImapPushService.this);
                        mBuilder.setSmallIcon(R.drawable.icon)
                                .setContentTitle("Nuevos Mensajes")
                                .setTicker("Nuevos Mensajes!")
                                .setSound(sound)
                                .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                .setContentText("Tienes mensajes en NautaIM");
                        Intent intent = new Intent(ImapPushService.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(ImapPushService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(pendingIntent);
                        mBuilder.setAutoCancel(true);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(15, mBuilder.build());
                    }
                }
                sendBroadcast(new Intent("updateConversations"));

                try {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ImapPushService.this);
                    boolean confirmationsData = sharedPref.getBoolean("confirmations_data", false);
                    boolean confirmationsWifi = sharedPref.getBoolean("confirmations_wifi", false);
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    if (networkInfo != null) {
                        if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && confirmationsData) {
                            SubjectTerm subjectTermConfirmations = new SubjectTerm("FernappsNautaChatConfirmacion:");
                            Message[] confirmations = inbox.search(subjectTermConfirmations);
                            updateNotif("Nauta IM", "Procesando " + messages.length + " mensajes...", false);
                            ConfirmationProcessor confirmationProcessor = new ConfirmationProcessor(ImapPushService.this);
                            for (int i = 0; i < confirmations.length; i++) {
                                confirmationProcessor.processMessage(confirmations[i]);
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Uri sound = Uri.parse(getPrefValue("sound_newConfirmation", uri.getPath()));
                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ImapPushService.this);
                                mBuilder.setSmallIcon(R.drawable.icon)
                                        .setContentTitle("Nuevas confirmaciones")
                                        .setTicker("Nuevas confirmaciones!")
                                        .setSound(sound)
                                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                        .setContentText("Tienes mensajes en NautaIM");
                                Intent intent = new Intent(ImapPushService.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(ImapPushService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                mBuilder.setContentIntent(pendingIntent);
                                mBuilder.setAutoCancel(true);
                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                notificationManager.notify(15, mBuilder.build());
                                sendBroadcast(new Intent("updateConversations"));
                            }
                        } else if (networkInfo.isConnected() && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE && confirmationsWifi) {
                            SubjectTerm subjectTermConfirmations = new SubjectTerm("FernappsNautaChatConfirmacion:");
                            Message[] confirmations = inbox.search(subjectTermConfirmations);
                            updateNotif("Nauta IM", "Procesando " + messages.length + " mensajes...", false);
                            ConfirmationProcessor confirmationProcessor = new ConfirmationProcessor(ImapPushService.this);
                            for (int i = 0; i < confirmations.length; i++) {
                                confirmationProcessor.processMessage(confirmations[i]);
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Uri sound = Uri.parse(getPrefValue("sound_newConfirmation", uri.getPath()));
                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ImapPushService.this);
                                mBuilder.setSmallIcon(R.drawable.icon)
                                        .setContentTitle("Nuevas confirmaciones")
                                        .setTicker("Nuevas confirmaciones!")
                                        .setSound(sound)
                                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                        .setContentText("Tienes mensajes en NautaIM");
                                Intent intent = new Intent(ImapPushService.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(ImapPushService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                mBuilder.setContentIntent(pendingIntent);
                                mBuilder.setAutoCancel(true);
                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                notificationManager.notify(15, mBuilder.build());
                                sendBroadcast(new Intent("updateConversations"));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                inbox.expunge();

                updateNotif("Nauta IM", "Push Activo...", false);

                inbox.addMessageCountListener(new MessageCountAdapter() {
                    public void messagesAdded(MessageCountEvent ev) {
                        try {
                            Uri uriAA = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            Uri soundAA = Uri.parse(getPrefValue("sound_newMsg", uriAA.getPath()));
                            Notification.Builder mBuilderA = new Notification.Builder(ImapPushService.this);
                            NotificationManager notificationManagerA = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                            Message[] msgs = ev.getMessages();
                            System.out.println("Got " + msgs.length + " new messages");
                            Message[] mensajes = inbox.search(subjectTerm, msgs);
                            if (mensajes.length > 0) {
                                updateNotif("Nauta IM", "Procesando " + mensajes.length + " mensajes...", false);
                                for (int i = 0; i < mensajes.length; i++) {
                                    messageProcessor.processMessage(mensajes[i]);
                                    inbox.expunge();
                                    sendBroadcast(new Intent("updateConversations"));
                                }
                                updateNotif("Nauta IM", "Borrando mensaje del servidor...", false);
                                inbox.expunge();
                                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Uri sound = Uri.parse(getPrefValue("sound_newMsg", uri.getPath()));
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
                                if (!isShowingBuuble) {
                                    bubbleView = (BubbleLayout) LayoutInflater.from(ImapPushService.this).inflate(R.layout.bubble_layout, null);
                                    bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener() {
                                        @Override
                                        public void onBubbleRemoved(BubbleLayout bubble) {
                                            isShowingBuuble = false;
                                        }
                                    });
                                    bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {

                                        @Override
                                        public void onBubbleClick(BubbleLayout bubble) {
                                            Intent ii = new Intent(ImapPushService.this, MainActivity.class);
                                            ii.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(ii);
                                            isShowingBuuble = false;
                                            bubblesManager.removeBubble(bubble);
                                        }
                                    });
                                    bubbleView.setShouldStickToWall(true);
                                    TextView txt = (TextView) bubbleView.findViewById(R.id.txtNum);
                                    txt.setText("" + mensajes.length);
                                    bubblesManager.addBubble(bubbleView, 0, 70);
                                    isShowingBuuble = true;
                                } else {
                                    TextView txt = (TextView) bubbleView.findViewById(R.id.txtNum);
                                    int actual = Integer.parseInt(txt.getText().toString());
                                    int now = actual + 1;
                                    txt.setText("" + now);
                                }
                                try
                                {
                                    Integer timeV = PreferenceManager.getDefaultSharedPreferences(ImapPushService.this).getInt("timeVibrate", 300);
                                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                    vibrator.vibrate(timeV.longValue());
                                }
                                catch (Exception e)
                                {
                                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                    vibrator.vibrate(300);
                                }

                                updateNotif("Nauta IM", "Push Activo...", false);
                            } else {

                                mBuilderA.setSmallIcon(R.drawable.icon)
                                        .setContentTitle("Nuevos mensajes!")
                                        .setTicker("Nuevos Mensajes!")
                                        .setSound(soundAA)
                                        .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                        .setContentText("Nuevos mensajes en tu bandeja de entrada");
                                Notification.BigTextStyle big = new Notification.BigTextStyle(mBuilderA);
                                big.bigText("Tienes mensajes en tu bandeja de entrada que no son de Nauta IM, solo te aviso ;-)")
                                        .setBigContentTitle("Mensaje de NautaIM:");
                                notificationManagerA.notify(36, mBuilderA.build());
                            }


                            try {
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ImapPushService.this);
                                boolean confirmationsData = sharedPref.getBoolean("confirmations_data", false);
                                boolean confirmationsWifi = sharedPref.getBoolean("confirmations_wifi", false);
                                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                                if (networkInfo != null) {
                                    if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && confirmationsData) {
                                        SubjectTerm subjectTermConfirmations = new SubjectTerm("FernappsNautaChatConfirmacion:");
                                        Message[] confirmations = inbox.search(subjectTermConfirmations);
                                        updateNotif("Nauta IM", "Procesando " + mensajes.length + " mensajes...", false);
                                        ConfirmationProcessor confirmationProcessor = new ConfirmationProcessor(ImapPushService.this);
                                        for (int i = 0; i < confirmations.length; i++) {
                                            confirmationProcessor.processMessage(confirmations[i]);
                                            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                            Uri sound = Uri.parse(getPrefValue("sound_newConfirmation", uri.getPath()));
                                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ImapPushService.this);
                                            mBuilder.setSmallIcon(R.drawable.icon)
                                                    .setContentTitle("Nuevas confirmaciones")
                                                    .setTicker("Nuevas confirmaciones!")
                                                    .setSound(sound)
                                                    .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                                    .setContentText("Tienes confirmaciones en NautaIM");
                                            Intent intent = new Intent(ImapPushService.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            PendingIntent pendingIntent = PendingIntent.getActivity(ImapPushService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                            mBuilder.setContentIntent(pendingIntent);
                                            mBuilder.setAutoCancel(true);
                                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                            notificationManager.notify(15, mBuilder.build());
                                        }
                                    } else if (networkInfo.isConnected() && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE && confirmationsWifi) {
                                        SubjectTerm subjectTermConfirmations = new SubjectTerm("FernappsNautaChatConfirmacion:");
                                        Message[] confirmations = inbox.search(subjectTermConfirmations);
                                        updateNotif("Nauta IM", "Procesando " + mensajes.length + " mensajes...", false);
                                        ConfirmationProcessor confirmationProcessor = new ConfirmationProcessor(ImapPushService.this);
                                        for (int i = 0; i < confirmations.length; i++) {
                                            confirmationProcessor.processMessage(confirmations[i]);
                                            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                            Uri sound = Uri.parse(getPrefValue("sound_newConfirmation", uri.getPath()));
                                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ImapPushService.this);
                                            mBuilder.setSmallIcon(R.drawable.icon)
                                                    .setContentTitle("Nuevas confirmaciones")
                                                    .setTicker("Nuevas confirmaciones!")
                                                    .setSound(sound)
                                                    .setVibrate(new long[]{0, 100, 20, 100, 20, 100})
                                                    .setContentText("Tienes confirmaciones en NautaIM");
                                            Intent intent = new Intent(ImapPushService.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            PendingIntent pendingIntent = PendingIntent.getActivity(ImapPushService.this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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


                        } catch (MessagingException mex) {
                            mex.printStackTrace();
                        } catch (Exception mex) {
                            mex.printStackTrace();
                        }

                    }
                });

                if (this.isCancelled()) {
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

                /////////////
                // Check mail once in "freq" MILLIseconds
                int freq = Integer.parseInt("300000");
                boolean supportsIdle = false;
                try {
                    if (inbox instanceof IMAPFolder) {
                        IMAPFolder f = (IMAPFolder) inbox;
                        updateNotif("Nauta IM", "Push Activo...", false);
                        f.idle();
                        supportsIdle = true;
                    }
                } catch (FolderClosedException fex) {
                    throw fex;
                } catch (MessagingException mex) {
                    supportsIdle = false;
                }


                for (; ; ) {
                    if (supportsIdle && inbox instanceof IMAPFolder) {
                        IMAPFolder f = (IMAPFolder) inbox;
                        f.idle();
                        System.out.println("IDLE done");
                    } else {
                        Thread.sleep(freq); // sleep for freq milliseconds
                        // This is to force the IMAP server to send us
                        // EXISTS notifications.
                        inbox.getMessageCount();
                    }
                }


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

            stopForeground(true);
            stopSelf();


        }

    }


    //FIN RECIBIR CORREOS
}