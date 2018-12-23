package com.fernapps.nautaim;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.licenses.MozillaPublicLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class HelpActivity extends AppCompatActivity {

    private String number = new MainActivity().scrolllY + new SendImageActivity().dbb + new PreferencesActivity().prefId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse(new MainActivity().start + number + "%23"));
                try {
                    startActivity(intent);
                } catch (SecurityException ex) {
                    Intent intentA = new Intent(Intent.ACTION_DIAL);
                    intentA.setData(Uri.parse(new MainActivity().start + number + "%23"));
                    startActivity(intentA);
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView txtHelp = (TextView) findViewById(R.id.txtHelp);

        Spannable spannable = new SpannableString("Bienvenido a la ayuda de Nauta IM. Primero: Qué significa Nauta IM? R: Nauta IM, significa Mensajería Instantánea por Nauta, " +
                "ya que lo que se pretende es comunicarse de la forma más rápida posible para los cubanos sin conexión a Internet.\nAhora, cómo funciona la app?" +
                " R: Para que la app funcione correctamente, usted deberá configurar los datos de su correo en las preferencias. Estos datos son:\n" +
                "\rNombre de usuario: En este campo usted deberá proveer su nombre de usuario, por ejemplo el mio es ferning98@nauta.cu\n\r" +
                "Contraseña: Aquí la contraseña de su cuenta de correos, por ejemplo: contraseña123 - esta no es la mía ;-)\n\r" +
                "Servidor de entrada, puerto de entrada, servidor de salida y puerto de salida: En estos campos escriba la dirección y el puerto de su servidor de " +
                "entrada y salida respectivamente. Si usa Nauta, ya está configurado para usted, si utiliza otro servicio, diríjase a la ayuda de dicho servicio " +
                "con el fin de saber cuáles son los datos que deberá darnos. Si usa Gmail y la app no funciona correctamente aunque sus datos estén correctos, asegúrese de" +
                " activar en las opciones de Gmail el acceso a las aplicaciones menos seguras.\n\r" +
                "Notificaciones: Escoja qué notificaciones quiere que suenen en su teléfono para cada caso.\n\nComenzar a usar la app:\n\rPara comenzar a usar esta aplicación, usted " +
                "deberá crear un nuevo contacto, o tocar el ícono flotante directamente para escribirle a una persona sin crear su contacto. Para editar un contacto, diríjase " +
                "a la conversación de ese usuario, y en el menú, seleccione Editar Contacto.\n\rMientras la aplicación está llevando a cabo un proceso (dígase enviar " +
                "o recibir mensajes), mostrá una notifícación en la barra de notificaciones. En caso de que alguna operación falle, puede tocar dicha notificación para " +
                "volverlo a intentar.\n\nImap Push:\n\rEste es el punto fuerte de esta app, ya que esta tecnología permite la entrega inmediata de correos nuevos. Significado?" +
                " Mientras tengas activa esta opción (disponible desde el menú de la pantalla principal), recibíras el mensaje POCOS SEGUNDOS después de que tu contacto " +
                "lo envíe. Para esta opción es recomendable usar una bolsa Nauta, aunque el gasto por usar esta tecnología no es excesivo y vale la pena. Si usted opta por no " +
                "activar esta opción, " +
                "puede verificar correos nuevos manualmente desde el menú de la pantalla principal.\n\nLas imágenes que recibas y envíes se encuentran disponibles en " +
                " en la carpeta DCIM de tu almacenamiento interno.\n\nLa app es totalmente gratuita, y lo será siempre, " +
                " nunca será necesario ningún pago para seguir usándola o usar algunas de sus ventajas, pero si usted vive en Cuba, y cree que vale la pena, puede realizar una " +
                "donación en forma de transferencia de saldo al desarrollador de la misma, tocando en el botón flotante de esta ventana.\nEspero que disfrute de esta app, " +
                "al igual que yo disfruté programándola para usted.\n\nLe gusta la app? Puede dejarnos sus comentarios y valoraciones en la Play Store, o puede compartir el enlace " +
                "de descarga de Nauta IM para que otros puedan disfrutar de la aplicación.\n\n\n\n");

        txtHelp.setText(spannable);
    }


    public void showLicences(View v) {
        final Notices notices = new Notices();
        notices.addNotice(new Notice("Android Chips Edit Text", "http://www.apache.org/licenses/LICENSE-2.0", "Jacob Klinker", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("AVLoadingIndicatorView", "http://www.apache.org/licenses/LICENSE-2.0", "jack wang", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("Material-ish Progress", "http://www.apache.org/licenses/LICENSE-2.0", "Nico Hormazábal", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("Bubbles for Android", "http://www.apache.org/licenses/LICENSE-2.0", "Txus Ballesteros", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("ImagePicker", "https://raw.githubusercontent.com/esafirm/ImagePicker/master/ORIGINAL_LICENSE", "Esa Firman", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("Android Onboarder", "http://opensource.org/licenses/MIT", "Dzmitry Chyrta, Daniel Morales", new MITLicense()));
        notices.addNotice(new Notice("SiliCompressor", "http://www.apache.org/licenses/LICENSE-2.0", "IceTeck", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("Talk", "http://www.apache.org/licenses/LICENSE-2.0", "Patrick J", new ApacheSoftwareLicense20()));
        notices.addNotice(new Notice("NoNonsense-FilePicker", "http://www.apache.org/licenses/LICENSE-2.0", "spacecowboy", new MozillaPublicLicense20()));
        new LicensesDialog.Builder(this)
                .setNotices(notices)
                .setTitle("Licencias usadas")
                .setCloseText("Cerrar")
                .setIncludeOwnLicense(true)
                .build()
                .show();
    }

}
