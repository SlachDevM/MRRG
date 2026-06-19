package com.mrrg.backend.dto;

public class ActivateAccountRequest {
    private String token;
    private String password;

    public ActivateAccountRequest() {
    }

    public ActivateAccountRequest(String token, String password) {
        this.token = token;
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
