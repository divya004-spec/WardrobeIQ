package com.example.wardrobeiq.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.wardrobeiq.models.ClothingAnalysisResult;

import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClothingImageUploader {

    public interface UploadCallback {
        // Called on the main thread with the AI result.
        void onResult(ClothingAnalysisResult result);

        // Called on the main thread when the upload/analysis failed.
        void onError(String message);
    }

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public static void upload(
            Context context,
            String imagePath,
            UploadCallback callback) {

        executor.execute(() -> {

            File imageFile = new File(imagePath);

            if (!imageFile.exists()) {
                mainHandler.post(() ->
                        callback.onError("Image file was not found."));
                return;
            }

            RequestBody requestBody =
                    RequestBody.create(
                            MediaType.parse("image/jpeg"),
                            imageFile
                    );

            MultipartBody.Part imagePart =
                    MultipartBody.Part.createFormData(
                            "image",
                            imageFile.getName(),
                            requestBody
                    );

            ClothingApiService api = RetrofitClient.getApiService();

            api.analyzeClothing(imagePart)
                    .enqueue(new Callback<ClothingAnalysisResult>() {

                        @Override
                        public void onResponse(
                                Call<ClothingAnalysisResult> call,
                                Response<ClothingAnalysisResult> response) {

                            if (response.isSuccessful()
                                    && response.body() != null) {

                                callback.onResult(response.body());

                            } else {
                                callback.onError(
                                        readError(response)
                                );
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<ClothingAnalysisResult> call,
                                Throwable throwable) {

                            callback.onError(
                                    "Could not reach the server. "
                                            + "Check your internet and try again. ("
                                            + throwable.getMessage() + ")"
                            );
                        }
                    });
        });
    }

    // Builds a readable message from an HTTP error.
    private static String readError(Response<?> response) {

        String serverMessage = "";

        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                serverMessage = new JSONObject(body).optString("message", "");
            }
        } catch (Exception ignored) {
        }

        if (response.code() == 404) {
            return "Server route not found (HTTP 404). "
                    + "Check BASE_URL and the Render deployment.";
        }

        if (!serverMessage.isEmpty()) {
            return serverMessage;
        }

        return "Upload failed. HTTP " + response.code();
    }
}