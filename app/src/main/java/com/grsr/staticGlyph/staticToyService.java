package com.grsr.staticGlyph;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import androidx.annotation.Nullable;

import java.io.File;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphMatrixFrame;
import com.nothing.ketchum.GlyphMatrixManager;
import com.nothing.ketchum.GlyphMatrixObject;
import com.nothing.ketchum.GlyphMatrixUtils;
import com.nothing.ketchum.GlyphToy;

/**
 * Ein Beispiel‑Dienst, der ein vom Benutzer ausgewähltes Bild als Glyph
 * darstellt und optional den Always‑On‑Modus unterstützt. Das Bild wird aus
 * dem internen Speicher geladen, in ein Integer‑Array konvertiert und dann über
 * den {@code GlyphMatrixManager} an die Matrix gesendet. Wenn die AOD‑Funktion
 * aktiviert ist, aktualisiert der Dienst bei jedem AOD‑Event das Bild auf der
 * Matrix.
 *
 * Beachten Sie: Für die Verwendung der Klassen {@code GlyphMatrixManager},
 * {@code Glyph} und {@code GlyphToy} müssen Sie die richtigen Pakete aus der
 * GlyphMatrixSDK importieren. Diese Datei enthält keine Importanweisungen für
 * diese Klassen, weil sie im AAR enthalten sind. Fügen Sie sie entsprechend
 * Ihrer Projektkonfiguration hinzu.
 */
public class staticToyService extends Service {

    // Verwalter für die Verbindung zur Glyph Matrix
    private GlyphMatrixManager mGM;
    private GlyphMatrixManager.Callback mCallback;

    private FirebaseAnalytics analytics;

    private final Handler serviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GlyphToy.MSG_GLYPH_TOY: {
                    Bundle bundle = msg.getData();
                    String event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA);
                    if (GlyphToy.EVENT_AOD.equals(event) || GlyphToy.EVENT_CHANGE.equals((event))) {
                      action();
                    }
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private final Messenger serviceMessenger = new Messenger(serviceHandler);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        analytics = FirebaseAnalytics.getInstance(this);
        sendLogToFirebase("Toy: onBind");
        // Initialisierung der Glyph‑Verbindung und Anzeige des Bildes
        initGlyph();
        return serviceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sendLogToFirebase("Toy: onUnBind");
        mGM.turnOff();
        mCallback = null;
        mGM = null;
        return false;
    }

    private void sendLogToFirebase(String message) {
        Bundle bundle = new Bundle();
        bundle.putString("log_message", message);
        analytics.logEvent("log_info", bundle);
    }



    /**
     * Initialisiert den GlyphMatrixManager, registriert das Gerät und sendet das
     * aktuell gespeicherte Bild an die Matrix. Diese Methode enthält bewusst
     * keine konkrete Implementierung der SDK‑Aufrufe; die entsprechenden
     * Methoden sind in den Kommentaren erwähnt und müssen mit den richtigen
     * Klassen des AAR ersetzt werden.
     */
    private void initGlyph() {
        sendLogToFirebase("Toy: initGlyph");
        try {
             mGM = GlyphMatrixManager.getInstance(getApplicationContext());
            mCallback = new GlyphMatrixManager.Callback() {
                @Override
                public void onServiceConnected(ComponentName componentName) {
                    sendLogToFirebase("Toy: onServiceConnected");
                    mGM.register(Glyph.DEVICE_23112);
                    action();
                }
                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    sendLogToFirebase("Toy: onServiceDisconntected");
                }
            };
            mGM.init(mCallback);

        } catch (Exception e) {
            sendLogToFirebase("Toy: initGlyph Error: " + e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private void action() {

        sendLogToFirebase("Toy: action");
        Bitmap data = loadImageData();
        sendLogToFirebase("Toy: Bitmap gelesen");
            try {
                sendLogToFirebase("Toy: create MatrixObject from Bitmap");
                GlyphMatrixObject matrixObject = new GlyphMatrixObject.Builder().setImageSource(data)
                        .setScale(100)
                        .setOrientation(0)
                        .setPosition(0, 0)
                        .setReverse(false)
                        .build();

                sendLogToFirebase("Toy: create MatrixFrame from MatrixObject");
                GlyphMatrixFrame frame = new GlyphMatrixFrame.Builder()
                        .addTop(matrixObject)
                        .build(this);

                sendLogToFirebase("Toy: set MatrixFrame from Frame Render");
                mGM.setMatrixFrame(frame.render());
            } catch (GlyphException e) {
                sendLogToFirebase("Toy: setMatrixFrame Fehler:" + e.getLocalizedMessage());
                throw new RuntimeException(e);
            }

    }
    /**
     * Lädt das gespeicherte Glyph‑Bild und konvertiert es in ein Integer‑Array
     * mit Graustufen‑Werten. Dieses Array entspricht einem 25×25‑Raster und kann
     * direkt an {@code setMatrixFrame(int[])} übergeben werden【751807440283616†L495-L506】.
     */
    private Bitmap loadImageData() {
        File file = new File(getFilesDir(), "selected_glyph.png");
        if (!file.exists()) {
            sendLogToFirebase("Toy: loadImageData: Datei nicht gefunden");
            throw new RuntimeException("BIld nicht gefunden");
        }

        sendLogToFirebase("Toy: loadImageData: Datei gefunden");
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            sendLogToFirebase("Toy: loadImageData: Bitmap nicht erstellt");
            throw new RuntimeException("Bitmap nicht erstellt");
        }
        sendLogToFirebase("Toy: loadImageData: Bitmap erstellt");

        return bitmap;

    }


}
