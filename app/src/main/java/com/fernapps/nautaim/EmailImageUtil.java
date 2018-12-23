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

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by FeRN@NDeZ on 31/03/2017.
 */

public class EmailImageUtil {


    public static String[] sendEmail(Session session, String fromEmail, String toEmail, String subject, String body, final Context context, String filepath, String fromName) {
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
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();
            // Now set the actual message
            messageBodyPart.setText(body);
            // Create a multipar message
            Multipart multipart = new MimeMultipart();
            // Set text message part
            multipart.addBodyPart(messageBodyPart);
            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            String filename = filepath;
            DataSource source = new FileDataSource(filename);
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            if (filename.endsWith(".zip")) {
                messageBodyPart.setHeader("Content-Type", "application/zip");
            }

            multipart.addBodyPart(messageBodyPart);
            // Send the complete message parts
            msg.setContent(multipart);
            SMTPTransport t =
                    (SMTPTransport) session.getTransport("smtp");

            t.send(msg);
            notificationManager.cancel(6);
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