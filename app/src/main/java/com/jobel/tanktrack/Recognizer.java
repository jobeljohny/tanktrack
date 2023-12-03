package com.jobel.tanktrack;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Recognizer {
private final static String TAG = "RECOGNIZER";
private static final Pattern datePattern = Pattern.compile("\\b(?:\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})\\b");
private static final Pattern amountPattern = Pattern.compile("\\b\\d{3,4}\\.\\d{2}\\b");
private final TextRecognizer textRecognizer;
Context context;
public Recognizer(Context context){
    this.context = context;
    textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);


}
    public CompletableFuture<String> read(Uri uri){
        CompletableFuture<String> resultFuture = new CompletableFuture<>();


        try {
            InputImage image = InputImage.fromFilePath(context,uri);
            textRecognizer.process(image).addOnSuccessListener(text -> {
                        Log.d(TAG, "read: completed recognition");
                        String recognizedText = text.getText();
                        Log.d(TAG, recognizedText);
                        String result = extractDateAndPrice(recognizedText);
                        resultFuture.complete(result);

            })
         .addOnFailureListener(e -> {
             resultFuture.completeExceptionally(e);
         });

        } catch (IOException e) {
            e.printStackTrace();
            resultFuture.completeExceptionally(e);
        }
        return resultFuture;

    }
    private String extractDateAndPrice(String data){
        Matcher amountMatcher = amountPattern.matcher(data);
        String amount = amountMatcher.find() ? amountMatcher.group() : "-";
        Matcher dateMatcher = datePattern.matcher(data);
        String date = dateMatcher.find() ? dateMatcher.group() : "-";
        return amount+"|"+date;
    }

}




