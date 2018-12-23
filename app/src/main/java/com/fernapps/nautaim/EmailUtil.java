package com.fernapps.nautaim;

/**
 * Created by FeRN@NDeZ on 05/04/2017.
 */

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.sun.mail.smtp.SMTPTransport;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.event.ConnectionAdapter;
import javax.mail.event.ConnectionEvent;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by FeRN@NDeZ on 31/03/2017.
 */

public class EmailUtil {

    public static String[] sendEmail(Session session, String fromEmail, String toEmail, String subject, String body, final Context context, String fromName) {
        try {
            String[] spl = toEmail.trim().split("\\p{Space}");
            if (spl.length > 1) {
                body = body + toEmail.trim() + "--||--";
            }
            toEmail = toEmail.replace(fromEmail, "").trim();
            MimeMessage msg = new MimeMessage(session);
            msg.addHeader("Content-type", "text/plain; charset=UTF-8");
            msg.addHeader("Content-Transfer-Encoding", "8bit");
            msg.setFrom(new InternetAddress(fromEmail, fromName));
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            SMTPTransport t =
                    (SMTPTransport) session.getTransport("smtp");

            t.addConnectionListener(new ConnectionAdapter() {
                @Override
                public void opened(ConnectionEvent e) {
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
                    mBuilder.setSmallIcon(R.drawable.icon)
                            .setContentTitle("Enviando Correo...")
                            .setProgress(0, 0, true);
                    notificationManager.notify(9808, mBuilder.build());
                    super.opened(e);
                }

                @Override
                public void disconnected(ConnectionEvent e) {
                    super.disconnected(e);
                }

                @Override
                public void closed(ConnectionEvent e) {
                    super.closed(e);
                }
            });

            t.send(msg);

            return new String[]{"Ok", "Ok"};
        } catch (AuthenticationFailedException e) {
            e.printStackTrace();
            return new String[]{"Auth", e.getMessage()};
        } catch (AddressException e) {
            e.printStackTrace();
            return new String[]{"Err", e.getMessage()};
        } catch (MessagingException e) {
            e.printStackTrace();
            return new String[]{"Err", e.getMessage()};
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String[]{"Err", e.getMessage()};
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"Err", e.getMessage()};
        }

    }

}