package com.memoriavoz.app;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    public static final String ACCION_NUEVA = "com.memoriavoz.app.NUEVA";
    private static final int REQ_MIC = 10;
    private static final int REQ_NOTIF = 11;
    private static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Tema t;
    private SpeechRecognizer reconocedor;
    private boolean escuchando;
    private boolean probeRespaldo;
    private boolean verPasados;
    private String filtro = "";
    private View anillo;
    private ImageView microfono;
    private AnimatorSet pulso;
    private TextView estado;
    private TextView resumen;
    private LinearLayout lista;
    private LinearLayout avisoBateria;
    private FrameLayout raizMarco;
    private View barraDeshacer;
    private boolean modoCalendario;
    private LocalDate diaElegido;
    private YearMonth mesVisible;
    private LinearLayout calendario;
    private LinearLayout filaPestanas;
    private View buscadorVista;

    private int dp(int v) {
        return t.dp(v);
    }

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        t = new Tema(this);
        Alarmas.crearCanales(this);

        raizMarco = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(t.fondo);
        scroll.setFillViewport(true);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(dp(20), dp(24), dp(20), dp(96));
        scroll.addView(raiz);
        raizMarco.addView(scroll);

        raiz.addView(texto(Fechas.hoyLargo(LocalDate.now()), 14, t.suave));
        TextView titulo = texto("Memoria de Voz", 30, t.tinta);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        raiz.addView(titulo);
        resumen = texto("", 15, t.acento);
        resumen.setPadding(0, dp(2), 0, 0);
        raiz.addView(resumen);

        avisoBateria = new LinearLayout(this);
        avisoBateria.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(avisoBateria);

        raiz.addView(crearMicrofono());

        estado = texto("Toca el micrófono y dime qué debo recordar", 16, t.suave);
        estado.setGravity(Gravity.CENTER);
        estado.setPadding(dp(8), dp(2), dp(8), dp(2));
        raiz.addView(estado);

        Button escribir = new Button(this);
        escribir.setText("Prefiero escribirlo");
        escribir.setAllCaps(false);
        escribir.setBackgroundColor(Color.TRANSPARENT);
        escribir.setStateListAnimator(null);
        escribir.setTextColor(t.acento);
        escribir.setOnClickListener(v -> pedirTexto());
        raiz.addView(escribir);

        filaPestanas = new LinearLayout(this);
        filaPestanas.setBackground(t.redondo(t.tarjeta, 14));
        filaPestanas.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams lpPest = new LinearLayout.LayoutParams(MP, dp(52));
        lpPest.topMargin = dp(4);
        raiz.addView(filaPestanas, lpPest);
        pintarPestanas();

        buscadorVista = crearBuscador();
        raiz.addView(buscadorVista);

        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(lista);

        calendario = new LinearLayout(this);
        calendario.setOrientation(LinearLayout.VERTICAL);
        calendario.setVisibility(View.GONE);
        raiz.addView(calendario);

        setContentView(raizMarco);
        pedirPermisoNotificaciones();
        atenderIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        atenderIntent(intent);
    }

    private void atenderIntent(Intent intent) {
        if (intent != null && ACCION_NUEVA.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_MAIN);
            handler.postDelayed(() -> {
                if (!escuchando) alternarEscucha();
            }, 350);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Alarmas.reprogramarTodo(this);
        pintarAvisoBateria();
        pintarLista();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        detenerPulso();
        if (reconocedor != null) reconocedor.destroy();
        super.onDestroy();
    }

    private TextView texto(String s, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private View crearMicrofono() {
        FrameLayout c = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(210), dp(210));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(4);
        c.setLayoutParams(lp);

        anillo = new View(this);
        anillo.setBackground(t.circulo(t.tenue));
        FrameLayout.LayoutParams lpA = new FrameLayout.LayoutParams(dp(150), dp(150), Gravity.CENTER);
        c.addView(anillo, lpA);

        microfono = new ImageView(this);
        microfono.setImageResource(R.drawable.ic_mic);
        microfono.setImageTintList(t.lista(t.sobreAcento));
        microfono.setPadding(dp(36), dp(36), dp(36), dp(36));
        microfono.setElevation(dp(6));
        microfono.setContentDescription("Grabar nota de voz");
        microfono.setClickable(true);
        microfono.setFocusable(true);
        microfono.setOnClickListener(v -> alternarEscucha());
        c.addView(microfono, new FrameLayout.LayoutParams(dp(140), dp(140), Gravity.CENTER));
        pintarMicrofono();
        return c;
    }

    private void pintarMicrofono() {
        microfono.setBackground(t.circulo(escuchando ? t.alerta : t.acento));
        microfono.setContentDescription(escuchando ? "Terminar de grabar" : "Grabar nota de voz");
        if (escuchando) iniciarPulso();
        else detenerPulso();
    }

    private void iniciarPulso() {
        detenerPulso();
        ObjectAnimator x = ObjectAnimator.ofFloat(anillo, View.SCALE_X, 1f, 1.38f);
        ObjectAnimator y = ObjectAnimator.ofFloat(anillo, View.SCALE_Y, 1f, 1.38f);
        ObjectAnimator a = ObjectAnimator.ofFloat(anillo, View.ALPHA, 1f, 0.15f);
        for (ObjectAnimator o : new ObjectAnimator[]{x, y, a}) {
            o.setRepeatCount(ValueAnimator.INFINITE);
            o.setRepeatMode(ValueAnimator.REVERSE);
            o.setDuration(650);
        }
        pulso = new AnimatorSet();
        pulso.playTogether(x, y, a);
        pulso.start();
    }

    private void detenerPulso() {
        if (pulso != null) pulso.cancel();
        pulso = null;
        if (anillo != null) {
            anillo.setScaleX(1f);
            anillo.setScaleY(1f);
            anillo.setAlpha(1f);
        }
    }

    private View crearBuscador() {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setBackground(t.redondo(t.tarjeta, 14));
        c.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MP, dp(52));
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);

        ImageView lupa = new ImageView(this);
        lupa.setImageResource(R.drawable.ic_buscar);
        lupa.setImageTintList(t.lista(t.suave));
        c.addView(lupa, new LinearLayout.LayoutParams(dp(22), dp(22)));

        EditText buscar = new EditText(this);
        buscar.setHint("Buscar en mi memoria");
        buscar.setHintTextColor(t.suave);
        buscar.setSingleLine(true);
        buscar.setTextColor(t.tinta);
        buscar.setTextSize(16);
        buscar.setBackgroundColor(Color.TRANSPARENT);
        buscar.setPadding(dp(10), 0, 0, 0);
        buscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                filtro = s.toString().trim().toLowerCase(Locale.ROOT);
                pintarLista();
            }
        });
        c.addView(buscar, new LinearLayout.LayoutParams(0, MP, 1f));
        return c;
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    private void pintarAvisoBateria() {
        avisoBateria.removeAllViews();
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) return;
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(t.redondo(t.tenue, 14));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MP, WC);
        lp.topMargin = dp(14);
        c.setLayoutParams(lp);
        c.addView(texto("Para que los avisos suenen con el celular en reposo, permite que la app funcione sin"
                + " restricción de batería.", 14, t.tinta));
        Button b = botonLleno("Permitir", v -> {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(WC, dp(44));
        lpB.topMargin = dp(10);
        b.setLayoutParams(lpB);
        c.addView(b);
        avisoBateria.addView(c);
    }

    private Button botonLleno(String s, View.OnClickListener accion) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(t.sobreAcento);
        b.setBackground(t.redondo(t.acento, 12));
        b.setStateListAnimator(null);
        b.setPadding(dp(20), 0, dp(20), 0);
        b.setOnClickListener(accion);
        return b;
    }

    private Button botonTexto(String s, int color, View.OnClickListener accion) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setStateListAnimator(null);
        b.setOnClickListener(accion);
        return b;
    }

    private void alternarEscucha() {
        if (escuchando) {
            if (reconocedor != null) reconocedor.stopListening();
            estado.setText("Procesando…");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            estado.setText("Este celular no tiene reconocimiento de voz disponible. Instala o actualiza la app Google.");
            return;
        }
        probeRespaldo = false;
        iniciarReconocimiento("es-EC");
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] permisos, int[] resultados) {
        super.onRequestPermissionsResult(req, permisos, resultados);
        if (req != REQ_MIC) return;
        if (resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED) {
            alternarEscucha();
        } else {
            estado.setText("Sin permiso de micrófono no puedo escucharte. Actívalo en Ajustes o usa «Prefiero escribirlo».");
        }
    }

    private void iniciarReconocimiento(String idioma) {
        if (reconocedor != null) reconocedor.destroy();
        reconocedor = SpeechRecognizer.createSpeechRecognizer(this);
        reconocedor.setRecognitionListener(new Oyente());
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, idioma);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        escuchando = true;
        pintarMicrofono();
        estado.setText("Te escucho… toca otra vez al terminar");
        reconocedor.startListening(i);
    }

    private void finEscucha() {
        escuchando = false;
        pintarMicrofono();
    }

    private final class Oyente implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle b) {}

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float v) {}

        @Override
        public void onBufferReceived(byte[] b) {}

        @Override
        public void onEndOfSpeech() {
            estado.setText("Procesando…");
        }

        @Override
        public void onError(int error) {
            if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED && !probeRespaldo) {
                probeRespaldo = true;
                iniciarReconocimiento("es-ES");
                return;
            }
            finEscucha();
            switch (error) {
                case SpeechRecognizer.ERROR_NO_MATCH:
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    estado.setText("No te escuché bien. Toca e inténtalo otra vez.");
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    estado.setText("Hace falta internet para reconocer la voz. Revisa la conexión.");
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    estado.setText("Falta el permiso de micrófono.");
                    break;
                default:
                    estado.setText("El reconocimiento de voz falló (código " + error + "). Inténtalo otra vez.");
            }
        }

        @Override
        public void onResults(Bundle resultados) {
            finEscucha();
            ArrayList<String> r = resultados.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (r == null || r.isEmpty() || r.get(0).trim().isEmpty()) {
                estado.setText("No te escuché bien. Toca e inténtalo otra vez.");
                return;
            }
            estado.setText("Toca el micrófono para agregar otro");
            abrirAnalizado(r.get(0), true);
        }

        @Override
        public void onPartialResults(Bundle parciales) {
            ArrayList<String> r = parciales.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (r != null && !r.isEmpty() && !r.get(0).isEmpty()) estado.setText(r.get(0));
        }

        @Override
        public void onEvent(int tipo, Bundle datos) {}
    }

    private Dialog hoja(View contenido) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView s = new ScrollView(this);
        s.addView(contenido);
        GradientDrawable g = new GradientDrawable();
        g.setColor(t.tarjeta);
        float r = dp(22);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        s.setBackground(g);
        d.setContentView(s);
        return d;
    }

    private void mostrarHoja(Dialog d) {
        d.show();
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(MP, WC);
        w.setGravity(Gravity.BOTTOM);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private LinearLayout cuerpoHoja(String titulo) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(22), dp(20), dp(16));
        TextView v = texto(titulo, 21, t.tinta);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, 0, 0, dp(12));
        c.addView(v);
        return c;
    }

    private EditText campo(String textoIni, String pista) {
        EditText e = new EditText(this);
        e.setText(textoIni);
        e.setHint(pista);
        e.setHintTextColor(t.suave);
        e.setTextColor(t.tinta);
        e.setTextSize(18);
        e.setBackground(t.redondo(t.fondo, 14));
        e.setPadding(dp(14), dp(14), dp(14), dp(14));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return e;
    }

    private LinearLayout filaAcciones(String etiqueta, View.OnClickListener cancelar, View.OnClickListener aceptar) {
        LinearLayout f = new LinearLayout(this);
        f.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        f.setPadding(0, dp(14), 0, 0);
        f.addView(botonTexto("Cancelar", t.suave, cancelar));
        Button a = botonLleno(etiqueta, aceptar);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WC, dp(50));
        lp.leftMargin = dp(8);
        f.addView(a, lp);
        return f;
    }

    private void pedirTexto() {
        LinearLayout c = cuerpoHoja("¿Qué debo recordar?");
        EditText e = campo("", "Ej.: pagar la luz el viernes a las 10");
        c.addView(e);
        Dialog d = hoja(c);
        c.addView(filaAcciones("Continuar", v -> d.dismiss(), v -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return;
            d.dismiss();
            abrirAnalizado(s, false);
        }));
        mostrarHoja(d);
    }

    private void abrirAnalizado(String dicho, boolean porVoz) {
        ParserFechas.Resultado p = ParserFechas.analizar(dicho, LocalDateTime.now());
        abrirEditor(null, p.texto, p.fecha, p.hora, p.repite, porVoz);
    }

    private TextView etiquetaSeccion(String s) {
        TextView v = texto(s, 13, t.suave);
        v.setPadding(0, dp(16), 0, dp(6));
        return v;
    }

    private TextView chip(String s, boolean activo, View.OnClickListener accion) {
        TextView v = texto(s, 14, activo ? t.sobreAcento : t.tinta);
        v.setGravity(Gravity.CENTER);
        v.setBackground(t.redondo(activo ? t.acento : t.fondo, 20));
        v.setPadding(dp(14), 0, dp(14), 0);
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(accion);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WC, dp(44));
        lp.rightMargin = dp(8);
        v.setLayoutParams(lp);
        return v;
    }

    private Switch interruptor(String s, boolean activo) {
        Switch w = new Switch(this);
        w.setText(s);
        w.setTextSize(15);
        w.setTextColor(t.tinta);
        w.setChecked(activo);
        w.setMinHeight(dp(46));
        return w;
    }

    /** Hoja de revisión: aquí se corrige lo que el celular haya entendido mal. */
    private void abrirEditor(Recordatorio existente, String textoInicial, LocalDate fechaIni, LocalTime horaIni,
                             int repiteIni, boolean porVoz) {
        final LocalDate[] fecha = {fechaIni};
        final LocalTime[] hora = {horaIni};
        final int[] repite = {repiteIni};

        LinearLayout c = cuerpoHoja("Revisa lo que entendí");
        EditText texto = campo(textoInicial, "Qué debo recordar");
        c.addView(texto);

        c.addView(etiquetaSeccion("Cuándo"));
        LinearLayout rapidas = new LinearLayout(this);
        c.addView(rapidas);
        Button bFecha = botonSuave();
        Button bHora = botonSuave();
        LinearLayout pares = new LinearLayout(this);
        pares.setPadding(0, dp(8), 0, 0);
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lpF.rightMargin = dp(8);
        pares.addView(bFecha, lpF);
        pares.addView(bHora, new LinearLayout.LayoutParams(0, dp(50), 1f));
        c.addView(pares);

        TextView tRepite = etiquetaSeccion("Repetir");
        c.addView(tRepite);
        HorizontalScrollView scrollRep = new HorizontalScrollView(this);
        scrollRep.setHorizontalScrollBarEnabled(false);
        LinearLayout filaRep = new LinearLayout(this);
        scrollRep.addView(filaRep);
        c.addView(scrollRep);

        TextView tAvisos = etiquetaSeccion("Avisarme");
        c.addView(tAvisos);
        Switch sDia = interruptor("Un día antes", existente == null || existente.diaAntes);
        Switch sMismo = interruptor("El mismo día", existente == null || existente.mismoDia);
        Switch sExacta = interruptor("A la hora exacta", existente != null && existente.exacta);
        c.addView(sDia);
        c.addView(sMismo);
        c.addView(sExacta);

        final Runnable[] refrescar = new Runnable[1];
        refrescar[0] = () -> {
            LocalDate hoy = LocalDate.now();
            rapidas.removeAllViews();
            rapidas.addView(chip("Hoy", hoy.equals(fecha[0]), v -> {
                fecha[0] = hoy;
                refrescar[0].run();
            }));
            rapidas.addView(chip("Mañana", hoy.plusDays(1).equals(fecha[0]), v -> {
                fecha[0] = hoy.plusDays(1);
                refrescar[0].run();
            }));
            rapidas.addView(chip("Sin fecha", fecha[0] == null, v -> {
                fecha[0] = null;
                hora[0] = null;
                repite[0] = ParserFechas.REPITE_NO;
                refrescar[0].run();
            }));

            filaRep.removeAllViews();
            String[] nombres = {"No", "Diario", "Semanal", "Mensual", "Anual"};
            for (int i = 0; i < nombres.length; i++) {
                final int valor = i;
                filaRep.addView(chip(nombres[i], repite[0] == valor, v -> {
                    repite[0] = valor;
                    refrescar[0].run();
                }));
            }

            bFecha.setText(fecha[0] == null ? "Elegir fecha" : Fechas.dia(fecha[0], hoy));
            bHora.setText(hora[0] == null ? "Sin hora (9 a. m.)" : Fechas.hora(hora[0]));
            sMismo.setText(hora[0] == null ? "El mismo día" : "El mismo día, una hora antes");
            int v = fecha[0] == null ? View.GONE : View.VISIBLE;
            bHora.setVisibility(v);
            tRepite.setVisibility(v);
            scrollRep.setVisibility(v);
            tAvisos.setVisibility(v);
            sDia.setVisibility(v);
            sMismo.setVisibility(v);
            sExacta.setVisibility(hora[0] == null ? View.GONE : v);
        };

        bFecha.setOnClickListener(v -> {
            LocalDate d = fecha[0] != null ? fecha[0] : LocalDate.now();
            new DatePickerDialog(this, (vista, a, m, dia) -> {
                fecha[0] = LocalDate.of(a, m + 1, dia);
                refrescar[0].run();
            }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        bHora.setOnClickListener(v -> {
            LocalTime h = hora[0] != null ? hora[0] : LocalTime.of(9, 0);
            new TimePickerDialog(this, (vista, hh, min) -> {
                hora[0] = LocalTime.of(hh, min);
                refrescar[0].run();
            }, h.getHour(), h.getMinute(), false).show();
        });
        refrescar[0].run();

        Dialog d = hoja(c);
        c.addView(filaAcciones("Guardar", v -> d.dismiss(), v -> {
            String s = texto.getText().toString().trim();
            if (s.isEmpty()) {
                texto.setError("Escribe algo para guardar");
                return;
            }
            if (guardar(existente, s, fecha[0], hora[0], repite[0], sDia.isChecked(), sMismo.isChecked(),
                    sExacta.isChecked(), porVoz)) {
                d.dismiss();
            }
        }));
        mostrarHoja(d);
    }

    private Button botonSuave() {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextColor(t.tinta);
        b.setTextSize(14);
        b.setBackground(t.redondo(t.tenue, 12));
        b.setStateListAnimator(null);
        return b;
    }

    private boolean guardar(Recordatorio existente, String texto, LocalDate f, LocalTime h, int repite, boolean dia,
                            boolean mismo, boolean exacta, boolean porVoz) {
        long ahora = System.currentTimeMillis();
        Recordatorio r = new Recordatorio();
        r.id = existente != null ? existente.id : ahora;
        r.texto = texto;
        r.creado = ahora;
        r.conHora = f != null && h != null;
        r.diaAntes = dia;
        r.mismoDia = mismo;
        r.exacta = exacta && h != null;
        if (f != null) {
            r.repite = repite;
            r.diaBase = f.getDayOfMonth();
            r.evento = Recordatorio.aMillis(f.atTime(h != null ? h : LocalTime.of(9, 0)));
            if (!r.diaAntes && !r.mismoDia && !r.exacta) r.exacta = true;
            if (r.evento <= ahora) {
                if (r.repite == ParserFechas.REPITE_NO) {
                    Toast.makeText(this, "Esa fecha y hora ya pasaron. Elige otra.", Toast.LENGTH_LONG).show();
                    return false;
                }
                r.avanzarRepeticion(ahora);
            }
        }
        Almacen.guardar(this, r);
        Alarmas.programar(this, r);
        Toast.makeText(this, f == null ? "Guardado en tu memoria" : "Listo, te avisaré", Toast.LENGTH_SHORT).show();
        pintarLista();
        if (porVoz) {
            String frase = f == null ? "Guardado en tu memoria."
                    : "Listo. Te aviso sobre esto para " + Alarmas.cuando(r) + ".";
            try {
                startForegroundService(Alarmas.intentVoz(this, r.id, 1, frase));
            } catch (RuntimeException e) {
                android.util.Log.e("MemoriaVoz", "No se pudo confirmar por voz", e);
            }
        }
        return true;
    }

    private void pintarLista() {
        lista.removeAllViews();
        long ahora = System.currentTimeMillis();
        LocalDate hoy = LocalDate.now();
        List<Recordatorio> deHoy = new ArrayList<>();
        List<Recordatorio> deManana = new ArrayList<>();
        List<Recordatorio> semana = new ArrayList<>();
        List<Recordatorio> despues = new ArrayList<>();
        List<Recordatorio> notas = new ArrayList<>();
        List<Recordatorio> pasados = new ArrayList<>();
        int totalHoy = 0;
        for (Recordatorio r : Almacen.todos(this)) {
            boolean coincide = filtro.isEmpty() || r.texto.toLowerCase(Locale.ROOT).contains(filtro);
            if (r.tieneFecha() && r.evento >= ahora && r.fechaEvento().toLocalDate().equals(hoy)) totalHoy++;
            if (!coincide) continue;
            if (!r.tieneFecha()) {
                notas.add(r);
            } else if (r.evento < ahora) {
                pasados.add(r);
            } else {
                long d = ChronoUnit.DAYS.between(hoy, r.fechaEvento().toLocalDate());
                (d == 0 ? deHoy : d == 1 ? deManana : d <= 7 ? semana : despues).add(r);
            }
        }
        resumen.setText(totalHoy == 0 ? "Nada pendiente para hoy"
                : totalHoy == 1 ? "Tienes 1 recordatorio hoy" : "Tienes " + totalHoy + " recordatorios hoy");

        lista.setVisibility(modoCalendario ? View.GONE : View.VISIBLE);
        buscadorVista.setVisibility(modoCalendario ? View.GONE : View.VISIBLE);
        calendario.setVisibility(modoCalendario ? View.VISIBLE : View.GONE);
        if (modoCalendario) {
            pintarCalendario();
            return;
        }

        Comparator<Recordatorio> porFecha = Comparator.comparingLong((Recordatorio r) -> r.evento);
        for (List<Recordatorio> l : List.of(deHoy, deManana, semana, despues)) l.sort(porFecha);
        notas.sort(Comparator.comparingLong((Recordatorio r) -> -r.creado));
        pasados.sort(Comparator.comparingLong((Recordatorio r) -> -r.evento));

        seccion("Hoy", deHoy);
        seccion("Mañana", deManana);
        seccion("Próximos 7 días", semana);
        seccion("Más adelante", despues);
        seccion("Información guardada", notas);
        if (!pasados.isEmpty()) {
            Button b = botonTexto((verPasados ? "Ocultar pasados" : "Ver pasados") + " (" + pasados.size() + ")",
                    t.suave, v -> {
                        verPasados = !verPasados;
                        pintarLista();
                    });
            lista.addView(b);
            if (verPasados) {
                for (Recordatorio r : pasados.size() > 30 ? pasados.subList(0, 30) : pasados) lista.addView(tarjeta(r));
            }
        }

        if (lista.getChildCount() == 0) {
            TextView v = texto(filtro.isEmpty() ? "Todavía no has guardado nada." : "No encontré nada con esa palabra.",
                    15, t.suave);
            v.setGravity(Gravity.CENTER);
            v.setPadding(0, dp(28), 0, 0);
            lista.addView(v);
        }
    }

    private void seccion(String nombre, List<Recordatorio> items) {
        if (items.isEmpty()) return;
        TextView v = texto(nombre, 17, t.tinta);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(22), 0, dp(8));
        lista.addView(v);
        for (Recordatorio r : items) lista.addView(tarjeta(r));
    }

    private void pintarPestanas() {
        filaPestanas.removeAllViews();
        filaPestanas.addView(pestana("Lista", !modoCalendario, false));
        filaPestanas.addView(pestana("Calendario", modoCalendario, true));
    }

    private TextView pestana(String nombre, boolean activa, boolean calendarioAlTocar) {
        TextView v = texto(nombre, 15, activa ? t.sobreAcento : t.tinta);
        v.setGravity(Gravity.CENTER);
        if (activa) v.setBackground(t.redondo(t.acento, 11));
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(x -> {
            modoCalendario = calendarioAlTocar;
            pintarPestanas();
            pintarLista();
        });
        v.setLayoutParams(new LinearLayout.LayoutParams(0, MP, 1f));
        return v;
    }

    private void cambiarMes(int meses) {
        mesVisible = mesVisible.plusMonths(meses);
        LocalDate hoy = LocalDate.now();
        diaElegido = YearMonth.from(hoy).equals(mesVisible) ? hoy : mesVisible.atDay(1);
        pintarCalendario();
    }

    private void pintarCalendario() {
        calendario.removeAllViews();
        LocalDate hoy = LocalDate.now();
        if (diaElegido == null) diaElegido = hoy;
        if (mesVisible == null) mesVisible = YearMonth.from(diaElegido);
        LocalDate inicio = mesVisible.atDay(1);
        LocalDate fin = mesVisible.atEndOfMonth();

        Map<LocalDate, List<Recordatorio>> porDia = new HashMap<>();
        for (Recordatorio r : Almacen.todos(this)) {
            if (!r.tieneFecha()) continue;
            for (LocalDate d : Fechas.ocurrencias(r.fechaEvento().toLocalDate(), r.repite, r.diaBase, inicio, fin)) {
                porDia.computeIfAbsent(d, k -> new ArrayList<>()).add(r);
            }
        }

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(t.redondo(t.tarjeta, 16));
        panel.setPadding(dp(8), dp(8), dp(8), dp(12));
        LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(MP, WC);
        lpPanel.topMargin = dp(10);
        calendario.addView(panel, lpPanel);

        LinearLayout cabecera = new LinearLayout(this);
        cabecera.setGravity(Gravity.CENTER_VERTICAL);
        cabecera.addView(icono(R.drawable.ic_atras, "Mes anterior", t.tinta, v -> cambiarMes(-1)));
        TextView titulo = texto(Fechas.mesAnio(inicio), 17, t.tinta);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setGravity(Gravity.CENTER);
        cabecera.addView(titulo, new LinearLayout.LayoutParams(0, WC, 1f));
        cabecera.addView(icono(R.drawable.ic_adelante, "Mes siguiente", t.tinta, v -> cambiarMes(1)));
        panel.addView(cabecera);

        LinearLayout semana = new LinearLayout(this);
        for (String n : new String[]{"L", "M", "M", "J", "V", "S", "D"}) {
            TextView v = texto(n, 13, t.suave);
            v.setGravity(Gravity.CENTER);
            semana.addView(v, new LinearLayout.LayoutParams(0, dp(28), 1f));
        }
        panel.addView(semana);

        int vacios = inicio.getDayOfWeek().getValue() - 1;
        int dias = mesVisible.lengthOfMonth();
        int total = vacios + dias;
        for (int fila = 0; fila * 7 < total; fila++) {
            LinearLayout f = new LinearLayout(this);
            for (int c = 0; c < 7; c++) {
                int n = fila * 7 + c - vacios + 1;
                View celda;
                if (n < 1 || n > dias) {
                    celda = new View(this);
                } else {
                    LocalDate d = mesVisible.atDay(n);
                    celda = celda(d, hoy, porDia.getOrDefault(d, Collections.emptyList()).size());
                }
                f.addView(celda, new LinearLayout.LayoutParams(0, dp(52), 1f));
            }
            panel.addView(f);
        }

        TextView dia = texto(Fechas.hoyLargo(diaElegido), 17, t.tinta);
        dia.setTypeface(Typeface.DEFAULT_BOLD);
        dia.setPadding(0, dp(18), 0, dp(8));
        calendario.addView(dia);

        List<Recordatorio> delDia = new ArrayList<>(porDia.getOrDefault(diaElegido, Collections.emptyList()));
        delDia.sort(Comparator.comparing((Recordatorio r) -> r.fechaEvento().toLocalTime()));
        if (delDia.isEmpty()) {
            calendario.addView(texto("Nada pendiente este día.", 15, t.suave));
        }
        for (Recordatorio r : delDia) calendario.addView(tarjeta(r, true));

        LocalDate elegido = diaElegido;
        if (!elegido.isBefore(hoy)) {
            calendario.addView(botonTexto("Agregar para este día", t.acento,
                    v -> abrirEditor(null, "", elegido, null, ParserFechas.REPITE_NO, false)));
        }
    }

    private View celda(LocalDate d, LocalDate hoy, int cantidad) {
        boolean elegido = d.equals(diaElegido);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setClickable(true);
        c.setFocusable(true);
        c.setContentDescription(d.getDayOfMonth() + ", " + (cantidad == 0 ? "sin pendientes"
                : cantidad == 1 ? "1 pendiente" : cantidad + " pendientes"));
        c.setOnClickListener(v -> {
            diaElegido = d;
            pintarCalendario();
        });

        TextView n = texto(String.valueOf(d.getDayOfMonth()), 15, elegido ? t.sobreAcento : t.tinta);
        n.setGravity(Gravity.CENTER);
        if (elegido) n.setBackground(t.circulo(t.acento));
        else if (d.equals(hoy)) n.setBackground(t.circulo(t.tenue));
        c.addView(n, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout puntos = new LinearLayout(this);
        puntos.setGravity(Gravity.CENTER);
        int color = d.isBefore(hoy) ? t.suave : t.acento;
        for (int i = 0; i < Math.min(cantidad, 3); i++) {
            View p = new View(this);
            p.setBackground(t.circulo(color));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(5), dp(5));
            lp.leftMargin = dp(1);
            lp.rightMargin = dp(1);
            puntos.addView(p, lp);
        }
        LinearLayout.LayoutParams lpP = new LinearLayout.LayoutParams(WC, dp(9));
        c.addView(puntos, lpP);
        return c;
    }

    private View tarjeta(Recordatorio r) {
        return tarjeta(r, false);
    }

    private View tarjeta(Recordatorio r, boolean enCalendario) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(12), dp(10), dp(4), dp(10));
        c.setBackground(t.redondo(t.tarjeta, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MP, WC);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        c.setClickable(true);
        c.setOnClickListener(v -> {
            LocalDate d = r.tieneFecha() ? r.fechaEvento().toLocalDate() : null;
            LocalTime h = r.conHora ? r.fechaEvento().toLocalTime() : null;
            abrirEditor(r, r.texto, d, h, r.repite, false);
        });

        if (r.tieneFecha() && !enCalendario) c.addView(insignia(r.fechaEvento().toLocalDate()));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(r.tieneFecha() && !enCalendario ? dp(12) : dp(4), 0, dp(4), 0);
        info.addView(texto(r.texto, 16, t.tinta));
        if (r.tieneFecha()) {
            String meta = r.conHora ? Fechas.hora(r.fechaEvento().toLocalTime())
                    : enCalendario ? "Sin hora" : Fechas.dia(r.fechaEvento().toLocalDate(), LocalDate.now());
            if (r.repite != ParserFechas.REPITE_NO) meta += " · " + Fechas.repeticion(r.repite);
            TextView f = texto(meta, 14, t.acento);
            f.setPadding(0, dp(3), 0, 0);
            info.addView(f);
        }
        c.addView(info, new LinearLayout.LayoutParams(0, WC, 1f));

        c.addView(icono(R.drawable.ic_altavoz, "Escuchar", t.acento,
                v -> startForegroundService(Alarmas.intentVoz(this, r.id, 1))));
        c.addView(icono(R.drawable.ic_borrar, "Borrar", t.suave, v -> borrar(r)));
        return c;
    }

    private View insignia(LocalDate d) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setBackground(t.redondo(t.tenue, 12));
        TextView dia = texto(String.valueOf(d.getDayOfMonth()), 19, t.tinta);
        dia.setTypeface(Typeface.DEFAULT_BOLD);
        dia.setGravity(Gravity.CENTER);
        TextView mes = texto(Fechas.mesCorto(d), 11, t.tinta);
        mes.setGravity(Gravity.CENTER);
        b.addView(dia);
        b.addView(mes);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(56)));
        return b;
    }

    private ImageButton icono(int recurso, String descripcion, int color, View.OnClickListener accion) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(recurso);
        b.setImageTintList(t.lista(color));
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setContentDescription(descripcion);
        b.setOnClickListener(accion);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(48)));
        return b;
    }

    private void borrar(Recordatorio r) {
        Alarmas.cancelar(this, r);
        Almacen.borrar(this, r.id);
        pintarLista();
        mostrarDeshacer(r);
    }

    private void mostrarDeshacer(Recordatorio r) {
        if (barraDeshacer != null) raizMarco.removeView(barraDeshacer);
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackground(t.redondo(t.tinta, 14));
        b.setPadding(dp(16), 0, dp(4), 0);
        b.setElevation(dp(8));
        b.addView(texto("Recordatorio borrado", 15, t.fondo), new LinearLayout.LayoutParams(0, WC, 1f));
        View vista = b;
        b.addView(botonTexto("Deshacer", t.tenue, v -> {
            Almacen.guardar(this, r);
            Alarmas.programar(this, r);
            raizMarco.removeView(vista);
            pintarLista();
        }));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MP, dp(56), Gravity.BOTTOM);
        lp.setMargins(dp(14), 0, dp(14), dp(18));
        raizMarco.addView(b, lp);
        barraDeshacer = b;
        handler.postDelayed(() -> raizMarco.removeView(vista), 6000);
    }
}
