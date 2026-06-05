package com.thejdog2000.dreadfall.config;

public final class ConfigValidationException extends Exception {
    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

