package com.example.mungnyang.api;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadAPI {
    @Multipart
    @POST("/upload")
    Call<ResponseBody> uploadDeltaBatch(@Part MultipartBody.Part[] deltas);
}
