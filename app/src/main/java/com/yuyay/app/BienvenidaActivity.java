package com.yuyay.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Pantalla de bienvenida que aparece la primera vez que se abre Yuyay. */
public class BienvenidaActivity extends Activity {
    private static final String PREFS = "inicio";
    private static final String VISTA = "bienvenida_vista";
    private static final int NOCHE = Color.parseColor("#141E3D");
    private static final int INDIGO = Color.parseColor("#2F3E8F");
    private static final int CIRUELA = Color.parseColor("#8A3F5C");
    private static final int DORADO = Color.parseColor("#FFC94A");
    private static final int CLARO = Color.parseColor("#E6E9F5");

    private Tema t;

    static boolean vista(Context c) {
        return c.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(VISTA, false);
    }

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        t = new Tema(this);
        getWindow().setStatusBarColor(NOCHE);
        getWindow().setNavigationBarColor(CIRUELA);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setGravity(Gravity.CENTER_HORIZONTAL);
        raiz.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{NOCHE, INDIGO, CIRUELA}));
        raiz.setPadding(t.dp(28), t.dp(32), t.dp(28), t.dp(28));

        raiz.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        FrameLayout escena = new FrameLayout(this);
        View brillo = new View(this);
        brillo.setBackground(t.circulo(DORADO));
        brillo.setAlpha(0.16f);
        escena.addView(brillo, new FrameLayout.LayoutParams(t.dp(230), t.dp(230), Gravity.CENTER));
        ImageView mascota = new ImageView(this);
        mascota.setImageResource(R.drawable.ic_yuyay);
        mascota.setContentDescription("Yuyay, tu ayudante para recordar");
        escena.addView(mascota, new FrameLayout.LayoutParams(t.dp(170), t.dp(170), Gravity.CENTER));
        raiz.addView(escena, new LinearLayout.LayoutParams(t.dp(240), t.dp(240)));

        ObjectAnimator flotar = ObjectAnimator.ofFloat(mascota, View.TRANSLATION_Y, -t.dp(8), t.dp(8));
        flotar.setDuration(1800);
        flotar.setRepeatCount(ValueAnimator.INFINITE);
        flotar.setRepeatMode(ValueAnimator.REVERSE);
        flotar.start();

        TextView titulo = texto("Bienvenido\na Yuyay", 40, Color.WHITE);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setLineSpacing(0, 0.95f);
        titulo.setPadding(0, t.dp(20), 0, 0);
        raiz.addView(titulo);

        TextView lema = texto("Dímelo con tu voz. Yo lo guardo y te aviso a tiempo.", 19, CLARO);
        lema.setPadding(0, t.dp(14), 0, 0);
        raiz.addView(lema);

        TextView kichwa = texto("En kichwa, yuyay significa recordar.", 15, DORADO);
        kichwa.setPadding(0, t.dp(12), 0, 0);
        raiz.addView(kichwa);

        raiz.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        Button comenzar = new Button(this);
        comenzar.setText("Comenzar");
        comenzar.setAllCaps(false);
        comenzar.setTextSize(19);
        comenzar.setTypeface(Typeface.DEFAULT_BOLD);
        comenzar.setTextColor(NOCHE);
        comenzar.setBackground(t.redondo(DORADO, 32));
        comenzar.setStateListAnimator(null);
        comenzar.setOnClickListener(v -> empezar());
        raiz.addView(comenzar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, t.dp(62)));

        TextView pie = texto("Tus notas se guardan solo en este celular.", 14, CLARO);
        pie.setPadding(0, t.dp(14), 0, 0);
        raiz.addView(pie);

        setContentView(raiz);
    }

    private TextView texto(String s, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private void empezar() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(VISTA, true).commit();
        List<String> faltan = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            faltan.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (faltan.isEmpty()) abrir();
        else requestPermissions(faltan.toArray(new String[0]), 1);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] permisos, int[] resultados) {
        super.onRequestPermissionsResult(req, permisos, resultados);
        abrir();
    }

    private void abrir() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
