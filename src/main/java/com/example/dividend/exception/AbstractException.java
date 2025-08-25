package com.example.dividend.exception;

public abstract class AbstractException extends RuntimeException {

    public abstract int getStatusCode();

    @Override
    abstract public String getMessage();
}
