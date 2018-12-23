package com.fernapps.nautaim;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ChatActivity extends AppCompatActivity {

    public IntentFilter intentFilter;
    public boolean isRecording = false;
    String recordingFilename = "";
    DatabaseHelper helpperDb;
    SQLiteDatabase db;
    String userId;
    View vToPostDelayed;
    MediaPlayer mediaPlayer;
    public BroadcastReceiver updatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateChat();
        }
    };
    MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        vToPostDelayed = new View(this);
        intentFilter = new IntentFilter("updateConversations");
        helpperDb = new DatabaseHelper(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        db = helpperDb.getWritableDatabase();
        Intent intent = getIntent();
        userId = intent.getStringExtra("userid");
        if (userId == null) {
            finishActivity(111);
        }
        setTitle(getUsernameByMail(getUserMailFromId(userId)));
        updateChat();
        EditText txtMsgToSend = (EditText) findViewById(R.id.txtMsgToSend);

        if (intent.getStringExtra("extraFromOutside") != null) {
            txtMsgToSend.setText(intent.getStringExtra("extraFromOutside"));
        }
        registerReceiver(updatesReceiver, intentFilter);

        if (getPrefValue("alertNewLimits", "si").equals("si")) {
            setPrefValue("alertNewLimits", "no");
            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Aqui hay algo nuevo!")
                    .setContentText("Para contribuir al rendimiento, ahora puedes establecer límites en la cantidad de mensajes por conversasción" +
                            ". Por defecto el límite son 100 mensajes, puedes cambiar esto en las preferencias. Los mensajes no se borran, solo no se cargan.\n" +
                            "Toque la imagen que representa a su contacto (su letra inicial) para realizar acciones que tengan que ver con dicho usuario.")
                    .show();
        }

        if (getPrefValue("alertDrafts", "si").equals("si")) {
            setPrefValue("alertDrafts", "no");
            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Aqui hay algo nuevo!")
                    .setContentText("Ahora la app guarda el texto que ha escrito para que no pierda su mensaje si sale de la app o de la conversación. Si hay algun " +
                            "texto guardado cuando entre a la conversación correspondiente verá su texto listo para continuar escribiendo. Ahora también puedes escoger un fondo para cada conversación. Pruébalo " +
                            "en el menú -> Escoger Fondo De Conversación.")
                    .show();
        }

        if (getPrefValue("alertVoice", "si").equals("si")) {
            setPrefValue("alertVoice", "no");
            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Aqui hay algo nuevo!")
                    .setContentText("Ahora puedes enviar mensajes de voz a tus contactos. Para enviar un mensaje, mantenga presionado el botón de grabación, grabe lo que desee enviar " +
                            "y suéltelo. Por favor, asegúrese que su contacto posea al menos la versión 1.3 de la app antes de enviar un mensaje de voz. Para enviar otro tipo de archivo use el menú.")
                    .show();
        }

        if (getPrefValue("alertShortcut", "si").equals("si")) {
            setPrefValue("alertShortcut", "no");
            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Aqui hay algo nuevo!")
                    .setContentText("Ahora puedes crear un acceso directo cualquier conversación en tu escritorio. Prueba mediante el menú -> Agregar al escritorio.")
                    .show();
        }


        try {
            txtMsgToSend.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        saveDraft();
                    }
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        Cursor c = db.rawQuery("SELECT * FROM drafts WHERE userid = " + userId, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            txtMsgToSend.setText(c.getString(2));
            txtMsgToSend.setSelection(c.getString(2).length());
        }
        c.close();
        startListeningForSaveDraft();

        try {
            String backPath = getBackgroundFromId(userId);
            if (backPath != null) {
                ScrollView scrollView = (ScrollView) findViewById(R.id.scrollViewChats);
                Drawable background = Drawable.createFromPath(backPath);
                scrollView.setBackground(background);
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, "Error al establecer el fondo de la conversación.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(ChatActivity.this, permissions, 222);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(ChatActivity.this, permissions, 222);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(ChatActivity.this, permissions, 222);
        }

        mediaPlayer = new MediaPlayer();
        mediaRecorder = new MediaRecorder();

        final FloatingActionButton recordVoiceBtn = (FloatingActionButton) findViewById(R.id.imgSendVoice);
        recordVoiceBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
                    ActivityCompat.requestPermissions(ChatActivity.this, permissions, 222);
                } else {
                    try {
                        if (event.getAction() == MotionEvent.ACTION_DOWN && !isRecording) {
                            long millisActual = Calendar.getInstance().getTimeInMillis();
                            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Enviados/");
                            dir.mkdirs();
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Enviados/", millisActual + ".wav");
                            recordingFilename = file.getAbsolutePath();
                            isRecording = true;

                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                            mediaRecorder.setOutputFile(recordingFilename);
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                            mediaRecorder.setAudioChannels(1);

                            mediaRecorder.setAudioEncodingBitRate(128);
                            mediaRecorder.setAudioSamplingRate(128);
                            try {
                                mediaRecorder.prepare();
                            } catch (IOException e) {
                                Log.e("NautaIM", "prepare() failed");
                            }
                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            vibrator.vibrate(50);
                            LinearLayout recordingLayout = (LinearLayout) findViewById(R.id.recordingLayout);
                            recordingLayout.setVisibility(View.VISIBLE);
                            LinearLayout chatLayout = (LinearLayout) findViewById(R.id.chatsContainer);
                            chatLayout.setVisibility(View.INVISIBLE);
                            setTitle("Grabando audio...");
                            mediaRecorder.start();
                        } else if (event.getAction() == MotionEvent.ACTION_UP && isRecording) {
                            isRecording = false;
                            mediaRecorder.stop();
                            mediaRecorder.reset();
                            File file = new File(recordingFilename);
                            long size = file.length() / 1024;
                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            vibrator.vibrate(400);
                            LinearLayout recordingLayout = (LinearLayout) findViewById(R.id.recordingLayout);
                            recordingLayout.setVisibility(View.INVISIBLE);
                            LinearLayout chatLayout = (LinearLayout) findViewById(R.id.chatsContainer);
                            chatLayout.setVisibility(View.VISIBLE);
                            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                    .setTitleText("Nuevo audio")
                                    .setContentText("Estás seguro de querer enviar este audio, su tamaño es: " + size + " BKs")
                                    .setConfirmText("Si")
                                    .setCancelText("No")
                                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                            new File(recordingFilename).delete();
                                            sweetAlertDialog.dismissWithAnimation();
                                        }
                                    })
                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                            ContentValues cv = new ContentValues();
                                            cv.put("user", getUserMailFromId(userId));
                                            cv.put("texto", "Un Audio");
                                            cv.put("date", "" + Calendar.getInstance().getTimeInMillis());
                                            cv.put("tipo", "s");
                                            cv.put("multimedia", "audio");
                                            cv.put("filename", recordingFilename);
                                            cv.put("state", "sending");
                                            long newId = db.insert("mensajes", "user", cv);
                                            ContentValues cv2 = new ContentValues();
                                            cv2.put("mailto", getUserMailFromId(userId));
                                            cv2.put("body", "Un Audio");
                                            cv2.put("msgid", "" + newId);
                                            db.insert("cola", "body", cv2);
                                            sendBroadcast(new Intent("updateConversations"));
                                            Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                            startService(mailSendIntent);
                                            sweetAlertDialog.dismissWithAnimation();
                                        }
                                    })
                                    .show();
                            setTitle(getUsernameByMail(getUserMailFromId(userId)));
                        }
                    } catch (Exception e) {
                        new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Error al grabar audio")
                                .setContentText("Ha ocurrido un error al grabar el audio. Asegúrate de mantener el botón presionado para grabar y no soltarlo al instante. El texto del error es: " + e.getMessage())
                                .show();
                    }

                }


                return false;
            }
        });

        try {
            new BubblesManager.Builder(ChatActivity.this).build().recycle();
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

        if (isGroup() && !imInThisGroup())
        {
            findViewById(R.id.txtMsgToSend).setEnabled(false);
            findViewById(R.id.imgSendVoice).setEnabled(false);
            findViewById(R.id.imgSendMsg).setEnabled(false);
        }
    }


    @Override
    public void onBackPressed() {
        try {
            EditText txtMsgToSend = (EditText) findViewById(R.id.txtMsgToSend);
            saveDraft();
            if (!txtMsgToSend.getText().toString().isEmpty()) {
                Toast.makeText(ChatActivity.this, "Borrador Guardado!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onBackPressed();
    }

    public void startListeningForSaveDraft() {
        EditText txtMsgToSend = (EditText) findViewById(R.id.txtMsgToSend);
        txtMsgToSend.postDelayed(new Runnable() {
            @Override
            public void run() {
                saveDraft();
                startListeningForSaveDraft();
            }
        }, 5000);
    }

    public void updateUnreadCounter()
    {

    }


    public void saveDraft() {
        Cursor c = db.rawQuery("SELECT * FROM drafts WHERE userid = " + userId, null);
        EditText txtMsgToSend = (EditText) findViewById(R.id.txtMsgToSend);
        String draftText = txtMsgToSend.getText().toString();
        ContentValues cv = new ContentValues();
        cv.put("userid", userId);
        cv.put("texto", draftText);
        if (c.getCount() > 0) {
            db.update("drafts", cv, "userid = ?", new String[]{userId});
        } else {
            db.insert("drafts", "userid", cv);
        }
        c.close();
    }

    public void deleteDraft() {
        try {
            db.delete("drafts", "userid = ?", new String[]{userId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        saveDraft();
        super.onPause();
        unregisterReceiver(updatesReceiver);
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerReceiver(updatesReceiver, intentFilter);
        updateChat();
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
                c.close();
                addNewUserToDb("Un Grupo", mail);
                return "Un Grupo";
            } else {
                c.close();
                addNewUserToDb("Usuario NautaIM", mail);
                return "Usuario NautaIM";
            }
        }
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

    public void updateChat() {

        try {
            final LinearLayout container = (LinearLayout) findViewById(R.id.chatsContainer);
            container.removeAllViews();
            String userEmail = getUserMailFromId(userId);
            String limit = getPrefValue("limitMsg", "100");
            try {
                Integer.parseInt(limit);
            } catch (Exception e) {
                limit = "100";
            }

            Cursor c = db.rawQuery("SELECT * FROM mensajes WHERE user LIKE '" + userEmail + "' ORDER BY _id DESC LIMIT " + limit, null);

            if (c.getCount() > 0) {
                c.moveToLast();
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.thereIsNoMessagesLayout);
                linearLayout.setVisibility(View.INVISIBLE);
                do {

                    String nombre = getUsernameByMail(c.getString(1));
                    final String texto = c.getString(2);
                    String date = c.getString(3);
                    final String tipo = c.getString(4);
                    String state = c.getString(5);
                    String multimedia = c.getString(6);
                    LayoutInflater layoutInflater =
                            (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    final View addView;
                    if (multimedia.equals("text")) {
                        if (tipo.equals("r")) {
                            addView = layoutInflater.inflate(R.layout.mensaje_external, null);
                            TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);

                            letraInicial.setText(nombre.substring(0, 1).toUpperCase());
                        } else {
                            addView = layoutInflater.inflate(R.layout.mensaje_local, null);
                            ProgressWheel progressWheel = (ProgressWheel) addView.findViewById(R.id.sendingProgress);
                            if (state.equals("sending")) {
                                progressWheel.spin();
                                progressWheel.setVisibility(View.VISIBLE);
                            } else if (state.equals("ok")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                ImageView imgRead = (ImageView) addView.findViewById(R.id.imgInfoReceived);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);

                            } else if (state.equals("read")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                ImageView imgRead = (ImageView) addView.findViewById(R.id.imgInfoReceived);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);
                                imgRead.setVisibility(View.VISIBLE);
                            }
                        }
                    } else if (multimedia.equals("image")) {
                        if (tipo.equals("r")) {
                            addView = layoutInflater.inflate(R.layout.mensaje_external_foto, null);
                            TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                            letraInicial.setText(nombre.substring(0, 1).toUpperCase());
                        } else {
                            addView = layoutInflater.inflate(R.layout.mensaje_local_foto, null);
                            ProgressWheel progressWheel = (ProgressWheel) addView.findViewById(R.id.sendingProgress);
                            if (state.equals("sending")) {
                                progressWheel.spin();
                                progressWheel.setVisibility(View.VISIBLE);
                            } else if (state.equals("ok")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);

                            } else if (state.equals("read")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                ImageView imgRead = (ImageView) addView.findViewById(R.id.imgInfoReceived);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);
                                imgRead.setVisibility(View.VISIBLE);
                            }
                        }
                    } else if (multimedia.equals("other")) {
                        if (tipo.equals("r")) {
                            addView = layoutInflater.inflate(R.layout.mensaje_external_file, null);
                            TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                            letraInicial.setText(nombre.substring(0, 1).toUpperCase());
                        } else {
                            addView = layoutInflater.inflate(R.layout.mensaje_local_file, null);
                            ProgressWheel progressWheel = (ProgressWheel) addView.findViewById(R.id.sendingProgress);
                            if (state.equals("sending")) {
                                progressWheel.spin();
                                progressWheel.setVisibility(View.VISIBLE);
                            } else if (state.equals("ok")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);

                            } else if (state.equals("read")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                ImageView imgRead = (ImageView) addView.findViewById(R.id.imgInfoReceived);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);
                                imgRead.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        if (tipo.equals("r")) {
                            addView = layoutInflater.inflate(R.layout.mensaje_external_audio, null);
                            TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                            letraInicial.setText(nombre.substring(0, 1).toUpperCase());
                        } else {
                            addView = layoutInflater.inflate(R.layout.mensaje_local_audio, null);
                            ProgressWheel progressWheel = (ProgressWheel) addView.findViewById(R.id.sendingProgress);
                            if (state.equals("sending")) {
                                progressWheel.spin();
                                progressWheel.setVisibility(View.VISIBLE);
                            } else if (state.equals("ok")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);

                            } else if (state.equals("read")) {
                                TextView txtInfoMsg = (TextView) addView.findViewById(R.id.txtInfoMsg);
                                ImageView imgSent = (ImageView) addView.findViewById(R.id.imgInfoSent);
                                ImageView imgRead = (ImageView) addView.findViewById(R.id.imgInfoReceived);
                                txtInfoMsg.setVisibility(View.INVISIBLE);
                                imgSent.setVisibility(View.VISIBLE);
                                imgRead.setVisibility(View.VISIBLE);
                            }
                        }
                    }


                    if (multimedia.equals("text")) {
                        TextView txtDate = (TextView) addView.findViewById(R.id.txtMsgDate);
                        TextView txtMsg = (TextView) addView.findViewById(R.id.txtMsg);
                        String toAddBegin = "";
                        final RelativeLayout relativeLayout = (RelativeLayout) addView.findViewById(R.id.layoutHolderMensajeExternal);
                        DateFormat formatoFecha = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                        DateFormat formatoHora = DateFormat.getTimeInstance(DateFormat.SHORT);
                        Calendar cal = Calendar.getInstance();
                        long dateL = Long.parseLong(date);
                        cal.setTimeInMillis(dateL);
                        String fechaPriv = "";
                        Calendar utilCal = Calendar.getInstance();
                        utilCal.set(Calendar.HOUR_OF_DAY, 0);
                        utilCal.set(Calendar.MINUTE, 0);
                        long today = utilCal.getTimeInMillis();
                        long currentTime = System.currentTimeMillis();
                        if ((currentTime - 15000) < dateL && dateL < currentTime) {
                            fechaPriv = "Ahora";
                        }else if (today < dateL && dateL < currentTime) {
                            fechaPriv = "Hoy, " + formatoHora.format(cal.getTime());
                        } else {
                            fechaPriv = formatoFecha.format(cal.getTime());
                        }

                        if (state.equals("error")) {
                            toAddBegin = "ERROR (Toque para reenviar) ";
                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                    startService(mailSendIntent);
                                    vToPostDelayed.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateChat();
                                        }
                                    }, 300);
                                }
                            });

                            txtMsg.setTextColor(getResources().getColor(R.color.warning_stroke_color));
                        }
                        txtDate.setText(fechaPriv);
                        String[] splHereA = getUserMailFromId(userId).split("\\p{Space}");
                        if (splHereA.length > 1) {
                            if (tipo.equals("r")) {
                                final String idGroupUser = texto.trim().substring(0, texto.indexOf(":"));
                                String nameGroupUser = getUsernameByMail(getUserMailFromId(idGroupUser));
                                String newText = nameGroupUser + ": " + texto.trim().substring(texto.indexOf(":") + 1);
                                txtMsg.setText(newText);
                                TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                                letraInicial.setText(nameGroupUser.substring(0, 1).toUpperCase());
                                addView.findViewById(R.id.containerLetraInicial).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                                .setTitleText("Contacto")
                                                .setContentText("Que deseas hacer con este usuario? Su correo electrónico es " +
                                                        "" + getUserMailFromId(idGroupUser))
                                                .setCancelText("Editar Contacto")
                                                .setConfirmText("Escribir Privado")
                                                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                    @Override
                                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                        sweetAlertDialog.dismissWithAnimation();
                                                        final EditText input = new EditText(ChatActivity.this);
                                                        input.setHint("Nuevo Nombre");
                                                        input.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
                                                        String userMail = getUserMailFromId(idGroupUser);
                                                        String[] spl = userMail.split("\\p{Space}");
                                                        AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                                                        alert.setTitle("Cambiar Nombre");
                                                        if (spl.length > 1) {
                                                            alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este grupo. Los correos de sus participantes son: " + getUserMailFromId(idGroupUser));
                                                        } else {
                                                            alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este usuario. Su email es: " + getUserMailFromId(idGroupUser));

                                                        }
                                                        alert.setView(input);
                                                        alert.setPositiveButton("Cambiar Nombre", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                if (input.getText().length() < 1) {
                                                                    new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                                                            .setTitleText("Mmmm...")
                                                                            .setContentText("Escribe algún nombre")
                                                                            .show();
                                                                } else {
                                                                    ContentValues cv = new ContentValues();
                                                                    cv.put("nombre", input.getText().toString());
                                                                    db.update("usuarios", cv, "_id = ?", new String[]{idGroupUser});
                                                                    Snackbar.make(findViewById(R.id.imgSendMsg), "Cambiado", Snackbar.LENGTH_LONG).show();
                                                                    dialogInterface.dismiss();
                                                                    updateChat();
                                                                }


                                                            }

                                                        })
                                                                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                        dialogInterface.dismiss();
                                                                    }
                                                                });

                                                        Dialog dialog = alert.create();
                                                        dialog.show();
                                                    }
                                                })
                                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                    @Override
                                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                        sweetAlertDialog.dismissWithAnimation();
                                                        Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class);
                                                        chatIntent.putExtra("userid", idGroupUser);
                                                        startActivity(chatIntent);
                                                    }
                                                })
                                                .show();
                                    }
                                });
                            } else {
                                SharedPreferences prefff = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this);
                                txtMsg.setText(texto.trim());
                            }

                        } else {
                            txtMsg.setText(toAddBegin + texto.trim());
                        }

                        if (state.equals("unread")) {
                            Typeface tf = txtMsg.getTypeface();
                            txtMsg.setTypeface(tf, Typeface.BOLD);
                        }


                        final String msgId = c.getString(0);
                        relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                        .setTitleText("Acciones")
                                        .setContentText("Que desea hacer con este mensaje")
                                        .setConfirmText("Eliminar")
                                        .setCancelText("Copiar Texto")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {

                                                container.removeView(addView);
                                                db.delete("mensajes", "_id = ?", new String[]{msgId});
                                                db.delete("cola", "msgid = ?", new String[]{msgId});
                                                sweetAlertDialog.setTitleText("Eliminar")
                                                        .setContentText("Eliminado correctamente.")
                                                        .showCancelButton(false)
                                                        .setConfirmText("Ok")
                                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                            @Override
                                                            public void onClick(SweetAlertDialog sweetAlertDialogA) {
                                                                sweetAlertDialogA.dismissWithAnimation();
                                                            }
                                                        }).changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                                            }
                                        })
                                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                                try {
                                                    clipboardManager.setText(texto.trim());
                                                    Snackbar.make(addView, "Copiado!", Snackbar.LENGTH_LONG).show();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                sweetAlertDialog.dismissWithAnimation();
                                            }
                                        });
                                sweetAlertDialog.show();
                                return false;
                            }
                        });
                        container.addView(addView);
                    } else if (multimedia.equals("image")) {
                        TextView txtDate = (TextView) addView.findViewById(R.id.txtMsgDate);
                        final ImageView txtMsg = (ImageView) addView.findViewById(R.id.txtMsg);
                        final RelativeLayout relativeLayout = (RelativeLayout) addView.findViewById(R.id.layoutHolderMensajeExternal);
                        DateFormat formatoFecha = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                        DateFormat formatoHora = DateFormat.getTimeInstance(DateFormat.SHORT);
                        Calendar cal = Calendar.getInstance();
                        long dateL = Long.parseLong(date);
                        cal.setTimeInMillis(dateL);
                        String fechaPriv = "";
                        Calendar utilCal = Calendar.getInstance();
                        utilCal.set(Calendar.HOUR_OF_DAY, 0);
                        utilCal.set(Calendar.MINUTE, 0);
                        long today = utilCal.getTimeInMillis();
                        long currentTime = System.currentTimeMillis();
                        if ((currentTime - 15000) < dateL && dateL < currentTime) {
                            fechaPriv = "Ahora";
                        } else if (today < dateL && dateL < currentTime) {
                            fechaPriv = "Hoy, " + formatoHora.format(cal.getTime());
                        } else {
                            fechaPriv = formatoFecha.format(cal.getTime());
                        }
                        if (state.equals("error")) {
                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                    startService(mailSendIntent);
                                    vToPostDelayed.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateChat();
                                        }
                                    }, 300);
                                }
                            });
                            txtDate.setText(fechaPriv + " (ERROR. Toque para reintentar)");
                        }


                        txtDate.setText(fechaPriv);
                        File file = new File(c.getString(7));
                        if (file.exists()) {
                            try {
                                txtMsg.setImageURI(Uri.parse(c.getString(7)));
                                final String filePath = c.getString(7);
                                final String[] splHereA = getUserMailFromId(userId).split("\\p{Space}");
                                if (tipo.equals("r") && splHereA.length > 1) {
                                    final String idGroupUser = texto.trim().substring(0, texto.indexOf(":"));
                                    final String nameGroupUser = getUsernameByMail(getUserMailFromId(idGroupUser));
                                    TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                                    letraInicial.setText(nameGroupUser.substring(0, 1).toUpperCase());
                                    addView.findViewById(R.id.containerLetraInicial).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                                    .setTitleText("Contacto")
                                                    .setContentText("Que deseas hacer con este usuario? Su correo electrónico es " +
                                                            "" + getUserMailFromId(idGroupUser))
                                                    .setCancelText("Editar Contacto")
                                                    .setConfirmText("Escribir Privado")
                                                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                        @Override
                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                            sweetAlertDialog.dismissWithAnimation();
                                                            final EditText input = new EditText(ChatActivity.this);
                                                            input.setHint("Nuevo Nombre");
                                                            input.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
                                                            String userMail = getUserMailFromId(idGroupUser);
                                                            String[] spl = userMail.split("\\p{Space}");
                                                            AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                                                            alert.setTitle("Cambiar Nombre");
                                                            if (spl.length > 1) {
                                                                alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este grupo. Los correos de sus participantes son: " + getUserMailFromId(idGroupUser));
                                                            } else {
                                                                alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este usuario. Su email es: " + getUserMailFromId(idGroupUser));

                                                            }
                                                            alert.setView(input);
                                                            alert.setPositiveButton("Cambiar Nombre", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    if (input.getText().length() < 1) {
                                                                        new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                                                                .setTitleText("Mmmm...")
                                                                                .setContentText("Escribe algún nombre")
                                                                                .show();
                                                                    } else {
                                                                        ContentValues cv = new ContentValues();
                                                                        cv.put("nombre", input.getText().toString());
                                                                        db.update("usuarios", cv, "_id = ?", new String[]{idGroupUser});
                                                                        Snackbar.make(findViewById(R.id.imgSendMsg), "Cambiado", Snackbar.LENGTH_LONG).show();
                                                                        dialogInterface.dismiss();
                                                                        updateChat();
                                                                    }


                                                                }

                                                            })
                                                                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                                            dialogInterface.dismiss();
                                                                        }
                                                                    });

                                                            Dialog dialog = alert.create();
                                                            dialog.show();
                                                        }
                                                    })
                                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                        @Override
                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                            sweetAlertDialog.dismissWithAnimation();
                                                            Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class);
                                                            chatIntent.putExtra("userid", idGroupUser);
                                                            startActivity(chatIntent);
                                                        }
                                                    })
                                                    .show();
                                        }
                                    });
                                }
                                txtMsg.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent imgIntent = new Intent(ChatActivity.this, VerImagenActivity.class);
                                        imgIntent.putExtra("filepath", filePath);

                                        if (splHereA.length > 1) {
                                            if (tipo.equals("r")) {
                                                final String idGroupUser = texto.trim().substring(0, texto.indexOf(":"));
                                                final String nameGroupUser = getUsernameByMail(getUserMailFromId(idGroupUser));
                                                final String nnnn = nameGroupUser;
                                                final String newText = nnnn + ": " + texto.trim().substring(texto.indexOf(":") + 1);
                                                imgIntent.putExtra("desc", newText);

                                            } else {
                                                imgIntent.putExtra("desc", "" + texto.trim());
                                            }

                                        } else {
                                            imgIntent.putExtra("desc", "" + texto.trim());
                                        }

                                        startActivity(imgIntent);
                                    }
                                });
                            } catch (OutOfMemoryError e) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error de memoria!")
                                        .setContentText("Lo sentimos pero ha pasado algo, puedes ver las imágenes recibidas en tu almacenamiento " +
                                                "interno, en la carpeta DCIM. Si el problema persiste, puedes borrar el mensaje sin problema pues la imagen queda almacenda. Error: " + e.getMessage())
                                        .show();
                                try {
                                    txtMsg.setImageResource(R.drawable.icon);
                                } catch (Exception ex) {
                                    txtMsg.setMinimumWidth(60);
                                    txtMsg.setMinimumHeight(60);
                                }

                                e.printStackTrace();
                            } catch (Exception e) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error!")
                                        .setContentText("Lo sentimos pero ha pasado algo, puedes ver las imágenes recibidas en tu almacenamiento " +
                                                "interno, en la carpeta DCIM. Si el problema persiste, puedes borrar el mensaje sin problema pues la imagen queda almacenda. Error: " + e.getMessage())
                                        .show();
                                try {
                                    txtMsg.setImageResource(R.drawable.icon);
                                } catch (Exception ex) {
                                    txtMsg.setMinimumWidth(60);
                                    txtMsg.setMinimumHeight(60);
                                }

                                e.printStackTrace();
                            }

                        } else {
                            txtMsg.setImageResource(R.drawable.icon);
                            txtMsg.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                            .setTitleText("Sin Imagen")
                                            .setContentText("La imágen no existe, su descripción era: " + texto.trim()).show();
                                }
                            });
                        }

                        final String msgId = c.getString(0);
                        relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                        .setTitleText("Eliminar Mensaje")
                                        .setContentText("Seguro de eliminar este mensaje?")
                                        .setConfirmText("Si")
                                        .setCancelText("No")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                sweetAlertDialog.dismissWithAnimation();
                                                container.removeView(addView);
                                                db.delete("mensajes", "_id = ?", new String[]{msgId});
                                                db.delete("cola", "msgid = ?", new String[]{msgId});
                                                Snackbar.make(findViewById(R.id.imgSendMsg), "Eliminado", Snackbar.LENGTH_LONG).show();

                                            }
                                        })
                                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                sweetAlertDialog.dismissWithAnimation();
                                            }
                                        })
                                        .show();
                                return false;
                            }
                        });
                        container.addView(addView);
                    } else if (multimedia.equals("audio")) {
                        //TODO>
                        //TODO> AQUI COMIENZA EL AUDIO
                        //TODO>
                        TextView txtDate = (TextView) addView.findViewById(R.id.txtMsgDate);
                        final FloatingActionButton txtMsg = (FloatingActionButton) addView.findViewById(R.id.txtMsg);
                        final RelativeLayout relativeLayout = (RelativeLayout) addView.findViewById(R.id.layoutHolderMensajeExternal);
                        DateFormat formatoFecha = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                        DateFormat formatoHora = DateFormat.getTimeInstance(DateFormat.SHORT);
                        Calendar cal = Calendar.getInstance();
                        long dateL = Long.parseLong(date);
                        cal.setTimeInMillis(dateL);
                        String fechaPriv = "";
                        Calendar utilCal = Calendar.getInstance();
                        utilCal.set(Calendar.HOUR_OF_DAY, 0);
                        utilCal.set(Calendar.MINUTE, 0);
                        long today = utilCal.getTimeInMillis();
                        long currentTime = System.currentTimeMillis();
                        if ((currentTime - 15000) < dateL && dateL < currentTime) {
                            fechaPriv = "Ahora";
                        }else if (today < dateL && dateL < currentTime) {
                            fechaPriv = "Hoy, " + formatoHora.format(cal.getTime());
                        } else {
                            fechaPriv = formatoFecha.format(cal.getTime());
                        }
                        if (state.equals("error")) {
                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                    startService(mailSendIntent);
                                    vToPostDelayed.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateChat();
                                        }
                                    }, 300);
                                }
                            });
                            txtDate.setText(fechaPriv + " (ERROR. Toque para reintentar)");
                        }


                        txtDate.setText(fechaPriv);
                        File file = new File(c.getString(7));
                        if (file.exists()) {
                            try {
                                final String filePath = c.getString(7);
                                final String[] splHereA = getUserMailFromId(userId).split("\\p{Space}");
                                if (tipo.equals("r") && splHereA.length > 1) {
                                    final String idGroupUser = texto.trim().substring(0, texto.indexOf(":"));
                                    final String nameGroupUser = getUsernameByMail(getUserMailFromId(idGroupUser));
                                    TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                                    letraInicial.setText(nameGroupUser.substring(0, 1).toUpperCase());
                                    addView.findViewById(R.id.containerLetraInicial).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                                    .setTitleText("Contacto")
                                                    .setContentText("Que deseas hacer con este usuario? Su correo electrónico es " +
                                                            "" + getUserMailFromId(idGroupUser))
                                                    .setCancelText("Editar Contacto")
                                                    .setConfirmText("Escribir Privado")
                                                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                        @Override
                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                            sweetAlertDialog.dismissWithAnimation();
                                                            final EditText input = new EditText(ChatActivity.this);
                                                            input.setHint("Nuevo Nombre");
                                                            input.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
                                                            String userMail = getUserMailFromId(idGroupUser);
                                                            String[] spl = userMail.split("\\p{Space}");
                                                            AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                                                            alert.setTitle("Cambiar Nombre");
                                                            if (spl.length > 1) {
                                                                alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este grupo. Los correos de sus participantes son: " + getUserMailFromId(idGroupUser));
                                                            } else {
                                                                alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este usuario. Su email es: " + getUserMailFromId(idGroupUser));

                                                            }
                                                            alert.setView(input);
                                                            alert.setPositiveButton("Cambiar Nombre", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    if (input.getText().length() < 1) {
                                                                        new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                                                                .setTitleText("Mmmm...")
                                                                                .setContentText("Escribe algún nombre")
                                                                                .show();
                                                                    } else {
                                                                        ContentValues cv = new ContentValues();
                                                                        cv.put("nombre", input.getText().toString());
                                                                        db.update("usuarios", cv, "_id = ?", new String[]{idGroupUser});
                                                                        Snackbar.make(findViewById(R.id.imgSendMsg), "Cambiado", Snackbar.LENGTH_LONG).show();
                                                                        dialogInterface.dismiss();
                                                                        updateChat();
                                                                    }


                                                                }

                                                            })
                                                                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                                            dialogInterface.dismiss();
                                                                        }
                                                                    });

                                                            Dialog dialog = alert.create();
                                                            dialog.show();
                                                        }
                                                    })
                                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                        @Override
                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                            sweetAlertDialog.dismissWithAnimation();
                                                            Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class);
                                                            chatIntent.putExtra("userid", idGroupUser);
                                                            startActivity(chatIntent);
                                                        }
                                                    })
                                                    .show();
                                        }
                                    });
                                }

                                txtMsg.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            mediaPlayer.stop();
                                            mediaPlayer.reset();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(filePath));
                                            mediaPlayer.prepare();
                                            mediaPlayer.start();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            showAlert("Error", "Error al cargar el audio");
                                        }
                                    }
                                });
                                //FIN
                            } catch (OutOfMemoryError e) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error de memoria!")
                                        .setContentText("Lo sentimos pero ha pasado algo, puedes ver las imágenes recibidas en tu almacenamiento " +
                                                "interno, en la carpeta DCIM. Si el problema persiste, puedes borrar el mensaje sin problema pues la imagen queda almacenda. Error: " + e.getMessage())
                                        .show();
                                try {
                                    txtMsg.setImageResource(R.drawable.icon);
                                } catch (Exception ex) {
                                    txtMsg.setMinimumWidth(60);
                                    txtMsg.setMinimumHeight(60);
                                }

                                e.printStackTrace();
                            } catch (Exception e) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Error!")
                                        .setContentText("Lo sentimos pero ha pasado algo, puedes ver las imágenes recibidas en tu almacenamiento " +
                                                "interno, en la carpeta DCIM. Si el problema persiste, puedes borrar el mensaje sin problema pues la imagen queda almacenda. Error: " + e.getMessage())
                                        .show();
                                try {
                                    txtMsg.setImageResource(R.drawable.icon);
                                } catch (Exception ex) {
                                    txtMsg.setMinimumWidth(60);
                                    txtMsg.setMinimumHeight(60);
                                }

                                e.printStackTrace();
                            }

                        } else {
                            try {
                                txtMsg.setImageResource(R.drawable.icon);
                            } catch (Exception ex) {
                                txtMsg.setMinimumWidth(60);
                                txtMsg.setMinimumHeight(60);
                            }

                            txtMsg.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                            .setTitleText("Sin Imagen")
                                            .setContentText("La imágen no existe, su descripción era: " + texto.trim()).show();
                                }
                            });
                        }

                        final String msgId = c.getString(0);
                        relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                        .setTitleText("Eliminar Mensaje")
                                        .setContentText("Seguro de eliminar este mensaje?")
                                        .setConfirmText("Si")
                                        .setCancelText("No")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                sweetAlertDialog.dismissWithAnimation();
                                                container.removeView(addView);
                                                db.delete("mensajes", "_id = ?", new String[]{msgId});
                                                db.delete("cola", "msgid = ?", new String[]{msgId});
                                                Snackbar.make(findViewById(R.id.imgSendMsg), "Eliminado", Snackbar.LENGTH_LONG).show();

                                            }
                                        })
                                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                sweetAlertDialog.dismissWithAnimation();
                                            }
                                        })
                                        .show();
                                return false;
                            }
                        });
                        container.addView(addView);
                        //TODO>
                        //TODO> AQUI TERMINA EL AUDIO
                        //TODO>
                    } else if (multimedia.equals("other")) {
                        //TODO>
                        //TODO> AQUI COMIENZA EL ARCHIVO
                        //TODO>
                        TextView txtDate = (TextView) addView.findViewById(R.id.txtMsgDate);
                        final TextView txtFileName = (TextView) addView.findViewById(R.id.fileName);
                        final ImageView imgFile = (ImageView) addView.findViewById(R.id.imgFile);
                        final RelativeLayout relativeLayout = (RelativeLayout) addView.findViewById(R.id.layoutHolderMensajeExternal);
                        DateFormat formatoFecha = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                        DateFormat formatoHora = DateFormat.getTimeInstance(DateFormat.SHORT);
                        Calendar cal = Calendar.getInstance();
                        long dateL = Long.parseLong(date);
                        cal.setTimeInMillis(dateL);
                        String fechaPriv = "";
                        Calendar utilCal = Calendar.getInstance();
                        utilCal.set(Calendar.HOUR_OF_DAY, 0);
                        utilCal.set(Calendar.MINUTE, 0);
                        long today = utilCal.getTimeInMillis();
                        long currentTime = System.currentTimeMillis();
                        if ((currentTime - 15000) < dateL && dateL < currentTime) {
                            fechaPriv = "Ahora";
                        } else if (today < dateL && dateL < currentTime) {
                            fechaPriv = "Hoy, " + formatoHora.format(cal.getTime());
                        } else {
                            fechaPriv = formatoFecha.format(cal.getTime());
                        }
                        if (state.equals("error")) {
                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                    startService(mailSendIntent);
                                    vToPostDelayed.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateChat();
                                        }
                                    }, 300);
                                }
                            });
                            txtDate.setText(fechaPriv + " (ERROR. Toque para reintentar)");
                        }


                        txtDate.setText(fechaPriv);
                        File file = new File(c.getString(7));
                        if (file.exists()) {
                            try {
                                txtFileName.setText(c.getString(2));
                                final String[] splHereA = getUserMailFromId(userId).split("\\p{Space}");
                                if (tipo.equals("r") && splHereA.length > 1) {
                                    final String idGroupUser = texto.trim().substring(0, texto.indexOf(":"));
                                    final String nameGroupUser = getUsernameByMail(getUserMailFromId(idGroupUser));
                                    TextView letraInicial = (TextView) addView.findViewById(R.id.txtLetraInicial);
                                    letraInicial.setText(nameGroupUser.substring(0, 1).toUpperCase());
                                    addView.findViewById(R.id.containerLetraInicial).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                                    .setTitleText("Contacto")
                                                    .setContentText("Que deseas hacer con este usuario? Su correo electrónico es " +
                                                            "" + getUserMailFromId(idGroupUser))
                                                    .setCancelText("Editar Contacto")
                                                    .setConfirmText("Escribir Privado")
                                                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                        @Override
                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                            sweetAlertDialog.dismissWithAnimation();
                                                            final EditText input = new EditText(ChatActivity.this);
                                                            input.setHint("Nuevo Nombre");
                                                            input.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
                                                            String userMail = getUserMailFromId(idGroupUser);
                                                            String[] spl = userMail.split("\\p{Space}");
                                                            AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                                                            alert.setTitle("Cambiar Nombre");
                                                            if (spl.length > 1) {
                                                                alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este grupo. Los correos de sus participantes son: " + getUserMailFromId(idGroupUser));
                                                            } else {
                                                                alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este usuario. Su email es: " + getUserMailFromId(idGroupUser));

                                                            }
                                                            alert.setView(input);
                                                            alert.setPositiveButton("Cambiar Nombre", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    if (input.getText().length() < 1) {
                                                                        new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                                                                .setTitleText("Mmmm...")
                                                                                .setContentText("Escribe algún nombre")
                                                                                .show();
                                                                    } else {
                                                                        ContentValues cv = new ContentValues();
                                                                        cv.put("nombre", input.getText().toString());
                                                                        db.update("usuarios", cv, "_id = ?", new String[]{idGroupUser});
                                                                        Snackbar.make(findViewById(R.id.imgSendMsg), "Cambiado", Snackbar.LENGTH_LONG).show();
                                                                        dialogInterface.dismiss();
                                                                        updateChat();
                                                                    }


                                                                }

                                                            })
                                                                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                                            dialogInterface.dismiss();
                                                                        }
                                                                    });

                                                            Dialog dialog = alert.create();
                                                            dialog.show();
                                                        }
                                                    })
                                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                        @Override
                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                            sweetAlertDialog.dismissWithAnimation();
                                                            Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class);
                                                            chatIntent.putExtra("userid", idGroupUser);
                                                            startActivity(chatIntent);
                                                        }
                                                    })
                                                    .show();
                                        }
                                    });
                                }

                                final String fPath = c.getString(7);
                                if (tipo.equals("r")) {

                                    //if (fPath.endsWith(""))

                                    imgFile.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Log.e("NAUTAIM", "FILE: " + fPath);
                                            Uri fUri = Uri.fromFile(new File(fPath));
                                            //Log.e("NAUTAIM", "URI: " + ClipData.newUri(getContentResolver(), texto, fUri));
                                            // return FileProvider
                                            //         .getUriForFile(getContext(),
                                            //                 getContext().getApplicationContext().getPackageName() + ".provider",
                                            //                 file);
                                            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                            viewIntent.setData(fUri);
                                            PackageManager packageManager = getPackageManager();
                                            List<ResolveInfo> activities = packageManager.queryIntentActivities(viewIntent, 0);
                                            boolean isIntentSafe = activities.size() > 0;
                                            if (isIntentSafe) {
                                                startActivity(viewIntent);
                                            } else {
                                                showAlert("Sin aplicación", "No tienes ninguna app que pueda abrir este archivo");
                                            }
                                        }
                                    });
                                }
                                //FIN
                            } catch (Exception e) {

                                e.printStackTrace();
                            }

                        } else {
                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showAlert("Sin archivo", "El archivo deseado ya no existe");
                                }
                            });
                        }

                        final String msgId = c.getString(0);
                        relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.NORMAL_TYPE)
                                        .setTitleText("Eliminar Mensaje")
                                        .setContentText("Seguro de eliminar este mensaje?")
                                        .setConfirmText("Si")
                                        .setCancelText("No")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                sweetAlertDialog.dismissWithAnimation();
                                                container.removeView(addView);
                                                db.delete("mensajes", "_id = ?", new String[]{msgId});
                                                db.delete("cola", "msgid = ?", new String[]{msgId});
                                                Snackbar.make(findViewById(R.id.imgSendMsg), "Eliminado", Snackbar.LENGTH_LONG).show();

                                            }
                                        })
                                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                sweetAlertDialog.dismissWithAnimation();
                                            }
                                        })
                                        .show();
                                return false;
                            }
                        });
                        container.addView(addView);
                        //TODO>
                        //TODO> AQUI TERMINA EL ARCHIVO
                        //TODO>
                    }


                } while (c.moveToPrevious());

                String[] spl = getUserMailFromId(userId).split("\\p{Space}");
                if (spl.length == 1) {
                    try {
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this);
                        boolean confirmationsData = sharedPref.getBoolean("confirmations_data", false);
                        boolean confirmationsWifi = sharedPref.getBoolean("confirmations_wifi", false);
                        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                        if (networkInfo != null) {
                            if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && confirmationsData) {
                                Cursor cur = db.rawQuery("SELECT * FROM mensajes WHERE state LIKE 'unread' AND user LIKE '" + getUserMailFromId(userId) + "'", null);
                                if (cur.getCount() > 0) {
                                    cur.moveToFirst();
                                    do {
                                        ContentValues cv2 = new ContentValues();
                                        cv2.put("mailto", getUserMailFromId(userId));
                                        cv2.put("body", "Confirmacion de lectura");
                                        cv2.put("confirmacion", "si");
                                        cv2.put("msgid", "" + cur.getString(0));
                                        db.insert("cola", "body", cv2);
                                    }
                                    while (cur.moveToNext());
                                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                    startService(mailSendIntent);
                                }
                                cur.close();

                            } else if (networkInfo.isConnected() && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE && confirmationsWifi) {
                                Cursor cur = db.rawQuery("SELECT * FROM mensajes WHERE state LIKE 'unread' AND user LIKE '" + getUserMailFromId(userId) + "'", null);
                                if (cur.getCount() > 0) {
                                    cur.moveToFirst();
                                    do {
                                        ContentValues cv2 = new ContentValues();
                                        cv2.put("mailto", getUserMailFromId(userId));
                                        cv2.put("body", "Confirmacion de lectura");
                                        cv2.put("confirmacion", "si");
                                        cv2.put("msgid", "" + cur.getString(0));
                                        db.insert("cola", "body", cv2);
                                    }
                                    while (cur.moveToNext());
                                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                    startService(mailSendIntent);
                                }
                                cur.close();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


                ContentValues cv = new ContentValues();
                cv.put("state", "ok");
                db.update("mensajes", cv, "state LIKE ? AND user LIKE ?", new String[]{"unread", getUserMailFromId(userId)});


            } else {
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.thereIsNoMessagesLayout);
                linearLayout.setVisibility(View.VISIBLE);
            }
            c.close();
            vToPostDelayed.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ScrollView scrollView = (ScrollView) findViewById(R.id.scrollViewChats);
                    scrollView.fullScroll(View.FOCUS_DOWN);

                }
            }, 300);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateUnreadCounter();


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
       // if (!isGroup() || !imInThisGroup())
      //  {
            menu.removeItem(R.id.action_salir_grupo);
      //  }
        return true;
    }

    public boolean isGroup()
    {
        String userMail = getUserMailFromId(userId);
        String[] spl = userMail.split("\\p{Space}");
        if (spl.length > 1)
        {
            return true;
        }else
        {
            return false;
        }
    }

    public boolean imInThisGroup()
    {
        String userMail = getUserMailFromId(userId);
        if (userMail.contains(getPrefValue("username_mail", "ERROR!")))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        ArrayList<Image> images;
        if (requestCode == 3888 && resultCode == RESULT_OK && data != null) {
            try {
                images = (ArrayList<Image>) ImagePicker.getImages(data);
                String filePath = images.get(0).getPath();
                ScrollView scrollView = (ScrollView) findViewById(R.id.scrollViewChats);
                Drawable background = Drawable.createFromPath(filePath);
                scrollView.setBackground(background);
                ContentValues cv = new ContentValues();
                cv.put("fondo", filePath);
                db.update("usuarios", cv, "_id = ?", new String[]{userId});
                showAlert(getUsernameByMail(getUserMailFromId(userId)), "Fondo Cambiado!");

            } catch (Exception e) {
                showAlert("Error al cambiar el fondo", e.getMessage());
            }


            return;
        } else if (requestCode == 953 && resultCode == RESULT_OK && data != null) {

            try {
                final Uri uri = data.getData();
                BufferedInputStream origin = null;
                long millisActual = Calendar.getInstance().getTimeInMillis();
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Enviados/");
                dir.mkdirs();
                final File zipFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Enviados/", millisActual + ".zip");
                FileOutputStream dest = new FileOutputStream(zipFile);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                out.setLevel(Deflater.BEST_COMPRESSION);
                byte[] dataZip = new byte[2000];
                FileInputStream fi = new FileInputStream(new File(uri.getPath()));
                origin = new BufferedInputStream(fi, 2000);
                ZipEntry entry = new ZipEntry(new File(uri.getPath()).getName());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(dataZip, 0, 2000)) != -1) {
                    out.write(dataZip, 0, count);
                }
                origin.close();
                out.close();
                new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Seguro?")
                        .setContentText("Quieres enviar este archivo. Su tamaño, luego de comprimirse, es de " + (zipFile.length() / 1024) + " KBs")
                        .setConfirmText("Si")
                        .setCancelText("No")
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                zipFile.delete();
                                sweetAlertDialog.dismissWithAnimation();
                            }
                        })
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                ContentValues cv = new ContentValues();
                                cv.put("user", getUserMailFromId(userId));
                                cv.put("texto", new File(uri.getPath()).getName());
                                cv.put("date", "" + Calendar.getInstance().getTimeInMillis());
                                cv.put("tipo", "s");
                                cv.put("multimedia", "other");
                                cv.put("filename", zipFile.getAbsolutePath());
                                cv.put("state", "sending");
                                long newId = db.insert("mensajes", "user", cv);
                                ContentValues cv2 = new ContentValues();
                                cv2.put("mailto", getUserMailFromId(userId));
                                cv2.put("body", new File(uri.getPath()).getName());
                                cv2.put("msgid", "" + newId);
                                db.insert("cola", "body", cv2);
                                sendBroadcast(new Intent("updateConversations"));
                                Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                                startService(mailSendIntent);
                                sweetAlertDialog.dismissWithAnimation();
                            }
                        })
                        .show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error en el archivo", "Error: " + e.getMessage());
            }

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_choosebackground:
                ImagePicker.create(this)
                        .returnAfterCapture(true) // set whether camera action should return immediate result or not
                        .imageTitle("Seleccione un fondo para " + getUsernameByMail(getUserMailFromId(userId))) // image selection title
                        .single() // single mode
                        .folderMode(true)
                        .folderTitle("Carpetas")
                        .showCamera(false) // show camera or not (true by default)
                        .imageDirectory("Camera")   // captured image directory name ("Camera" folder by default)
                        .start(3888); // start image picker activity with request code
                break;
            case R.id.action_salir_grupo:
               new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                       .setTitleText("Seguro?")
                       .setContentText("Estás seguro de querer salir de este grupo? No se borrarán los mensajes actuales, pero no podrás escribir más ni leer mensjaes nuevos. Esta opción no se puede deshacer. Los otros usuarios deberán tener la versión 1.4 de la app o superior para que atiendan a la solicitud.")
                       .setCancelText("No!")
                       .setConfirmText("Pues si!")
                       .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                           @Override
                           public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.dismissWithAnimation();
                           }
                       })
                       .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                           @Override
                           public void onClick(SweetAlertDialog sweetAlertDialog) {
                               sweetAlertDialog.dismissWithAnimation();
                               if (isConnected())
                               {
                                   String username = getUserMailFromId("" + userId);
                                   addRequestAndSendEmail(username, "TWUgdm95IGRlIGVzdGUgZ3J1cG8h");
                               }
                           }
                       }).show();
                break;
            case R.id.action_create_shortcut:
                Intent createShortcutIntent = new Intent();
                Intent startThisChatIntent = new Intent(getApplicationContext(), MainActivity.class);
                startThisChatIntent.setAction(Intent.ACTION_MAIN);
                startThisChatIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startThisChatIntent.putExtra("openChat", userId);
                startThisChatIntent.setClass(getApplicationContext(), MainActivity.class);
                startThisChatIntent.setClassName(getApplicationContext(), MainActivity.class.getName());
                createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getUsernameByMail(getUserMailFromId(userId)));
                createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, startThisChatIntent);
                createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ChatActivity.this, R.drawable.icon_shortcut));
                createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                ChatActivity.this.sendBroadcast(createShortcutIntent);
                Toast.makeText(ChatActivity.this, "Creando acceso directo...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_send_file:
                Intent i = new Intent(ChatActivity.this, FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                startActivityForResult(i, 953);
                break;
            case R.id.action_pick_image:
                Intent intent = new Intent(ChatActivity.this, SendImageActivity.class);
                intent.putExtra("userid", userId);
                startActivity(intent);
                break;
            case R.id.action_bolsanauta:
                try {
                    Intent starttPlanes = getPackageManager().getLaunchIntentForPackage("com.fernapps.planesetecsa");
                    startActivity(starttPlanes);
                } catch (Exception e) {
                    new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Aplicación no instalada")
                            .setContentText("Usted no tiene la aplicacion Planes ETECSA instalada, necesaria para llevar a cabo esta operación. Como " +
                                    " alternativa, puede marcar *133# para contratar una bolsa Nauta, o *222*328# para verifcar su saldo.")
                            .show();
                }

                break;
            case R.id.action_delete_contact:
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Seguro?")
                        .setContentText("Estás seguro de querer eliminar este contacto y todos sus mensajes?")
                        .setConfirmText("Si")
                        .setCancelText("No")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                Cursor curM = db.rawQuery("SELECT * FROM mensajes WHERE user LIKE '" + getUserMailFromId(userId) + "'", null);
                                if (curM.getCount() > 0) {
                                    curM.moveToFirst();
                                    do {
                                        db.delete("cola", "msgid = ?", new String[]{curM.getString(0)});
                                    } while (curM.moveToNext());
                                }
                                curM.close();
                                db.delete("mensajes", "user LIKE ?", new String[]{getUserMailFromId(userId)});
                                db.delete("usuarios", "_id = ?", new String[]{userId});
                                updateChat();
                                sweetAlertDialog.setTitleText("Eliminar")
                                        .setContentText("Eliminado correctamente.")
                                        .showCancelButton(false)
                                        .setConfirmText("Ok")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialogA) {
                                                sweetAlertDialogA.dismissWithAnimation();
                                                finish();
                                            }
                                        }).changeAlertType(SweetAlertDialog.SUCCESS_TYPE);

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
            case R.id.action_delete_msg:
                SweetAlertDialog sweetAlertDialogB = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Seguro?")
                        .setContentText("Estás seguro de querer eliminar todos los mensajes de este usuario?")
                        .setConfirmText("Si")
                        .setCancelText("No")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                Cursor c = db.rawQuery("SELECT * FROM mensajes WHERE user LIKE '" + getUserMailFromId(userId) + "'", null);
                                if (c.getCount() > 0) {
                                    c.moveToFirst();
                                    do {
                                        Cursor curM = db.rawQuery("SELECT * FROM mensajes WHERE user LIKE '" + getUserMailFromId(userId) + "'", null);
                                        if (curM.getCount() > 0) {
                                            curM.moveToFirst();
                                            do {
                                                db.delete("cola", "msgid = ?", new String[]{curM.getString(0)});
                                            } while (curM.moveToNext());
                                        }
                                        curM.close();
                                        db.delete("mensajes", "user LIKE ?", new String[]{getUserMailFromId(userId)});
                                        sweetAlertDialog.setTitleText("Eliminar")
                                                .setContentText("Eliminado correctamente.")
                                                .showCancelButton(false)
                                                .setConfirmText("Ok")
                                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                    @Override
                                                    public void onClick(SweetAlertDialog sweetAlertDialogA) {
                                                        sweetAlertDialogA.dismissWithAnimation();
                                                        updateChat();
                                                    }
                                                }).changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                                    } while (c.moveToNext());

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
                sweetAlertDialogB.show();
                break;
            case R.id.action_edit_contact:
                final EditText input = new EditText(ChatActivity.this);
                input.setHint("Nuevo Nombre");
                input.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
                String userMail = getUserMailFromId(userId);
                String[] spl = userMail.split("\\p{Space}");
                AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                alert.setTitle("Cambiar Nombre");
                if (spl.length > 1) {
                    alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este grupo. Los correos de sus participantes son: " + getUserMailFromId(userId));
                } else {
                    alert.setMessage("Escriba el nuevo nombre con el cual quiere que se identifique a este usuario. Su email es: " + getUserMailFromId(userId));

                }
                alert.setView(input);
                alert.setPositiveButton("Cambiar Nombre", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (input.getText().length() < 1) {
                            new SweetAlertDialog(ChatActivity.this, SweetAlertDialog.WARNING_TYPE)
                                    .setTitleText("Mmmm...")
                                    .setContentText("Escribe algún nombre")
                                    .show();
                        } else {
                            ContentValues cv = new ContentValues();
                            cv.put("nombre", input.getText().toString());
                            db.update("usuarios", cv, "_id = ?", new String[]{userId});
                            Snackbar.make(findViewById(R.id.imgSendMsg), "Cambiado", Snackbar.LENGTH_LONG).show();
                            setTitle(input.getText().toString());
                            dialogInterface.dismiss();
                            updateChat();
                        }


                    }

                })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });

                Dialog dialog = alert.create();
                dialog.show();
                break;
            case R.id.action_deleteUnsent:
                SweetAlertDialog sweetAlertDialogA = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
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
                sweetAlertDialogA.show();
                break;
            case R.id.action_retrySend:
                if (isConnected()) {
                    Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
                    startService(mailSendIntent);
                }

                break;
            case R.id.action_update:
                if (isConnected()) {
                    startService(new Intent(ChatActivity.this, ReceiveMailsWithoutPush.class));
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }


    public String getPrefValue(final String key, final String defaultValue) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            return sharedPref.getString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            vToPostDelayed.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getPrefValue(key, defaultValue);
                }
            }, 900);
            return "";
        }

    }

    public void setPrefValue(final String key, final String value) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, value);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
            vToPostDelayed.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPrefValue(key, value);
                }
            }, 900);
        }
    }

    public String getUserMailFromId(String id) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE _id = " + id, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String mail = c.getString(2);
            c.close();
            return mail;
        }
        c.close();
        return "0";
    }

    public String getBackgroundFromId(String id) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE _id = " + id, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String back = c.getString(3);
            c.close();
            return back;
        }
        c.close();
        return "No";
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        EditText txtMsgToSend = (EditText) findViewById(R.id.txtMsgToSend);
        Snackbar.make(txtMsgToSend, "Sin Conexión!", Snackbar.LENGTH_LONG).show();
        return false;
    }

    public void newMsgForSending(View v) {
        final String fromEmail = getPrefValue("username_mail", "ERROR");
        final String password = getPrefValue("pass_email", "ERROR");
        if (fromEmail.equals("ERROR") || password.equals("ERROR")) {
            showAlert("Espera!", "Debes configurar los datos de tu correo en las preferencias. Sin esto no podemos enviar ni recibir mensajes.");

        } else {
            EditText txtMsgToSend = (EditText) findViewById(R.id.txtMsgToSend);
            String textoAEnviar = txtMsgToSend.getText().toString();
            if (textoAEnviar.isEmpty()) {
                Snackbar.make(txtMsgToSend, "Escribe algo para enviar", Snackbar.LENGTH_LONG).show();
            } else {
                if (isConnected()) {
                    String username = getUserMailFromId("" + userId);
                    addRequestAndSendEmail(username, textoAEnviar);
                    txtMsgToSend.setText("");
                    saveDraft();
                    deleteDraft();
                }

            }

        }


    }

    public void showAlert(String title, String texto) {
        new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText(title)
                .setContentText(texto)
                .show();
    }

    public void addRequestAndSendEmail(String mailto, String body) {
        if (body.equals("TWUgdm95IGRlIGVzdGUgZ3J1cG8h"))
        {
            ContentValues cv = new ContentValues();
            cv.put("user", mailto);
            cv.put("texto", "Saliste de este grupo y ya no podrás enviar ni recibir mensajes nuevos en esta conversación");
            cv.put("date", "" + Calendar.getInstance().getTimeInMillis());
            cv.put("tipo", "s");
            cv.put("state", "sending");
            cv.put("multimedia", "text");
            int newIdId = new Long(db.insert("mensajes", "user", cv)).intValue();
            ContentValues cv2 = new ContentValues();
            cv2.put("mailto", mailto);
            cv2.put("body", body);
            cv2.put("msgid", "" + newIdId);
            db.insert("cola", "body", cv2);
            Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
            startService(mailSendIntent);
            updateChat();
        }
        else
        {
            ContentValues cv = new ContentValues();
            cv.put("user", mailto);
            cv.put("texto", body);
            cv.put("date", "" + Calendar.getInstance().getTimeInMillis());
            cv.put("tipo", "s");
            cv.put("state", "sending");
            cv.put("multimedia", "text");
            int newIdId = new Long(db.insert("mensajes", "user", cv)).intValue();
            ContentValues cv2 = new ContentValues();
            cv2.put("mailto", mailto);
            cv2.put("body", body);
            cv2.put("msgid", "" + newIdId);
            db.insert("cola", "body", cv2);
            Intent mailSendIntent = new Intent(ChatActivity.this, SendMailHelperService.class);
            startService(mailSendIntent);
            updateChat();
        }

    }
}
