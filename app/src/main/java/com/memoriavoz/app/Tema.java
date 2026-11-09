package com.memoriavoz.app;

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
        fondo = Color.parseColor(oscuro ? "#14181C" : "#F5F3EE");
        tarjeta = Color.parseColor(oscuro ? "#1F252B" : "#FFFFFF");
        tinta = Color.parseColor(oscuro ? "#ECEFF1" : "#1F2933");
        suave = Color.parseColor(oscuro ? "#A3ADB6" : "#5F6A75");
        acento = Color.parseColor(oscuro ? "#5EC9BC" : "#0F766E");
        sobreAcento = Color.parseColor(oscuro ? "#0C1F1D" : "#FFFFFF");
        tenue = Color.parseColor(oscuro ? "#1C3A37" : "#D9EFEC");
        alerta = Color.parseColor(oscuro ? "#F08A7B" : "#BE3C2D");
        linea = Color.parseColor(oscuro ? "#323A42" : "#E2DED5");
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
