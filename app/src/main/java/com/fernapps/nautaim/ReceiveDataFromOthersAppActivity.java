package com.fernapps.nautaim;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ReceiveDataFromOthersAppActivity extends Activity {
    String toSend = "";
    String tipo = "";
    DatabaseHelper helpperDb;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_data_from_others_app);
        helpperDb = new DatabaseHelper(ReceiveDataFromOthersAppActivity.this);
        db = helpperDb.getWritableDatabase();
        Intent intent = getIntent();
        final String data = intent.getDataString();
        if (intent.getType().contains("image/")) {

            //intent.getDataString();
            toSend = intent.getClipData().getItemAt(0).getUri().toString();
            tipo = "image";


        } else if (intent.getType().equals("text/plain")) {
            toSend = intent.getStringExtra(Intent.EXTRA_TEXT);
            tipo = "text";
        }

        final Cursor c = db.rawQuery("SELECT * FROM usuarios", null);
        final ArrayList<String> users = new ArrayList<String>();
        if (c.getCount() > 0) {
            c.moveToFirst();
            do {
                users.add(c.getString(2));
            } while (c.moveToNext());


        }
        c.close();
        final ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<String>(ReceiveDataFromOthersAppActivity.this, android.R.layout.simple_list_item_1, users);
        final AutoCompleteTextView numberEdit = (AutoCompleteTextView) findViewById(R.id.txtEmailToSend);
        numberEdit.setAdapter(autoCompleteAdapter);
        numberEdit.setThreshold(1);
        numberEdit.setDropDownVerticalOffset(-100);
        final ImageView fab = (ImageView) findViewById(R.id.faba);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoCompleteTextView numberEditA = (AutoCompleteTextView) findViewById(R.id.txtEmailToSend);
                String textToSend = numberEditA.getText().toString();
                if (!textToSend.isEmpty()) {
                    if (userExists(textToSend)) {
                        if (tipo.equals("text")) {
                            Intent intent = new Intent(ReceiveDataFromOthersAppActivity.this, ChatActivity.class);
                            intent.putExtra("extraFromOutside", toSend);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("userid", getUserIdFromMail(textToSend));
                            startActivity(intent);
                        } else if (tipo.equals("image")) {
                            Intent intent = new Intent(ReceiveDataFromOthersAppActivity.this, SendImageActivity.class);
                            intent.putExtra("extraFromOutside", toSend);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("userid", getUserIdFromMail(textToSend));
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        new SweetAlertDialog(ReceiveDataFromOthersAppActivity.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Mhmmm")
                                .setContentText("Lo siento, pero ese email no está en tus contactos de la app. Escríbele un mensaje al usuario antes de enviarle una imagen.")
                                .show();
                    }

                }


            }
        });
    }

    public boolean userExists(String email) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + email + "'", null);
        if (c.getCount() > 0) {
            return true;
        } else {
            return false;
        }
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
                c.close();
                return "" + addNewUserToDb("Un Grupo", email);
            } else {
                c.close();
                return "" + addNewUserToDb("Usuario NautaIM", email);
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

}
