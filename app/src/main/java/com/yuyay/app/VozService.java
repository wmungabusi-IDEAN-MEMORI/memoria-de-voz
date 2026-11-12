package com.yuyay.app;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dice el recordatorio en voz alta usando el volumen de alarma. */
public class VozService extends Service implements TextToSpeech.OnInitListener {
    private static final int NOTIF_ID = 7001;
    private static final long LIMITE_MS = 150_000L;

    private static final class Pendiente {
        final String frase;
        final int veces;

        Pendiente(String frase, int veces) {
            this.frase = frase;
            this.veces = veces;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Pendiente> pendientes = new ArrayList<>();
    private final Runnable cierre = this::terminar;
    private TextToSpeech tts;
    private boolean listo;
    private int enCurso;
    private int contador;
    private AudioManager audio;
    private AudioFocusRequest foco;
    private PowerManager.WakeLock wake;

    private final AudioAttributes atributos = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build();

    @Override
    public void onCreate() {
        super.onCreate();
        Alarmas.crearCanales(this);
        wake = getSystemService(PowerManager.class)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MemoriaVoz:lectura");
        wake.acquire(LIMITE_MS);
        audio = getSystemService(AudioManager.class);
        foco = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(atributos)
                .build();
        tts = new TextToSpeech(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = new Notification.Builder(this, Alarmas.CANAL_VOZ)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle("Leyendo tu recordatorio")
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, n);
        }
        handler.removeCallbacks(cierre);
        handler.postDelayed(cierre, LIMITE_MS);

        long id = intent == null ? -1 : intent.getLongExtra(Alarmas.EXTRA_ID, -1);
        int veces = intent == null ? 1 : intent.getIntExtra(Alarmas.EXTRA_VECES, 1);
        String frase = intent == null ? null : intent.getStringExtra(Alarmas.EXTRA_FRASE);
        Recordatorio r = Almacen.buscar(this, id);
        if (r == null && frase == null) {
            if (enCurso == 0 && pendientes.isEmpty()) terminar();
            return START_NOT_STICKY;
        }
        pendientes.add(new Pendiente(frase != null ? frase : Alarmas.frase(r), veces));
        if (listo) hablarPendientes();
        return START_NOT_STICKY;
    }

    @Override
    public void onInit(int estado) {
        if (estado != TextToSpeech.SUCCESS) {
            terminar();
            return;
        }
        Locale[] idiomas = {new Locale("es", "EC"), new Locale("es", "MX"), new Locale("es", "US"),
                new Locale("es", "ES"), new Locale("es")};
        boolean idiomaOk = false;
        for (Locale l : idiomas) {
            if (tts.setLanguage(l) >= TextToSpeech.LANG_AVAILABLE) {
                idiomaOk = true;
                break;
            }
        }
        if (!idiomaOk) {
            android.util.Log.w("MemoriaVoz", "No hay voz en español instalada");
        }
        tts.setAudioAttributes(atributos);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                handler.post(VozService.this::fragmentoTerminado);
            }

            @Override
            public void onError(String utteranceId) {
                handler.post(VozService.this::fragmentoTerminado);
            }
        });
        listo = true;
        audio.requestAudioFocus(foco);
        hablarPendientes();
    }

    private void hablarPendientes() {
        for (Pendiente p : pendientes) {
            decirSilencio(p.veces > 1 ? 1800 : 300);
            for (int i = 0; i < p.veces; i++) {
                if (i > 0) decirSilencio(1500);
                enCurso++;
                tts.speak(p.frase, TextToSpeech.QUEUE_ADD, null, "f" + (contador++));
            }
        }
        pendientes.clear();
        if (enCurso == 0) terminar();
    }

    private void decirSilencio(long ms) {
        enCurso++;
        tts.playSilentUtterance(ms, TextToSpeech.QUEUE_ADD, "s" + (contador++));
    }

    private void fragmentoTerminado() {
        enCurso--;
        if (enCurso <= 0 && pendientes.isEmpty()) terminar();
    }

    private void terminar() {
        handler.removeCallbacks(cierre);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (audio != null && foco != null) audio.abandonAudioFocusRequest(foco);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (wake != null && wake.isHeld()) wake.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
