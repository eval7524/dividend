package com.example.dividend.exception.impl;

import com.example.dividend.exception.AbstractException;

public class PasswordMisMatchException extends AbstractException {

    private final String username;

    public PasswordMisMatchException(String username) {
        this.username = username;
    }

    @Override
    public int getStatusCode() {
        return 401; // Unauthorized
    }

    @Override
    public String getMessage() {
        return "비밀번호가 일치하지 않습니다. ID : " + username;
    }
}