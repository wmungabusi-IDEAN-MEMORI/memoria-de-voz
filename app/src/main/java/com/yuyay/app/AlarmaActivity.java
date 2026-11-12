package com.yuyay.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pantalla que aparece sobre el bloqueo cuando llega un recordatorio. */
public class AlarmaActivity extends Activity {
    private long id;

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        id = getIntent().getLongExtra(Alarmas.EXTRA_ID, -1);
        Recordatorio r = Almacen.buscar(this, id);
        if (r == null) {
            finish();
            return;
        }
        String cuando = getIntent().getStringExtra(Alarmas.EXTRA_CUANDO);
        if (cuando == null) cuando = Alarmas.cuando(r);

        Tema t = new Tema(this);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setGravity(Gravity.CENTER_HORIZONTAL);
        raiz.setBackgroundColor(t.fondo);
        raiz.setPadding(t.dp(28), t.dp(72), t.dp(28), t.dp(36));

        ImageView icono = new ImageView(this);
        icono.setImageResource(R.drawable.ic_yuyay);
        raiz.addView(icono, new LinearLayout.LayoutParams(t.dp(140), t.dp(140)));

        TextView etiqueta = texto("Recordatorio", 16, t.suave);
        etiqueta.setPadding(0, t.dp(26), 0, t.dp(8));
        raiz.addView(etiqueta);

        TextView nota = texto(r.texto, 30, t.tinta);
        nota.setTypeface(Typeface.DEFAULT_BOLD);
        raiz.addView(nota);

        TextView fecha = texto(cuando, 18, t.acento);
        fecha.setPadding(0, t.dp(12), 0, 0);
        LinearLayout.LayoutParams lpFecha = new LinearLayout.LayoutParams(-2, 0, 1f);
        raiz.addView(fecha, lpFecha);

        raiz.addView(boton("Escuchar otra vez", t.tenue, t.tinta, t, () ->
                startForegroundService(Alarmas.intentVoz(this, id, 1))));
        raiz.addView(boton("Posponer " + Alarmas.MINUTOS_POSPONER + " min", t.tenue, t.tinta, t, () -> {
            Alarmas.posponer(this, id);
            finish();
        }));
        raiz.addView(boton("Listo", t.acento, t.sobreAcento, t, () -> {
            Alarmas.cerrarAviso(this, id);
            finish();
        }));
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

    private Button boton(String s, int fondo, int color, Tema t, Runnable accion) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(color);
        b.setBackground(t.redondo(fondo, 16));
        b.setStateListAnimator(null);
        b.setOnClickListener(v -> accion.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, t.dp(58));
        lp.topMargin = t.dp(12);
        b.setLayoutParams(lp);
        return b;
    }
}
