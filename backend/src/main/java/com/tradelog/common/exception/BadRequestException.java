package com.tradelog.common.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
