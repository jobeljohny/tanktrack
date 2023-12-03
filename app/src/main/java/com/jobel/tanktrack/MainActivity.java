package com.jobel.tanktrack;


import androidx.activity.result.ActivityResultLauncher;;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.tbruyelle.rxpermissions3.RxPermissions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity {
    Button camera_btn, upload_btn,view_btn,export_btn;
    //temp
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int READ_PERMISSION_CODE = 200;

    Uri imageUri = Uri.EMPTY;
    String tempFilePath = "";
    boolean OCRInitialized = false;
    Recognizer Ocr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera_btn = findViewById(R.id.camera_btn);
        upload_btn = findViewById(R.id.upload_btn);
        view_btn = findViewById(R.id.view_btn);
        export_btn = findViewById(R.id.export_btn);
        final RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {
                        Log.d("PERMISSIONS", "onCreate: permission granted");
                        initializeOCR();

                    } else {
                        Toast.makeText(this,"Permission not granted",Toast.LENGTH_LONG).show();
                    }
                });
        camera_btn.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                imageUri = FileProvider.getUriForFile(this, "com.jobel.tanktrack.provider", createImageFile());
                takePicture.launch(imageUri);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            }
        });
        upload_btn.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                selectedPictureLauncher.launch("image/");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_CODE);
            }
        });

        view_btn.setOnClickListener(view -> {
           openBillsFolder();
        });

        export_btn.setOnClickListener(view ->{
            exportBills();
        });
    }

    private void initializeOCR(){
        if(!OCRInitialized) {
            OCRInitialized = true;
            Log.d("MAIN", "initializing OCR ");
            Ocr = new Recognizer(this);
        }
    }

    ActivityResultLauncher<Uri> takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
        if (success) {
           readOCR(imageUri);
        } else {
            Log.d("MyLog", "not success");
        }
    });

    ActivityResultLauncher selectedPictureLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            readOCR(uri);
        } else {
            Toast.makeText(this, "no image selected", Toast.LENGTH_SHORT).show();
        }
    });

    void readOCR(Uri uri){
        Ocr.read(uri).thenApply(result -> {
            Log.d("OCR Result", "price | date: "+ result);
            startPreviewActivity(result,uri);
            return result;
        }).exceptionally(exception ->{
            Log.d("OCR Result" ,exception.getMessage());
            exception.printStackTrace();
            return null; //again, something to return
        });

    }

    void startPreviewActivity(String result,Uri uri){
        Intent intent = new Intent(this, PreviewDialog.class);
        intent.putExtra("datePrice", result);
        intent.putExtra("billImage", uri);
        startActivity(intent);
    }

    File createImageFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File file = File.createTempFile("temp_image", ".jpg", dir);
            tempFilePath = file.getAbsolutePath();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void openBillsFolder(){
        File folderPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"FuelBills");
        if (folderPath.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("content://" + folderPath.getAbsolutePath());
            intent.setDataAndType(uri,"*/*");
            startActivity(intent);
        } else {
            Toast.makeText(this, "No bills present", Toast.LENGTH_SHORT).show();
        }
    }

    void exportBills(){

        Toast.makeText(this,"exporting...",Toast.LENGTH_SHORT).show();

        File folderPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"FuelBills");
        File zipFilePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FuelBills.zip");
        try {
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            zipFolder(folderPath, zipOut);
            zipOut.close();
            fos.close();

            Toast.makeText(this,"Successfully exported to Downloads",Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("EXPORT:", e.getMessage());
            Toast.makeText(this,"Error Occured",Toast.LENGTH_LONG).show();
        }
    }

    private static void zipFolder(File folder, ZipOutputStream zipOut) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
                ZipEntry zipEntry = new ZipEntry(file.getPath().substring(folder.getPath().length() + 1));
                zipOut.putNextEntry(zipEntry);
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, length);
                }
                fis.close();
                zipOut.closeEntry();
            }
    }

}