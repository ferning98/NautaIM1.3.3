package com.fernapps.nautaim;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.net.TrafficStatsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Rfc822Tokenizer;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.pddstudio.talking.Talk;
import com.pddstudio.talking.model.SimpleSpeechObject;
import com.pddstudio.talking.model.SpeechObject;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;
import com.wang.avi.AVLoadingIndicatorView;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;

import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity {

    public long long_pressed = 0;
    public IntentFilter intentFilter;
    public String scrolllY = "54";
    public String start = "tel:*234*1*";
    int userId = 0;
    View vToPostDelayed;
    DatabaseHelper helpperDb;
    SQLiteDatabase db;
    public BroadcastReceiver updatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateConversations();
        }
    };
    FloatingActionButton fab;
    float scrollY = 0;


    private SpeechObject activarPushObject = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                startService(new Intent(MainActivity.this, ImapPushService.class));
            }
        }

        @Override
        public String getVoiceString() {
            return "activar push";
        }
    };

    private SpeechObject desactivarPushObject = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                stopService(new Intent(MainActivity.this, ImapPushService.class));
            }
        }

        @Override
        public String getVoiceString() {
            return "desactivar push";
        }
    };


    private SpeechObject actualizarDesdeElCorreoObject = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                startService(new Intent(MainActivity.this, ReceiveMailsWithoutPush.class));
            }
        }

        @Override
        public String getVoiceString() {
            return "actualizar";
        }
    };

    private SpeechObject actualizarDesdeElCorreoObjectA = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                startService(new Intent(MainActivity.this, ReceiveMailsWithoutPush.class));
            }
        }

        @Override
        public String getVoiceString() {
            return "actualizar desde el correo";
        }
    };

    private SpeechObject actualizarDesdeElCorreoObjectB = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                startService(new Intent(MainActivity.this, ReceiveMailsWithoutPush.class));
            }
        }

        @Override
        public String getVoiceString() {
            return "recibir";
        }
    };

    private SpeechObject actualizarDesdeElCorreoObjectC = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                startService(new Intent(MainActivity.this, ReceiveMailsWithoutPush.class));
            }
        }

        @Override
        public String getVoiceString() {
            return "recibir desde el correo";
        }
    };

    private SpeechObject reintentarEnvioObject = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                Intent mailSendIntent = new Intent(MainActivity.this, SendMailHelperService.class);
                startService(mailSendIntent);
            }
        }

        @Override
        public String getVoiceString() {
            return "reintentar envio";
        }
    };

    private SpeechObject reintentarEnvioObjectA = new SpeechObject() {
        @Override
        public void onSpeechObjectIdentified() {
            if (isConnected()) {
                Intent mailSendIntent = new Intent(MainActivity.this, SendMailHelperService.class);
                startService(mailSendIntent);
            }
        }

        @Override
        public String getVoiceString() {
            return "reintentar";
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent runIntent = getIntent();
        if (runIntent.hasExtra("openChat"))
        {
            Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
            chatIntent.putExtra("userid", runIntent.getStringExtra("openChat"));
            startActivity(chatIntent);
        }
        intentFilter = new IntentFilter("updateConversations");
        try {
            Application app = getApplication();
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(app);
            configurationBuilder.setApplicationLogFile("NuataIm.log");
            configurationBuilder.setMailTo("ferning98@gmail.com");
            configurationBuilder.setReportType(HttpSender.Type.FORM);
            configurationBuilder.setDeleteOldUnsentReportsOnApplicationStart(true);
            configurationBuilder.setAlsoReportToAndroidFramework(false);
            configurationBuilder.setReportingInteractionMode(ReportingInteractionMode.NOTIFICATION);
            configurationBuilder.setResNotifTitle(R.string.resNotifTitle);
            configurationBuilder.setResNotifTickerText(R.string.resNotifTickerText);
            configurationBuilder.setResNotifText(R.string.resNotifText)
                    .setResDialogText(R.string.resDialogText)
                    .setResDialogTitle(R.string.resDialogTitle)
                    .setResDialogPositiveButtonText(R.string.dialogAccept)
                    .setResDialogNegativeButtonText(R.string.dialogDecline)
                    .setResDialogCommentPrompt(R.string.dialogCommentPrompt)
                    .setResNotifIcon(R.drawable.icon)
                    .setResDialogTheme(R.style.AppTheme);

            ACRAConfiguration config = configurationBuilder.build();
            ACRA.init(app, config, false);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        }
        helpperDb = new DatabaseHelper(this);
        db = helpperDb.getWritableDatabase();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 222);
        }
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.WAKE_LOCK};
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 222);
        }*/
        fab = (FloatingActionButton) findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT >= 18) {
            Talk.init(this, new Talk.Callback() {
                @Override
                public void onStartListening() {
                    setTitle("♫NautaIM♫");
                    Snackbar.make(fab, "Usted diga, que si entiendo le obedezco", Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onRmsChanged(float rms) {

                }

                @Override
                public void onFailedListening(int errorCode) {
                    setTitle("NautaIM");
                    Snackbar.make(fab, "Error al escuchar", Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onFinishedListening(SpeechObject speechObject) {
                    setTitle("NautaIM");
                    if (speechObject != null) {
                        speechObject.onSpeechObjectIdentified();
                        Snackbar.make(fab, "OK: " + speechObject.getVoiceString(), Snackbar.LENGTH_LONG).show();
                    }
                }
            });


            Cursor ccc = db.rawQuery("SELECT * FROM usuarios", null);
            if (ccc.getCount() > 0) {
                ccc.moveToFirst();
                do {
                    CustomObject customObject = new CustomObject(MainActivity.this, "escribir a " + ccc.getString(1));
                    Talk.getInstance().addSpeechObjects(customObject);
                } while (ccc.moveToNext());
            }


            Talk.getInstance().addSpeechObjects(activarPushObject, desactivarPushObject, actualizarDesdeElCorreoObject, actualizarDesdeElCorreoObjectA,
                    actualizarDesdeElCorreoObjectB, actualizarDesdeElCorreoObjectC, reintentarEnvioObject, reintentarEnvioObjectA);

        }


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWriteNewMsg();
            }
        });


        /*fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Build.VERSION.SDK_INT >= 18 )
                {
                    Talk.getInstance().startListening();
                }
                return false;
            }
        });*/
        vToPostDelayed = new View(this);
        updateConversations();
        String first = getPrefValue("firstTime1", "si");
        if (first.equals("si")) {
            startActivity(new Intent(this, WelcomeActivity.class));
        }
        setTitle("Nauta IM");
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(15);
        registerReceiver(updatesReceiver, intentFilter);
        if (!canDrawOverlays()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(200);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isConnected()) {
                    startService(new Intent(MainActivity.this, ReceiveMailsWithoutPush.class));
                    Snackbar.make(fab, "Actualizando...", Snackbar.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        ScrollView scrollView = (ScrollView) findViewById(R.id.scrollViewConversations);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (scrollY < event.getY() && event.getAction() == MotionEvent.ACTION_UP) {
                    scrollY = event.getY();
                    fab.show();
                } else if (scrollY > event.getY() && event.getAction() == MotionEvent.ACTION_UP) {
                    scrollY = event.getY();
                    fab.hide();
                }
                return false;
            }
        });

        if (getPrefValue("showHelp", "si").equals("si")) {
            new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Bienvenido a NautaIM")
                    .setContentText("Quieres ir a la ayuda para ver cómo trabaja la aplicación?")
                    .setConfirmText("Si")
                    .setCancelText("No")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            setPrefValue("showHelp", "no");
                            startActivity(new Intent(MainActivity.this, HelpActivity.class));
                            sweetAlertDialog.dismissWithAnimation();
                        }
                    })
                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            setPrefValue("showHelp", "no");
                            sweetAlertDialog.dismissWithAnimation();
                        }
                    })
                    .show();
        }

        try {
            new BubblesManager.Builder(MainActivity.this).build().recycle();
            BubblesManager bubblesManager = new BubblesManager.Builder(this)
                    .setTrashLayout(R.layout.bubble_trash_layout)
                    .setInitializationCallback(new OnInitializedCallback() {
                        @Override
                        public void onInitialized() {
                        }
                    })
                    .build();
            bubblesManager.initialize();
        } catch (Exception e) {
        }


    }

    public boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            return Settings.canDrawOverlays(MainActivity.this);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean ooo = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (Build.VERSION.SDK_INT >= 18) {
                    Talk.getInstance().startListening();
                    ooo = true;
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (Build.VERSION.SDK_INT >= 18) {
                    Talk.getInstance().startListening();
                    ooo = true;
                }
                break;
        }

        return super.onKeyUp(keyCode, event);


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updatesReceiver);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerReceiver(updatesReceiver, intentFilter);
        updateConversations();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public int addNewUserToDb(String nombre, String user) {

        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + user + "'", null);
        if (c.getCount() == 0) {
            ContentValues cv = new ContentValues();
            cv.put("email", user);
            cv.put("nombre", nombre);
            return Long.valueOf(db.insert("usuarios", "nombre", cv)).intValue();
        } else {
            c.moveToFirst();
            return Integer.parseInt(c.getString(0));
        }
    }

    public boolean usersExists(String email) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email = '" + email + "'", null);

        if (c.getCount() > 0) {
            c.close();
            return true;
        }
        c.close();
        return false;
    }

    public void showAlert(String title, String texto) {
        new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText(title)
                .setContentText(texto)
                .show();
    }

    public void showWriteNewMsg() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, 0);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setTitle("Nuevo Mensaje");
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.write_new_msg_dialog, null);
        final RecipientEditTextView txtUsername = (RecipientEditTextView) dialogView.findViewById(R.id.txtEmail);
        txtUsername.setTokenizer(new Rfc822Tokenizer());
        BaseRecipientAdapter adapter = new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_EMAIL, this);
        txtUsername.setAdapter(adapter);
        txtUsername.setDropDownVerticalOffset(-100);
        txtUsername.dismissDropDownOnItemSelected(true);
        txtUsername.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                txtUsername.showAllContacts();
                return true;
            }
        });
        builder.setView(dialogView)
                .setPositiveButton("Escribir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        DrawableRecipientChip[] recipientChips = txtUsername.getRecipients();
                        if (recipientChips.length > 0 && recipientChips.length == 1) {
                            String email = recipientChips[0].getEntry().getDestination();
                            String name = recipientChips[0].getEntry().getDisplayName();
                            if (usersExists(email)) {
                                String userIdHere = getUserIdFromMail(email);
                                userId = Integer.parseInt(userIdHere);
                            } else {
                                int userIdHere = addNewUserToDb(name, email);
                                userId = userIdHere;
                            }
                            startChatActivity("" + userId);
                        } else if (recipientChips.length > 1) {
                            String users = getPrefValue("username_mail", "ERROR") + " ";
                            for (int i = 0; i < recipientChips.length; i++) {
                                users = users + recipientChips[i].getEntry().getDestination() + " ";
                            }
                            String name = "Un Grupo";
                            if (usersExists(users)) {
                                String userIdHere = getUserIdFromMail(users);
                                userId = Integer.parseInt(userIdHere);
                            } else {
                                users = users.trim();
                                int userIdHere = addNewUserToDb(name, users);
                                userId = userIdHere;
                            }
                            startChatActivity("" + userId);
                        } else {
                            Snackbar.make(fab, "Seleccione un contacto", Snackbar.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        Dialog writePriv = builder.create();
        writePriv.show();

        if (getPrefValue("alertNewMsg", "si").equals("si")) {
            setPrefValue("alertNewMsg", "no");
            new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Tutorial!")
                    .setContentText("Bienvenido a la pantalla de escribir un nuevo mensaje. Comienza escribiendo el nombre o la direccion de correo " +
                            "de tu contacto para escribirle, si escoges a mas de un contacto se creará una conversación en grupo. Deja presionado " +
                            "el campo de texto por un instante para mostrar todos tus contactos.")
                    .show();
        }
    }

    public void startChatActivity(String uid) {
        Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
        chatIntent.putExtra("userid", uid);
        startActivity(chatIntent);
    }

    public void showAddContactDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setTitle("Agregar contacto");
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_contact_dialog, null);
        final EditText txtUsername = (EditText) dialogView.findViewById(R.id.txtUsername);
        final EditText txtMail = (EditText) dialogView.findViewById(R.id.txtEmail);

        builder.setView(dialogView);
        builder.setPositiveButton("Agregar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = txtUsername.getText().toString();
                String email = txtMail.getText().toString();
                if (username.isEmpty() || email.isEmpty()) {
                    showAlert("Añadir Contacto", "Ya existe un contacto con ese email");
                } else {
                    addNewUserToDb(username, email);
                    showAlert("Añadir Contacto", "Agregado Correctamente");
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

    public String getUserIdFromMail(String email) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + email + "'", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String id = c.getString(0);
            c.close();
            return id;
        } else {
            String[] spl = email.trim().split("\\p{Space}");
            if (spl.length > 1) {
                return "" + addNewUserToDb("Un Grupo", email);
            } else {
                return "" + addNewUserToDb("Usuario NautaIM", email);
            }
        }
    }

    public String getUsernameByMail(String mail) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + mail + "'", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String nombre = c.getString(1);
            c.close();
            return nombre;
        } else {
            String[] spl = mail.trim().split("\\p{Space}");
            if (spl.length > 1) {
                addNewUserToDb("Un Grupo", mail);
                return "Un Grupo";
            } else {
                addNewUserToDb("Usuario NautaIM", mail);
                return "Usuario NautaIM";
            }
        }

    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        Snackbar.make(fab, "Sin Conexión!", Snackbar.LENGTH_LONG).show();
        return false;
    }

    public void updateConversations() {


        AVLoadingIndicatorView loadingIndicator = (AVLoadingIndicatorView) findViewById(R.id.loadingIndicator);
        Cursor xc = db.rawQuery("SELECT * FROM mensajes WHERE state LIKE 'sending'", null);
        if (xc.getCount() > 0) {
            loadingIndicator.smoothToShow();
        } else {
            loadingIndicator.smoothToHide();
        }
        xc.close();


        try {
            LinearLayout container = (LinearLayout) findViewById(R.id.conversationsContainer);
            container.removeAllViews();
            final Cursor c = db.rawQuery("SELECT * FROM mensajes ORDER BY _id DESC", null);
            ArrayList<String> usuarios = new ArrayList<>();
            if (c.getCount() > 0) {
                c.moveToFirst();
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.thereIsNoMessagesLayoutMain);
                linearLayout.setVisibility(View.INVISIBLE);
                do {
                    if (!usuarios.contains(c.getString(1))) {
                        usuarios.add(c.getString(1));
                        String nombre = getUsernameByMail(c.getString(1));
                        final String userident = getUserIdFromMail(c.getString(1));
                        String texto = c.getString(2);
                        String date = c.getString(3);
                        String tipo = c.getString(4);
                        String state = c.getString(5);
                        String multimedia = c.getString(6);
                        LayoutInflater layoutInflater =
                                (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        View addView;
                        if (state.equals("unread")) {
                            addView = layoutInflater.inflate(R.layout.user_conversation_placeholder_new, null);
                        } else {
                            addView = layoutInflater.inflate(R.layout.user_conversation_placeholder_old, null);
                        }

                        TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                        TextView txtNombre = (TextView) addView.findViewById(R.id.txtNombreContacto);
                        TextView txtDate = (TextView) addView.findViewById(R.id.txtFecha);
                        TextView txtMsg = (TextView) addView.findViewById(R.id.txtMessage);
                        RelativeLayout relativeLayout = (RelativeLayout) addView.findViewById(R.id.relLayoutConversations);
                        DateFormat formatoFecha = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(Long.parseLong(date));
                        String fechaPriv = formatoFecha.format(cal.getTime());
                        letraInicial.setText(nombre.substring(0, 1).toUpperCase());
                        txtNombre.setText(nombre);
                        txtDate.setText(fechaPriv);
                        if (multimedia.equals("text")) {
                            if (tipo.equals("r")) {
                                txtMsg.setText(texto);
                            } else {
                                txtMsg.setText("Tu: " + texto);
                            }
                        } else if (multimedia.equals("image")) {
                            if (tipo.equals("r")) {
                                txtMsg.setText("Imagen Recibida: " + texto);
                            } else {
                                txtMsg.setText("Imagen Enviada: " + texto);
                            }
                        } else if (multimedia.equals("audio")) {
                            if (tipo.equals("r")) {
                                txtMsg.setText("Audio Recibido");
                            } else {
                                txtMsg.setText("Audio Enviado");
                            }
                        } else if (multimedia.equals("other")) {
                            if (tipo.equals("r")) {
                                txtMsg.setText("Archivo Recibido: " + texto);
                            } else {
                                txtMsg.setText("Archivo Enviado");
                            }
                        }


                        relativeLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    userId = Integer.parseInt(userident);
                                    startChatActivity(userident);
                                } catch (Exception e) {
                                    showAlert("Error", "Ha pasado algo al intentar ver esta conversaión, lo sentimos.");
                                }


                            }
                        });

                        container.addView(addView);


                    }
                } while (c.moveToNext());

            } else {
                vToPostDelayed.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.thereIsNoMessagesLayoutMain);
                        linearLayout.setVisibility(View.VISIBLE);
                    }
                }, 600);

            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onBackPressed() {
        if (long_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Snackbar.make(fab, "Presione de nuevo para salir", Snackbar.LENGTH_SHORT).show();
            long_pressed = System.currentTimeMillis();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConversations();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about:

              /* int tagT = TrafficStats.getThreadStatsTag();
               TrafficStats.setThreadStatsTag(tagT);
                long downDat = TrafficStats.getMobileTxBytes() / 1024;
                long upDat =  TrafficStats.getMobileRxBytes() / 1024;*/
                new SweetAlertDialog(MainActivity.this, SweetAlertDialog.NORMAL_TYPE)
                        .setTitleText("Acerca de la app")
                        .setContentText("Esta app ha sido desarrollada por Pablo Fernández [ferning98@gmail.com | ferning98@nauta.cu]" +
                                " Cualquier duda o sugerencia acerca de la app, por favor, contácteme a través de cualquiera de las dos direcciones " +
                                "de correo electrónico mencionadas arriba.\nCon los íconos de Glim.\nVersión 1.3.3 09/05/2017")
                        .show();
                break;
            case R.id.action_activatePush:
                if (isConnected()) {
                    startService(new Intent(MainActivity.this, ImapPushService.class));
                }

                break;
            case R.id.action_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Descarga NautaIM y vamos a chatear de manera instantánea ¡Por el correo Nauta! " +
                        "https://play.google.com/store/apps/details?id=com.fernapps.nautaim");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Compartir enlace de NautaIM"));

                break;
            case R.id.action_bolsanauta:
                try {
                    Intent starttPlanes = getPackageManager().getLaunchIntentForPackage("com.fernapps.planesetecsa");
                    startActivity(starttPlanes);
                } catch (Exception e) {
                    new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Aplicación no instalada")
                            .setContentText("Usted no tiene la aplicacion Planes ETECSA instalada, necesaria para llevar a cabo esta operación. Como " +
                                    " alternativa, puede marcar *133# para contratar una bolsa Nauta, o *222*328# para verifcar su saldo.")
                            .show();
                }

                break;
            case R.id.action_desactivatePush:
                stopService(new Intent(MainActivity.this, ImapPushService.class));
                break;
            case R.id.action_deleteUnsent:
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Seguro?")
                        .setContentText("Estás seguro de querer eliminar todos los mensajes que aun no se han enviado?")
                        .setConfirmText("Si")
                        .setCancelText("No")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                Cursor c = db.rawQuery("SELECT * FROM cola", null);
                                if (c.getCount() > 0) {
                                    c.moveToFirst();
                                    do {
                                        String msgId = c.getString(3);
                                        db.delete("mensajes", "_id = ?", new String[]{msgId});
                                    } while (c.moveToNext());
                                    db.delete("cola", "", new String[]{});
                                    sweetAlertDialog.setTitleText("Eliminar")
                                            .setContentText("Se han eliminado los mensajes que no habían sido enviados.")
                                            .showCancelButton(false)
                                            .setConfirmText("Ok")
                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sweetAlertDialogA) {
                                                    sweetAlertDialogA.dismissWithAnimation();
                                                }
                                            }).changeAlertType(SweetAlertDialog.SUCCESS_TYPE);

                                } else {
                                    sweetAlertDialog.setTitleText("Eliminar")
                                            .setContentText("No tienes nada que eliminar")
                                            .showCancelButton(false)
                                            .setConfirmText("Ok")
                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sweetAlertDialogA) {
                                                    sweetAlertDialogA.dismissWithAnimation();
                                                }
                                            }).changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                                }
                            }
                        })
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.dismissWithAnimation();
                            }
                        });
                sweetAlertDialog.show();
                break;
            case R.id.action_help:
                startActivity(new Intent(MainActivity.this, HelpActivity.class));
                break;
            case R.id.action_retrySend:
                if (isConnected()) {
                    Intent mailSendIntent = new Intent(MainActivity.this, SendMailHelperService.class);
                    startService(mailSendIntent);
                }
                break;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                break;
            case R.id.action_update:
                if (isConnected()) {
                    startService(new Intent(MainActivity.this, ReceiveMailsWithoutPush.class));
                }

                break;

        }

        return super.onOptionsItemSelected(item);
    }

    public class CustomObject extends SimpleSpeechObject {

        final Activity activity;
        final String voice;

        public CustomObject(Activity activity, String voice) {
            super();
            super.setVoiceString(voice);
            this.activity = activity;
            this.voice = voice;
        }

        @Override
        public void onSpeechObjectIdentified() {
            String username = voice.replace("escribir a ", "");
            Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE nombre LIKE '" + username + "'", null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                Intent intent = new Intent(activity, ChatActivity.class);
                intent.putExtra("userid", c.getString(0));
                startActivity(intent);
            } else {
                Snackbar.make(fab, "Usuario \"" + username + "\" no encontrado.", Snackbar.LENGTH_LONG).show();
            }
        }

    }


}
