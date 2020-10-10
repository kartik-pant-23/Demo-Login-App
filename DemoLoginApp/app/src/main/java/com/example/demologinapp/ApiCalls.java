package com.example.demologinapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiCalls {

    @POST("oauth/v2/accessToken/")
    Call<LinkedInAccessDetails> getAccessToken(
            @Query("client_id") String clientId,
            @Query("client_secret") String clientSecret,
            @Query("grant_type") String grantType,
            @Query("redirect_uri") String redirectUri,
            @Query("code") String code
    );

    @GET("v2/me/")
    Call<String> getUserDetails(
            @Query("projection") String projection,
            @Query("oauth2_access_token") String accessToken
    );

}
