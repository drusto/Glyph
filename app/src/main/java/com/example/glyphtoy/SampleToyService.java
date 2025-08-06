package com.example.glyphtoy;

import android.app.Service;
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
public class SampleToyService extends Service {

    // Verwalter für die Verbindung zur Glyph Matrix
    private Object mGM;

    // Empfängt Nachrichten vom System (z. B. AOD‑Events)
    private final Handler serviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            // Die Konstanten MSG_GLYPH_TOY, MSG_GLYPH_TOY_DATA und EVENT_AOD
            // stammen aus der Klasse GlyphToy des Nothing SDK. Stellen Sie
            // sicher, dass Sie die Klasse importieren und verwenden.
            switch (msg.what) {
                // case GlyphToy.MSG_GLYPH_TOY:
                //     Bundle bundle = msg.getData();
                //     String event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA);
                //     if (GlyphToy.EVENT_AOD.equals(event)) {
                //         // Beim AOD‑Ereignis die Matrix erneut mit dem Bild füllen
                //         int[] data = loadImageData();
                //         if (mGM != null && data != null) {
                //             // ((GlyphMatrixManager)mGM).setMatrixFrame(data);
                //         }
                //     }
                //     break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private final Messenger serviceMessenger = new Messenger(serviceHandler);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Initialisierung der Glyph‑Verbindung und Anzeige des Bildes
        initGlyph();
        return serviceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Bei Beendigung die Verbindung trennen
        if (mGM != null) {
            try {
                // ((GlyphMatrixManager)mGM).unInit();
            } catch (Exception e) {
                // ignorieren
            }
            mGM = null;
        }
        return super.onUnbind(intent);
    }

    /**
     * Initialisiert den GlyphMatrixManager, registriert das Gerät und sendet das
     * aktuell gespeicherte Bild an die Matrix. Diese Methode enthält bewusst
     * keine konkrete Implementierung der SDK‑Aufrufe; die entsprechenden
     * Methoden sind in den Kommentaren erwähnt und müssen mit den richtigen
     * Klassen des AAR ersetzt werden.
     */
    private void initGlyph() {
        try {
            // mGM = new GlyphMatrixManager();
            // ((GlyphMatrixManager)mGM).init(null); // Callback implementieren falls benötigt
            // ((GlyphMatrixManager)mGM).register(Glyph.DEVICE_23112);

            int[] data = loadImageData();
            if (data != null) {
                // ((GlyphMatrixManager)mGM).setMatrixFrame(data);
            }
        } catch (Exception e) {
            // Fehlerbehandlung (optional)
        }
    }

    /**
     * Lädt das gespeicherte Glyph‑Bild und konvertiert es in ein Integer‑Array
     * mit ARGB‑Werten. Dieses Array entspricht einem 25×25‑Raster und kann
     * direkt an {@code setMatrixFrame(int[])} übergeben werden【751807440283616†L495-L506】.
     */
    private int[] loadImageData() {
        File file = new File(getFilesDir(), "selected_glyph.png");
        if (!file.exists()) {
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] data = new int[width * height];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[index++] = bitmap.getPixel(x, y);
            }
        }
        return data;
    }
}
