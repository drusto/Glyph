package com.grsr.staticGlyph;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.nothing.ketchum.GlyphMatrixUtils;

/**
 * Diese Aktivität ermöglicht es dem Benutzer, ein quadratisches Bild aus der Galerie
 * auszuwählen und im internen Speicher abzulegen. Der ausgewählte Inhalt kann
 * später vom {@link staticToyService} gelesen werden, um ihn auf der Glyph Matrix
 * darzustellen.
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;

    private ImageView previewImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewImageView = findViewById(R.id.preview_image_view);
        Button selectButton = findViewById(R.id.select_image_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Öffnet den Systemdialog zum Auswählen eines Bildes
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), REQUEST_CODE_PICK_IMAGE);
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
                    }

                    // Für das Toy nur ein skaliertes Graustufenbild speichern
                    Bitmap toyBitmap = getSquareBitmapFromUri(uri);
                    Bitmap grayscale = GlyphMatrixUtils.toGrayscaleBitmap(toyBitmap, 255);
                    saveBitmap(grayscale);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Lädt ein Bitmap aus der angegebenen Uri, skaliert und beschneidet es zu einem
     * quadratischen Bild.
     */
    private Bitmap getSquareBitmapFromUri(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        Bitmap original = BitmapFactory.decodeStream(input);
        if (input != null) {
            input.close();
        }
        if (original == null) {
            throw new IOException("Konnte Bild nicht laden");
        }
        int size = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - size) / 2;
        int y = (original.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(original, x, y, size, size);
        return Bitmap.createScaledBitmap(squared, 25, 25, true);
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
