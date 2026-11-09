package com.memoriavoz.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int FONDO = Color.rgb(245, 243, 238);
    private static final int TINTA = Color.rgb(31, 41, 51);
    private static final int SUAVE = Color.rgb(102, 112, 122);
    private static final int ACENTO = Color.rgb(15, 118, 110);
    private static final int ALERTA = Color.rgb(190, 60, 45);
    private static final int REQ_MIC = 10;
    private static final int REQ_NOTIF = 11;

    private SpeechRecognizer reconocedor;
    private boolean escuchando;
    private boolean probeRespaldo;
    private TextView microfono;
    private TextView estado;
    private LinearLayout lista;
    private LinearLayout avisoBateria;
    private String filtro = "";

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        Alarmas.crearCanales(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(FONDO);
        scroll.setFillViewport(true);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(dp(20), dp(28), dp(20), dp(32));
        scroll.addView(raiz);

        TextView titulo = texto("Memoria de Voz", 28, TINTA);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        raiz.addView(titulo);

        avisoBateria = new LinearLayout(this);
        avisoBateria.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(avisoBateria);

        microfono = new TextView(this);
        microfono.setText("🎙");
        microfono.setTextSize(52);
        microfono.setGravity(Gravity.CENTER);
        microfono.setContentDescription("Grabar nota de voz");
        LinearLayout.LayoutParams lpMic = new LinearLayout.LayoutParams(dp(136), dp(136));
        lpMic.gravity = Gravity.CENTER_HORIZONTAL;
        lpMic.topMargin = dp(26);
        raiz.addView(microfono, lpMic);
        microfono.setOnClickListener(v -> alternarEscucha());
        pintarMicrofono();

        estado = texto("Toca y dime qué debo recordar.\nEjemplo: «Reunión con Juan mañana a las 3 de la tarde»", 15, SUAVE);
        estado.setGravity(Gravity.CENTER);
        estado.setPadding(0, dp(14), 0, dp(6));
        raiz.addView(estado);

        Button escribir = new Button(this);
        escribir.setText("Prefiero escribirlo");
        escribir.setAllCaps(false);
        escribir.setBackgroundColor(Color.TRANSPARENT);
        escribir.setTextColor(ACENTO);
        escribir.setOnClickListener(v -> pedirTexto());
        raiz.addView(escribir);

        EditText buscar = new EditText(this);
        buscar.setHint("Buscar en mi memoria");
        buscar.setSingleLine(true);
        buscar.setTextColor(TINTA);
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
        raiz.addView(buscar);

        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(lista);

        setContentView(scroll);
        pedirPermisoNotificaciones();
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
        if (reconocedor != null) reconocedor.destroy();
        super.onDestroy();
    }

    private TextView texto(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private void pintarMicrofono() {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(escuchando ? ALERTA : ACENTO);
        microfono.setBackground(g);
        microfono.setElevation(dp(5));
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
        TextView t = texto("Para que los avisos suenen con el celular en reposo, permite que la app funcione sin"
                + " restricción de batería.", 14, TINTA);
        t.setPadding(0, dp(14), 0, dp(4));
        Button b = new Button(this);
        b.setText("Permitir");
        b.setAllCaps(false);
        b.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        avisoBateria.addView(t);
        avisoBateria.addView(b);
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
            estado.setText("Toca para agregar otro recordatorio.");
            abrirAnalizado(r.get(0));
        }

        @Override
        public void onPartialResults(Bundle parciales) {
            ArrayList<String> r = parciales.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (r != null && !r.isEmpty() && !r.get(0).isEmpty()) estado.setText(r.get(0));
        }

        @Override
        public void onEvent(int tipo, Bundle datos) {}
    }

    private void pedirTexto() {
        EditText e = new EditText(this);
        e.setHint("Ej.: pagar la luz el viernes a las 10");
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this)
                .setTitle("Escribe qué debo recordar")
                .setView(conMargen(e))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (d, w) -> {
                    String s = e.getText().toString().trim();
                    if (!s.isEmpty()) abrirAnalizado(s);
                })
                .show();
    }

    private View conMargen(View v) {
        LinearLayout c = new LinearLayout(this);
        c.setPadding(dp(22), dp(10), dp(22), 0);
        c.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return c;
    }

    private void abrirAnalizado(String dicho) {
        ParserFechas.Resultado p = ParserFechas.analizar(dicho, LocalDateTime.now());
        abrirEditor(null, p.texto, p.fecha, p.hora);
    }

    /** Pantalla de revisión: aquí se corrige lo que el celular haya entendido mal. */
    private void abrirEditor(Recordatorio existente, String textoInicial, LocalDate fechaIni, LocalTime horaIni) {
        final LocalDate[] fecha = {fechaIni};
        final LocalTime[] hora = {horaIni};

        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(22), dp(10), dp(22), 0);

        EditText campo = new EditText(this);
        campo.setText(textoInicial);
        campo.setTextColor(TINTA);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        c.addView(campo);

        Button bFecha = new Button(this);
        Button bHora = new Button(this);
        bFecha.setAllCaps(false);
        bHora.setAllCaps(false);
        CheckBox cDia = new CheckBox(this);
        CheckBox cMismo = new CheckBox(this);
        CheckBox cExacta = new CheckBox(this);
        cDia.setText("Avisarme un día antes");
        cExacta.setText("Avisarme también a la hora exacta");
        cDia.setChecked(existente == null || existente.diaAntes);
        cMismo.setChecked(existente == null || existente.mismoDia);
        cExacta.setChecked(existente != null && existente.exacta);

        Runnable refrescar = () -> {
            bFecha.setText(fecha[0] == null ? "Sin fecha: solo guardar (toca para elegir)"
                    : "Fecha: " + Fechas.dia(fecha[0], LocalDate.now()));
            bHora.setText(hora[0] == null ? "Sin hora (te aviso a las 9 de la mañana)"
                    : "Hora: " + Fechas.hora(hora[0]));
            cMismo.setText(hora[0] == null ? "Avisarme el mismo día" : "Avisarme el mismo día, una hora antes");
            int v = fecha[0] == null ? View.GONE : View.VISIBLE;
            bHora.setVisibility(v);
            cDia.setVisibility(v);
            cMismo.setVisibility(v);
            cExacta.setVisibility(hora[0] == null ? View.GONE : v);
        };

        bFecha.setOnClickListener(v -> {
            LocalDate d = fecha[0] != null ? fecha[0] : LocalDate.now();
            new DatePickerDialog(this, (vista, a, m, dia) -> {
                fecha[0] = LocalDate.of(a, m + 1, dia);
                refrescar.run();
            }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        bHora.setOnClickListener(v -> {
            LocalTime t = hora[0] != null ? hora[0] : LocalTime.of(9, 0);
            new TimePickerDialog(this, (vista, h, min) -> {
                hora[0] = LocalTime.of(h, min);
                refrescar.run();
            }, t.getHour(), t.getMinute(), false).show();
        });

        c.addView(bFecha);
        c.addView(bHora);
        c.addView(cDia);
        c.addView(cMismo);
        c.addView(cExacta);
        refrescar.run();

        ScrollView s = new ScrollView(this);
        s.addView(c);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Revisa lo que entendí")
                .setView(s)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null);
        if (fechaIni != null || existente != null) {
            b.setNeutralButton("Quitar fecha", null);
        }
        AlertDialog dialogo = b.create();
        dialogo.setOnShowListener(x -> {
            Button neutro = dialogo.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (neutro != null) {
                neutro.setOnClickListener(v -> {
                    fecha[0] = null;
                    hora[0] = null;
                    refrescar.run();
                });
            }
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String t = campo.getText().toString().trim();
                if (t.isEmpty()) {
                    campo.setError("Escribe algo para guardar");
                    return;
                }
                if (guardar(existente, t, fecha[0], hora[0], cDia.isChecked(), cMismo.isChecked(),
                        cExacta.isChecked())) {
                    dialogo.dismiss();
                }
            });
        });
        dialogo.show();
    }

    private boolean guardar(Recordatorio existente, String t, LocalDate f, LocalTime h, boolean dia, boolean mismo,
                            boolean exacta) {
        long ahora = System.currentTimeMillis();
        long evento = 0;
        if (f != null) {
            LocalDateTime ev = f.atTime(h != null ? h : LocalTime.of(9, 0));
            evento = Recordatorio.aMillis(ev);
            if (evento <= ahora) {
                Toast.makeText(this, "Esa fecha y hora ya pasaron. Elige otra.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        Recordatorio r = new Recordatorio();
        r.id = existente != null ? existente.id : ahora;
        r.texto = t;
        r.evento = evento;
        r.conHora = f != null && h != null;
        r.diaAntes = dia;
        r.mismoDia = mismo;
        r.exacta = exacta && h != null;
        r.creado = ahora;
        r.ultimoAviso = 0;
        if (f != null && !r.diaAntes && !r.mismoDia && !r.exacta) r.exacta = true;
        Almacen.guardar(this, r);
        Alarmas.programar(this, r);
        Toast.makeText(this, f == null ? "Guardado en tu memoria" : "Listo, te avisaré", Toast.LENGTH_SHORT).show();
        pintarLista();
        return true;
    }

    private void pintarLista() {
        lista.removeAllViews();
        long ahora = System.currentTimeMillis();
        List<Recordatorio> proximos = new ArrayList<>();
        List<Recordatorio> notas = new ArrayList<>();
        List<Recordatorio> pasados = new ArrayList<>();
        for (Recordatorio r : Almacen.todos(this)) {
            if (!filtro.isEmpty() && !r.texto.toLowerCase(Locale.ROOT).contains(filtro)) continue;
            if (!r.tieneFecha()) notas.add(r);
            else if (r.evento >= ahora) proximos.add(r);
            else pasados.add(r);
        }
        proximos.sort(Comparator.comparingLong((Recordatorio r) -> r.evento));
        notas.sort(Comparator.comparingLong((Recordatorio r) -> -r.creado));
        pasados.sort(Comparator.comparingLong((Recordatorio r) -> -r.evento));

        seccion("Próximos", proximos);
        seccion("Información guardada", notas);
        seccion("Pasados", pasados.size() > 30 ? pasados.subList(0, 30) : pasados);

        if (proximos.isEmpty() && notas.isEmpty() && pasados.isEmpty()) {
            TextView v = texto(filtro.isEmpty() ? "Todavía no has guardado nada." : "No encontré nada con esa palabra.",
                    15, SUAVE);
            v.setGravity(Gravity.CENTER);
            v.setPadding(0, dp(24), 0, 0);
            lista.addView(v);
        }
    }

    private void seccion(String nombre, List<Recordatorio> items) {
        if (items.isEmpty()) return;
        TextView t = texto(nombre, 17, TINTA);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(20), 0, dp(6));
        lista.addView(t);
        for (Recordatorio r : items) lista.addView(tarjeta(r));
    }

    private View tarjeta(Recordatorio r) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(10), dp(4));
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE);
        g.setCornerRadius(dp(12));
        c.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);

        c.addView(texto(r.texto, 16, TINTA));
        if (r.tieneFecha()) {
            TextView f = texto(Fechas.completa(r.fechaEvento(), r.conHora, LocalDate.now()), 14, ACENTO);
            f.setPadding(0, dp(4), 0, 0);
            c.addView(f);
        }

        LinearLayout botones = new LinearLayout(this);
        botones.setGravity(Gravity.END);
        botones.addView(boton("Escuchar", ACENTO, v -> startForegroundService(Alarmas.intentVoz(this, r.id, 1))));
        botones.addView(boton("Editar", SUAVE, v -> {
            LocalDate d = r.tieneFecha() ? r.fechaEvento().toLocalDate() : null;
            LocalTime h = r.conHora ? r.fechaEvento().toLocalTime() : null;
            abrirEditor(r, r.texto, d, h);
        }));
        botones.addView(boton("Borrar", ALERTA, v -> new AlertDialog.Builder(this)
                .setMessage("¿Borrar este recordatorio?")
                .setNegativeButton("No", null)
                .setPositiveButton("Borrar", (d, w) -> {
                    Alarmas.cancelar(this, r);
                    Almacen.borrar(this, r.id);
                    pintarLista();
                })
                .show()));
        c.addView(botones);
        return c;
    }

    private Button boton(String t, int color, View.OnClickListener accion) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setOnClickListener(accion);
        return b;
    }
}
