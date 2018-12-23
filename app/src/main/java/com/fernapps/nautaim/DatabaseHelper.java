package com.fernapps.nautaim;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by FeRN@NDeZ on 05/04/2017.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "nautaim.db";
    private static final int SCHEMA = 3;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE usuarios (_id integer NULL PRIMARY KEY AUTOINCREMENT, nombre text NOT NULL, email text NOT NULL, fondo text NULL);");

        db.execSQL("CREATE TABLE mensajes ( _id integer NULL PRIMARY KEY AUTOINCREMENT, " +
                "user text NOT NULL, texto text NOT NULL, date text NOT NULL, " +
                "tipo text NOT NULL, state text NOT NULL, multimedia text NOT NULL, filename text NULL, remoteid text NULL);");

        db.execSQL("CREATE TABLE cola ( _id integer NULL PRIMARY KEY AUTOINCREMENT, mailto text NOT NULL, " +
                "body text NOT NULL, msgid text NOT NULL, confirmacion text NULL);");

        db.execSQL("CREATE TABLE drafts ( _id integer NULL PRIMARY KEY AUTOINCREMENT, userid text NOT NULL, " +
                "texto text NOT NULL);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE drafts ( _id integer NULL PRIMARY KEY AUTOINCREMENT, userid text NOT NULL, " +
                    "texto text NOT NULL);");
            db.execSQL("ALTER TABLE usuarios ADD fondo text NULL");
        }


    }

}

