package com.example.mlkit_is2024_2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

// ML Kit: Texto (OCR)
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

// ML Kit: Rostros
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

// ML Kit: Código de barras
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;

// Común
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements OnSuccessListener<Text>, OnFailureListener {

    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    Bitmap mSelectedImage;
    ImageView mImageView;
    TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);

        // Permisos
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
            }
        }
    }

    // --------- Abrir Galería / Cámara ----------
    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    // --------- Resultado de selección/captura ----------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_CAMERA) {
                    // Nota: ACTION_IMAGE_CAPTURE devuelve un thumbnail.
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                } else {
                    mSelectedImage = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), data.getData());
                }
                mImageView.setImageBitmap(mSelectedImage);
                txtResults.setText(""); // limpiar resultados previos
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error: " + e.getMessage());
    }

    // ================== ROSTROS ==================
    public void Rostrosfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Primero selecciona una imagen");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.size() == 0) {
                            txtResults.setText("No hay rostros");
                        } else {
                            txtResults.setText("Hay " + faces.size() + " rostro(s)");
                        }

                        // Dibuja bounding boxes
                        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
                        Bitmap bitmap = drawable.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(bitmap);

                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(5);
                        paint.setStyle(Paint.Style.STROKE);

                        for (Face rostro : faces) {
                            canvas.drawRect(rostro.getBoundingBox(), paint);
                        }

                        mImageView.setImageBitmap(bitmap);
                    }
                })
                .addOnFailureListener(this);
    }

    // ================== OCR ==================
    public void OCRfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Primero selecciona una imagen");
            return;
        }
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    @Override
    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
        StringBuilder resultados = new StringBuilder();
        if (blocks.size() == 0) {
            resultados.append("No hay texto");
        } else {
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    List<Text.Element> elements = lines.get(j).getElements();
                    for (int k = 0; k < elements.size(); k++) {
                        resultados.append(elements.get(k).getText()).append(" ");
                    }
                }
            }
            resultados.append("\n");
        }
        txtResults.setText(resultados.toString());
    }

    // ================== CÓDIGO DE BARRAS ==================
    // Usaremos el botón actual "Labeling" (id btLabel) para esto.
    // En el XML, el onClick del botón apunta a este método: CodigoBarrasfx
    public void CodigoBarrasfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Primero selecciona una imagen");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        // Limita a formatos típicos de productos (incluye EAN-13 de tu foto).
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_QR_CODE
                )
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes == null || barcodes.isEmpty()) {
                        txtResults.setText("No se detectó ningún código de barras.");
                        return;
                    }

                    // Dibuja rectángulos sobre los códigos detectados
                    Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable())
                            .getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(bitmap);
                    Paint p = new Paint();
                    p.setColor(Color.GREEN);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(6);

                    StringBuilder sb = new StringBuilder("Códigos detectados:\n");

                    for (Barcode bc : barcodes) {
                        if (bc.getBoundingBox() != null) {
                            canvas.drawRect(bc.getBoundingBox(), p);
                        }
                        String valor = bc.getRawValue();      // p.ej. 7862117323358
                        int formato = bc.getFormat();
                        sb.append(valor)
                                .append("  (formato: ").append(formatoToString(formato)).append(")\n");
                    }

                    mImageView.setImageBitmap(bitmap);
                    txtResults.setText(sb.toString());
                })
                .addOnFailureListener(this);
    }

    private String formatoToString(int f) {
        switch (f) {
            case Barcode.FORMAT_EAN_13: return "EAN_13";
            case Barcode.FORMAT_EAN_8:  return "EAN_8";
            case Barcode.FORMAT_UPC_A:  return "UPC_A";
            case Barcode.FORMAT_UPC_E:  return "UPC_E";
            case Barcode.FORMAT_CODE_128: return "CODE_128";
            case Barcode.FORMAT_QR_CODE: return "QR_CODE";
            default: return String.valueOf(f);
        }
    }
}
