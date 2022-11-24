package com.example.background.workers;

import static com.example.background.Constants.KEY_IMAGE_URI;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SaveImageToFileWorker extends Worker {

    public SaveImageToFileWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    private static final String TAG = SaveImageToFileWorker.class.getSimpleName();

    private static final String TITLE = "Blurred Image";
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault());

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();

        // Makes a notification when the work starts and slows down the work so that it's easier to
        // see each WorkRequest start, even on emulated devices
        WorkerUtils.makeStatusNotification("Saving image", appContext);
        WorkerUtils.sleep();



        ContentResolver resolver = appContext.getContentResolver();

        try {

            String resourceUri = getInputData().getString(KEY_IMAGE_URI);

            // Create a bitmap
            Bitmap picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri)));

            // Blur the bitmap
            Bitmap output = WorkerUtils.blurBitmap(picture, appContext);

            // Write bitmap to a temp file
            String outputUri = MediaStore.Images.Media.insertImage
                    (resolver, output, TITLE, DATE_FORMATTER.format(new Date()));

            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri");
                throw new IllegalArgumentException("Invalid input uri");
            }

            Data outputData = new Data.Builder()
                    .putString(KEY_IMAGE_URI, outputUri.toString())
                    .build();

            // If there were no errors, return SUCCESS
            return Result.success(outputData);
        } catch (Throwable throwable) {
            /*
            Technically WorkManager will return Result.failure()
            but it's best to be explicit about it
            Thus if there were errors, we're return FAILURE
             */
            Log.e(TAG, "Error applying blur", throwable);
            return Result.failure();
        }


    }
}
