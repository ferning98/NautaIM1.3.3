package com.fernapps.nautaim;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by FeRN@NDeZ on 05/04/2017.
 */

public class MessageProcessor {
    Context context;
    DatabaseHelper helpperDb;
    SQLiteDatabase db;


    public MessageProcessor(Context c) {
        context = c;
        helpperDb = new DatabaseHelper(context);
        db = helpperDb.getWritableDatabase();
    }

    public String processMessage(Message mensaje) {
        String toAddBegin = "";
        try {
            String subject = mensaje.getSubject();
            if (subject.startsWith("FernappsNautaChatMensajeNuevo")) {
                String msgId = subject.replace("FernappsNautaChatMensajeNuevo", "");
                Date sentdate = mensaje.getReceivedDate();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(sentdate);
                long dateMillis = calendar.getTimeInMillis();
                Address[] fromss = mensaje.getFrom();
                String fromOne = fromss[0].toString();
                String fromName = fromOne;
                String fromMail = fromOne;
                if (fromOne.contains("<") && fromOne.contains(">")) {
                    int indexOfStart = fromOne.indexOf("<");
                    int indexOfEnd = fromOne.indexOf(">");
                    fromName = fromOne.substring(0, (indexOfStart - 1));
                    fromMail = fromOne.substring((indexOfStart + 1), indexOfEnd);
                }

                Address[] addresses = mensaje.getRecipients(Message.RecipientType.TO);
                int userid = addNewUserToDb(fromName, fromMail);


                if (!mensaje.isMimeType("multipart/*")) {

                    String texto = mensaje.getContent().toString();
                    int lastIndex = texto.indexOf("||--||");
                    if (addresses.length > 1) {
                        int firstIndexMails = lastIndex + 6;
                        String allMails = texto.substring(firstIndexMails, texto.indexOf("--||--"));
                        addNewUserToDb("Un Grupo", allMails);
                        fromMail = allMails;
                        toAddBegin = userid + ":";
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("user", fromMail);
                    if (lastIndex >= 0) {
                        String txtToAdd = toAddBegin + texto.substring(0, lastIndex);
                        if (texto.substring(0, lastIndex).equals("TWUgdm95IGRlIGVzdGUgZ3J1cG8h"))
                        {
                            ContentValues cvX = new ContentValues();
                            int indexOfStart = fromOne.indexOf("<");
                            int indexOfEnd = fromOne.indexOf(">");
                            cvX.put("email", getUserMailFromId("" + userid).replace(fromOne.substring((indexOfStart + 1), indexOfEnd), "").trim());
                            db.update("usuarios", cvX, "_id = ?", new String[]{"" + userid});
                            cv.put("texto", toAddBegin + "Ha salido de este grupo y no participará más de esta conversación.");
                        }
                        else
                        {
                            cv.put("texto", txtToAdd);
                        }
                    } else {
                        cv.put("texto", toAddBegin + texto);
                    }
                    cv.put("date", "" + dateMillis);
                    cv.put("tipo", "r");
                    cv.put("state", "unread");
                    cv.put("multimedia", "text");
                    cv.put("remoteid", msgId);
                    db.insert("mensajes", "user", cv);
                    mensaje.setFlag(Flags.Flag.DELETED, true);
                    context.sendBroadcast(new Intent("updateConversations"));
                } else {
                    Multipart multi = (Multipart) mensaje.getContent();
                    String texto = multi.getBodyPart(0).getContent().toString();
                    Part multimediaPart = multi.getBodyPart(1);
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    Notification.Builder mBuilderImg = new Notification.Builder(context);
                    if (multimediaPart.isMimeType("image/*")) {
                        long millisActual = Calendar.getInstance().getTimeInMillis();
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Recibidos/");
                        dir.mkdirs();
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Recibidos/", millisActual + ".jpg");
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        InputStream imagen = multimediaPart.getInputStream();
                        byte[] bytes = new byte[1000];
                        int leidos = 0;
                        int totalImg = multimediaPart.getSize();
                        int totalLeidos = 0;
                        mBuilderImg.setContentTitle("Descargando Imagen...");
                        mBuilderImg.setSmallIcon(R.drawable.icon);
                        mBuilderImg.setProgress(100, 0, false);
                        notificationManager.notify(75, mBuilderImg.build());
                        while ((leidos = imagen.read(bytes)) > 0) {
                            totalLeidos = totalLeidos + leidos;
                            int aImg = (totalLeidos * 100) / totalImg;
                            fileOutputStream.write(bytes, 0, leidos);
                            mBuilderImg.setProgress(100, aImg, false);
                            notificationManager.notify(75, mBuilderImg.build());
                        }
                        notificationManager.cancel(75);
                        int lastIndex = texto.indexOf("||--||");
                        notificationManager.cancel(7);
                        ContentValues cv = new ContentValues();
                        if (addresses.length > 1) {
                            int firstIndexMails = lastIndex + 6;
                            String allMails = texto.substring(firstIndexMails, texto.indexOf("--||--"));
                            addNewUserToDb("Un Grupo", allMails);
                            fromMail = allMails;
                            toAddBegin = userid + ":";
                        }
                        cv.put("user", fromMail);
                        if (lastIndex >= 0) {
                            cv.put("texto", toAddBegin + texto.substring(0, lastIndex));
                        } else {
                            cv.put("texto", toAddBegin + texto);
                        }
                        cv.put("date", "" + dateMillis);
                        cv.put("tipo", "r");
                        cv.put("state", "unread");
                        cv.put("multimedia", "image");
                        cv.put("remoteid", msgId);
                        cv.put("filename", file.getAbsolutePath());
                        db.insert("mensajes", "user", cv);
                        mensaje.setFlag(Flags.Flag.DELETED, true);
                    } else if (multimediaPart.isMimeType("audio/*")) {
                        long millisActual = Calendar.getInstance().getTimeInMillis();
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Recibidos/");
                        dir.mkdirs();
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Recibidos/", millisActual + ".wav");
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        InputStream imagen = multimediaPart.getInputStream();

                        byte[] bytes = new byte[1000];
                        int leidos = 0;
                        int totalImg = multimediaPart.getSize();
                        int totalLeidos = 0;
                        mBuilderImg.setContentTitle("Descargando Audio...");
                        mBuilderImg.setSmallIcon(R.drawable.icon);
                        mBuilderImg.setProgress(100, 0, false);
                        notificationManager.notify(75, mBuilderImg.build());
                        while ((leidos = imagen.read(bytes)) > 0) {
                            totalLeidos = totalLeidos + leidos;
                            int aImg = (totalLeidos * 100) / totalImg;
                            fileOutputStream.write(bytes, 0, leidos);
                            mBuilderImg.setProgress(100, aImg, false);
                            notificationManager.notify(75, mBuilderImg.build());
                        }
                        notificationManager.cancel(75);
                        int lastIndex = texto.indexOf("||--||");
                        notificationManager.cancel(7);
                        ContentValues cv = new ContentValues();
                        if (addresses.length > 1) {
                            int firstIndexMails = lastIndex + 6;
                            String allMails = texto.substring(firstIndexMails, texto.indexOf("--||--"));
                            addNewUserToDb("Un Grupo", allMails);
                            fromMail = allMails;
                            toAddBegin = userid + ":";
                        }
                        cv.put("user", fromMail);
                        if (lastIndex >= 0) {
                            cv.put("texto", toAddBegin + texto.substring(0, lastIndex));
                        } else {
                            cv.put("texto", toAddBegin + texto);
                        }
                        cv.put("date", "" + dateMillis);
                        cv.put("tipo", "r");
                        cv.put("state", "unread");
                        cv.put("multimedia", "audio");
                        cv.put("remoteid", msgId);
                        cv.put("filename", file.getAbsolutePath());
                        db.insert("mensajes", "user", cv);
                        mensaje.setFlag(Flags.Flag.DELETED, true);
                    } else if (multimediaPart.isMimeType("application/zip")) {
                        long millisActual = Calendar.getInstance().getTimeInMillis();
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Recibidos/");
                        dir.mkdirs();
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/NautaIM/Recibidos/", millisActual + ".zip");
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        InputStream imagen = multimediaPart.getInputStream();
                        byte[] bytes = new byte[1000];
                        int leidos = 0;
                        int totalImg = multimediaPart.getSize();
                        int totalLeidos = 0;
                        mBuilderImg.setContentTitle("Descargando Archivo...");
                        mBuilderImg.setSmallIcon(R.drawable.icon);
                        mBuilderImg.setProgress(100, 0, false);
                        notificationManager.notify(75, mBuilderImg.build());
                        while ((leidos = imagen.read(bytes)) > 0) {
                            totalLeidos = totalLeidos + leidos;
                            int aImg = (totalLeidos * 100) / totalImg;
                            fileOutputStream.write(bytes, 0, leidos);
                            mBuilderImg.setProgress(100, aImg, false);
                            notificationManager.notify(75, mBuilderImg.build());
                        }
                        notificationManager.cancel(75);

                        //ZIP
                        String newFileName = "";
                        FileInputStream fin = new FileInputStream(file);
                        ZipInputStream zip = new ZipInputStream(fin);
                        ZipEntry ze = null;
                        long currentTime = System.currentTimeMillis();
                        while ((ze = zip.getNextEntry()) != null) {
                            newFileName = dir.getAbsolutePath() + "/" + +currentTime + ze.getName();
                            FileOutputStream fout = new FileOutputStream(newFileName);
                            for (int c = zip.read(); c != -1; c = zip.read()) {
                                fout.write(c);
                            }
                            zip.closeEntry();
                            fout.close();
                        }
                        zip.close();

                        //FIN ZIP
                        int lastIndex = texto.indexOf("||--||");
                        notificationManager.cancel(7);
                        ContentValues cv = new ContentValues();
                        if (addresses.length > 1) {
                            int firstIndexMails = lastIndex + 6;
                            String allMails = texto.substring(firstIndexMails, texto.indexOf("--||--"));
                            addNewUserToDb("Un Grupo", allMails);
                            fromMail = allMails;
                            toAddBegin = userid + ":";
                        }
                        cv.put("user", fromMail);
                        if (lastIndex >= 0) {
                            cv.put("texto", toAddBegin + texto.substring(0, lastIndex));
                        } else {
                            cv.put("texto", toAddBegin + texto);
                        }
                        cv.put("date", "" + dateMillis);
                        cv.put("tipo", "r");
                        cv.put("state", "unread");
                        cv.put("multimedia", "other");
                        cv.put("remoteid", msgId);
                        cv.put("filename", newFileName);
                        db.insert("mensajes", "user", cv);
                        mensaje.setFlag(Flags.Flag.DELETED, true);
                    } else {
                        mensaje.setSubject("Mensaje Editado Por NautaIM");
                    }

                    context.sendBroadcast(new Intent("updateConversations"));

                }
            }


        } catch (MessagingException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return "Ok";
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

    public String getUserIdFromMail(String email, String name) {
        Cursor c = db.rawQuery("SELECT * FROM usuarios WHERE email LIKE '" + email + "'", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            String id = c.getString(0);
            c.close();
            return id;
        } else {
            ContentValues cv = new ContentValues();
            cv.put("email", email);
            cv.put("nombre", name);
            long id = db.insert("usuarios", "nombre", cv);
            return "" + id;
        }
    }

}

