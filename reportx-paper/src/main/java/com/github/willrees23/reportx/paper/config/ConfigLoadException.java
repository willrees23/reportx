package com.github.willrees23.reportx.paper.config;

public class ConfigLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigLoadException(String message) {
        super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
