package com.example.wardrobeiq.network;

import com.example.wardrobeiq.models.ClothingAnalysisResult;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ClothingApiService {

    // No leading slash: it is added to BASE_URL.
    @Multipart
    @POST("analyze-clothing")
    Call<ClothingAnalysisResult> analyzeClothing(
            @Part MultipartBody.Part image
    );
}