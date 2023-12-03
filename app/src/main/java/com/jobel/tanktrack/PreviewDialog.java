package com.jobel.tanktrack;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PreviewDialog extends AppCompatActivity {

    EditText priceEdit,dateEdit;
    ImageView imageView;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_dialog);
        priceEdit = findViewById(R.id.price_edit);
        dateEdit = findViewById(R.id.date_edit);
        imageView = findViewById(R.id.previewImage);
        button = findViewById(R.id.confrimButton);

        Intent intent = getIntent();
        String datePrice = intent.getStringExtra("datePrice");
        Uri uri  = intent.getParcelableExtra("billImage");
        updateDialog(datePrice,uri);

        button.setOnClickListener(view -> {
              saveBill(uri);
        });
    }

    void updateDialog(String datePrice, Uri uri){
        String PriceAndDate[] = datePrice.split("\\|");
        Log.d("dialog Content", "price: "+PriceAndDate[0]+" date: "+PriceAndDate[1]);
        imageView.setImageURI(uri);
        priceEdit.setText(PriceAndDate[0]);
        dateEdit.setText(PriceAndDate[1]);
    }

    void saveBill(Uri uri){
        String price = priceEdit.getText().toString();
        price = formatPrice(price);
        String date = dateEdit.getText().toString();
        date = formatDate(date);

        File baseDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File newDirectory = new File(baseDirectory, "FuelBills");
        if (!newDirectory.exists()) {
            newDirectory.mkdirs();
        }

        String imageName = date + "_Rs."+price+".jpg";

        File imageFile = new File(newDirectory, imageName);
        try (
                InputStream inputStream = this.getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(imageFile)
        ) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            Toast.makeText(this,"Successfully saved",Toast.LENGTH_LONG).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("SAVING_ERR", e.toString());
            if(e.getMessage().contains("File exists"))
                Toast.makeText(this,"Bill already exists",Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(this, "Error saving bill", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static String formatPrice(String price) {
        if (!"-".equals(price) && price.endsWith(".00")) {
            return price.substring(0, price.length() - 3);
        }
        return price;
    }

        public static String formatDate(String date){
        return date.replace('/','-');
        }
}