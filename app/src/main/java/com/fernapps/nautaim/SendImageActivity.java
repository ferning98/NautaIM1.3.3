package com.fernapps.nautaim;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.camera.CameraModule;
import com.esafirm.imagepicker.features.camera.ImmediateCameraModule;
import com.esafirm.imagepicker.features.camera.OnImageReadyListener;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SendImageActivity extends AppCompatActivity {
    private static final int RC_CODE_PICKER = 2000;
    private static final int RC_CAMERA = 3000;
    public String dbb = "364";
    String userId = "";
    DatabaseHelper helpperDb;
    SQLiteDatabase db;
    String filePath = "";
    private ArrayList<Image> images = new ArrayList<>();
    private CameraModule cameraModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        helpperDb = new DatabaseHelper(this);
        db = helpperDb.getWritableDatabase();
        if (intent.getStringExtra("userid") == null) {
            finishActivity(222);
        } else {
            userId = intent.getStringExtra("userid");
        }
        if (intent.getStringExtra("extraFromOutside") != null) {
            final ImageView imgToSendView = (ImageView) findViewById(R.id.imgToSend);
            final TextView txtInfoImg = (TextView) findViewById(R.id.txtInfoImg);
            String outsideData = intent.getStringExtra("extraFromOutside");
            filePath = new CompressImageHelper(SendImageActivity.this).compressImage(outsideData);
            File file = new File(filePath);
            long size = file.length() / 1024;
            txtInfoImg.setText("Imágen a enviar después de comprimir: " + size + " Kbs");
            imgToSendView.setImageURI(Uri.parse(filePath));
        } else {
            start();
        }
        setTitle("Imagen a " + getUsernameByMail(getUserMailFromId(userId)));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected()) {
                    final EditText txtInfoImg = (EditText) findViewById(R.id.txtDescriptionImg);
                    ContentValues cv = new ContentValues();
                    cv.put("user", getUserMailFromId(userId));
                    cv.put("texto", txtInfoImg.getText().toString());
                    cv.put("date", "" + Calendar.getInstance().getTimeInMillis());
                    cv.put("tipo", "s");
                    cv.put("multimedia", "image");
                    cv.put("filename", filePath);
                    cv.put("state", "sending");
                    long newId = db.insert("mensajes", "user", cv);
                    ContentValues cv2 = new ContentValues();
                    cv2.put("mailto", getUserMailFromId(userId));
                    cv2.put("body", txtInfoImg.getText().toString());
                    cv2.put("msgid", "" + newId);
                    db.insert("cola", "body", cv2);
                    sendBroadcast(new Intent("updateConversations"));
                    Intent mailSendIntent = new Intent(SendImageActivity.this, SendMailHelperService.class);
                    startService(mailSendIntent);
                    finish();
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public void setPrefValue(String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        final ImageView imgToSendView = (ImageView) findViewById(R.id.imgToSend);
        final CompressImageHelper compressImageHelper = new CompressImageHelper(SendImageActivity.this);
        final TextView txtInfoImg = (TextView) findViewById(R.id.txtInfoImg);
        if (requestCode == RC_CODE_PICKER && resultCode == RESULT_OK && data != null) {
            images = (ArrayList<Image>) ImagePicker.getImages(data);
            filePath = compressImageHelper.compressImage(images.get(0).getPath());
            File file = new File(filePath);
            long size = file.length() / 1024;
            txtInfoImg.setText("Imágen a enviar después de comprimir: " + size + " Kbs");
            imgToSendView.setImageURI(Uri.parse(filePath));
            return;
        } else if (requestCode == RC_CAMERA && resultCode == RESULT_OK) {
            getCameraModule().getImage(this, data, new OnImageReadyListener() {
                @Override
                public void onImageReady(List<Image> resultImages) {
                    images = (ArrayList<Image>) resultImages;
                    Log.e("NAUTAIM", images.get(0).getPath());
                    filePath = compressImageHelper.compressImage(images.get(0).getPath());
                    File file = new File(filePath);
                    long size = file.length() / 1024;
                    txtInfoImg.setText("Imágen a enviar después de comprimir: " + size + " Kbs");
                    imgToSendView.setImageURI(Uri.parse(filePath));
                }
            });
        } else {
            finish();
        }
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        TextView txtInfoImg = (TextView) findViewById(R.id.txtInfoImg);
        Snackbar.make(txtInfoImg, "Sin Conexión!", Snackbar.LENGTH_LONG).show();
        return false;
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

    public void start() {
        ImagePicker.create(this)
                .returnAfterCapture(true) // set whether camera action should return immediate result or not
                .imageTitle("Toque la imagen para seleccionarla") // image selection title
                .single() // single mode
                .folderMode(true)
                .folderTitle("Carpetas")
                .showCamera(true) // show camera or not (true by default)
                .imageDirectory("Camera")   // captured image directory name ("Camera" folder by default)
                .start(RC_CODE_PICKER); // start image picker activity with request code
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_CAMERA) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void captureImage() {
        startActivityForResult(
                getCameraModule().getCameraIntent(SendImageActivity.this), RC_CAMERA);
    }

    private ImmediateCameraModule getCameraModule() {
        if (cameraModule == null) {
            cameraModule = new ImmediateCameraModule();
        }
        return (ImmediateCameraModule) cameraModule;
    }

}
