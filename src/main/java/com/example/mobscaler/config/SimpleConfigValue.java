package com.example.mobscaler.config;

public class SimpleConfigValue<T> {
    private final T value;

    public SimpleConfigValue(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}