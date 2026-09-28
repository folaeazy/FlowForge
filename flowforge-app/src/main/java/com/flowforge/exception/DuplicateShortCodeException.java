package com.flowforge.exception;

public class DuplicateShortCodeException extends RuntimeException{

    public DuplicateShortCodeException(String shortCode) {
        super("short_code already exists: " +  shortCode);
    }
}
