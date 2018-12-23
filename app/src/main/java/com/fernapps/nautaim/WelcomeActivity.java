package com.fernapps.nautaim;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.chyrta.onboarder.OnboarderActivity;
import com.chyrta.onboarder.OnboarderPage;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends OnboarderActivity {

    List<OnboarderPage> onboarderPages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onboarderPages = new ArrayList<OnboarderPage>();
        super.onCreate(savedInstanceState);
        OnboarderPage onboarderPage1 = new OnboarderPage("Bienvenido", "Nauta IM, una app de mensajería instantánea para Cuba");
        OnboarderPage onboarderPage2 = new OnboarderPage("Con Nauta IM:", "Podrás enviar texto e imágenes, mensajes de voz y cualquier otro tipo de archivos a tus amigos...");
        OnboarderPage onboarderPage3 = new OnboarderPage("Por tu correo", "...Y lo recibirán casi al instante en sus teléfonos.");
        OnboarderPage onboarderPage4 = new OnboarderPage("Nauta IM", "Sin Intermediarios = Sin retrasos");
        OnboarderPage onboarderPage5 = new OnboarderPage("Comienza!", "Especifica todos los datos de tu correo para comenzar a usar la app.");

        onboarderPage1.setTitleColor(R.color.black);
        onboarderPage1.setDescriptionColor(R.color.white);
        onboarderPage1.setBackgroundColor(R.color.colorAccent);
        onboarderPage2.setTitleColor(R.color.black);
        onboarderPage2.setDescriptionColor(R.color.white);
        onboarderPage2.setBackgroundColor(R.color.error_stroke_color);
        onboarderPage3.setTitleColor(R.color.black);
        onboarderPage3.setDescriptionColor(R.color.white);
        onboarderPage3.setBackgroundColor(R.color.success_stroke_color);
        onboarderPage4.setTitleColor(R.color.black);
        onboarderPage4.setDescriptionColor(R.color.white);
        onboarderPage4.setBackgroundColor(R.color.red_btn_bg_color);
        onboarderPage5.setTitleColor(R.color.black);
        onboarderPage5.setDescriptionColor(R.color.white);
        onboarderPage5.setBackgroundColor(R.color.gray_btn_bg_color);

        onboarderPage1.setImageResourceId(R.drawable.icon);
        onboarderPage2.setImageResourceId(R.drawable.android_sms_two_two);
        onboarderPage3.setImageResourceId(R.drawable.android_mail_two_two);
        onboarderPage4.setImageResourceId(R.drawable.android_contacts_one);
        onboarderPage5.setImageResourceId(R.drawable.android_settings_one);


        setSkipButtonTitle("Saltar");
        setFinishButtonTitle("Terminar");
        setActiveIndicatorColor(R.color.colorPrimary);
        setInactiveIndicatorColor(R.color.colorAccent);
        shouldDarkenButtonsLayout(true);
        onboarderPages.add(onboarderPage1);
        onboarderPages.add(onboarderPage2);
        onboarderPages.add(onboarderPage3);
        onboarderPages.add(onboarderPage4);
        onboarderPages.add(onboarderPage5);
        setOnboardPagesReady(onboarderPages);

    }

    @Override
    protected void onSkipButtonPressed() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("firstTime1", "no");
        editor.apply();
        startActivity(new Intent(WelcomeActivity.this, PreferencesActivity.class));
        Toast.makeText(this, "BIENVENIDO!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onFinishButtonPressed() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("firstTime1", "no");
        editor.apply();
        startActivity(new Intent(WelcomeActivity.this, PreferencesActivity.class));
        Toast.makeText(this, "BIENVENIDO!", Toast.LENGTH_LONG).show();
        finish();
    }
}
