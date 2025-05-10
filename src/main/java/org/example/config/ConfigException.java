package org.example.config;

public class ConfigException extends RuntimeException {
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}