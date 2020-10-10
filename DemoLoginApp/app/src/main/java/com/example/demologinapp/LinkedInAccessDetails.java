package com.example.demologinapp;

public class LinkedInAccessDetails {

    private String access_token;
    private String expires_in;

    public LinkedInAccessDetails(String access_token, String expires_in) {
        this.access_token = access_token;
        this.expires_in = expires_in;
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getExpires_in() {
        return expires_in;
    }
}
