package com.yuyay.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/** Colores de la app en modo claro y oscuro, y utilidades de forma. */
final class Tema {
    final int fondo, tarjeta, tinta, suave, acento, sobreAcento, tenue, alerta, linea;
    private final float densidad;

    Tema(Context c) {
        densidad = c.getResources().getDisplayMetrics().density;
        boolean oscuro = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        fondo = Color.parseColor(oscuro ? "#121829" : "#FBF6EF");
        tarjeta = Color.parseColor(oscuro ? "#1C2338" : "#FFFFFF");
        tinta = Color.parseColor(oscuro ? "#EEF0F7" : "#1B2A4E");
        suave = Color.parseColor(oscuro ? "#A6AEC4" : "#5A6378");
        acento = Color.parseColor(oscuro ? "#FFC94A" : "#2F3E8F");
        sobreAcento = Color.parseColor(oscuro ? "#1B2A4E" : "#FFFFFF");
        tenue = Color.parseColor(oscuro ? "#2A3352" : "#FFE9BD");
        alerta = Color.parseColor(oscuro ? "#FF8A70" : "#C2462B");
        linea = Color.parseColor(oscuro ? "#2E3756" : "#E6DFD3");
    }

    int dp(int v) {
        return Math.round(v * densidad);
    }

    GradientDrawable redondo(int color, int radioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radioDp));
        return g;
    }

    GradientDrawable circulo(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    ColorStateList lista(int color) {
        return ColorStateList.valueOf(color);
    }
}
