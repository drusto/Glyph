package com.grsr.staticGlyph;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Diese Aktivität ermöglicht es dem Benutzer, ein quadratisches Bild aus der Galerie
 * auszuwählen und im internen Speicher abzulegen. Der ausgewählte Inhalt kann
 * später vom {@link staticToyService} gelesen werden, um ihn auf der Glyph Matrix
 * darzustellen.
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;

    private ImageView previewImageView;
    private ImageView previewBitpmapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewImageView = findViewById(R.id.preview_image_view);
        previewBitpmapView = findViewById(R.id.preview_bitmap_view);
        Button selectButton = findViewById(R.id.select_image_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Öffnet den Systemdialog zum Auswählen eines Bildes
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                throw new RuntimeException("Test Crash"); // Force a crash

                //startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), REQUEST_CODE_PICK_IMAGE);
            }
        });

        // Beim Start: Wenn ein Vorschaubild gespeichert wurde, lade es
        File imgFile = new File(getFilesDir(), "selected_glyph_preview.png");
        if (!imgFile.exists()) {
            // Fallback: Verwende das Toy-Bild, falls kein Vorschaubild existiert
            imgFile = new File(getFilesDir(), "selected_glyph.png");
        }
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            previewImageView.setImageBitmap(bitmap);
        }

        File glyphFile = new File(getFilesDir(), "selected_glyph.png");
        if (glyphFile.exists()) {
            Bitmap bitmapGlyph = BitmapFactory.decodeFile(glyphFile.getAbsolutePath());
            previewBitpmapView.setImageBitmap(bitmapGlyph);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // Originales Bild für die Vorschau laden und speichern
                    InputStream previewStream = getContentResolver().openInputStream(uri);
                    Bitmap previewBitmap = BitmapFactory.decodeStream(previewStream);
                    if (previewStream != null) {
                        previewStream.close();
                    }
                    if (previewBitmap != null) {
                        previewImageView.setImageBitmap(previewBitmap);
                        savePreviewBitmap(previewBitmap);

                        // Für das Toy nur ein skaliertes Graustufenbild speichern
                        Bitmap toyBitmap = toGlyph25(previewBitmap);
                        previewBitpmapView.setImageBitmap(toyBitmap);
                        saveBitmap(toyBitmap);
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private Bitmap toGlyph25(Bitmap src) {
        final int TARGET = 25;
        Bitmap scaled = Bitmap.createBitmap(TARGET, TARGET, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaled);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

        // Bild proportional skalieren & mittig setzen
        float sw = src.getWidth();
        float sh = src.getHeight();
        float scale = Math.min(TARGET / sw, TARGET / sh);
        float scaledW = sw * scale;
        float scaledH = sh * scale;
        float dx = (TARGET - scaledW) / 2f;
        float dy = (TARGET - scaledH) / 2f;

        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postTranslate(dx, dy);
        canvas.drawBitmap(src, m, paint);

        // Jetzt Pixel farblich anpassen
        for (int y = 0; y < TARGET; y++) {
            for (int x = 0; x < TARGET; x++) {
                int pixel = scaled.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xff;

                if (alpha == 0) {
                    // Transparenter Bereich → Hintergrundfarbe #000000
                    scaled.setPixel(x, y, 0xFF000000);
                } else {
                    // Helligkeit berechnen (Luminanz)
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = (pixel) & 0xff;
                    float luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f; // 0..1

                    // Helligkeit auf Bereich #1C1C1C bis #FFFFFF mappen
                    // 0% → #1C1C1C (28,28,28), 100% → #FFFFFF (255,255,255)
                    int minVal = 0x1C; // 28
                    int maxVal = 0xFF; // 255
                    int gray = Math.round(minVal + (maxVal - minVal) * luminance);

                    scaled.setPixel(x, y, 0xFF000000 | (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        return scaled;
    }


    /**
     * Speichert das gegebene Bitmap im internen Speicher unter dem Namen
     * {@code selected_glyph.png}. Wenn bereits eine Datei vorhanden ist, wird sie
     * überschrieben.
     */
    private void saveBitmap(Bitmap bitmap) throws IOException {
        File file = new File(getFilesDir(), "selected_glyph.png");
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
    }

    /**
     * Speichert das Vorschaubild unter dem Namen {@code selected_glyph_preview.png}.
     */
    private void savePreviewBitmap(Bitmap bitmap) throws IOException {
        File file = new File(getFilesDir(), "selected_glyph_preview.png");
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
    }
}
