package com.example.webcubic.realtimetracking.Remote;

public class Common {

    public static final String baseURL = "https://maps.googleapis.com";
    public static IGoogleApi getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleApi.class);

    }
}
