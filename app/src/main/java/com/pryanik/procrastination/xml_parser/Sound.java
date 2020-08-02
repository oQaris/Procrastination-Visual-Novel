package com.pryanik.procrastination.xml_parser;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public class Sound implements IEvent, Serializable {
    private final String src;

    public Sound(String src) {
        if (src == null)
            throw new IllegalArgumentException("Звук не может быть пустым!");
        this.src = src;
    }

    public String getSrc() {
        return src;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sound sound = (Sound) o;
        return Objects.equals(src, sound.src);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src);
    }

    @NotNull
    @Override
    public String toString() {
        return "Sound{" +
                "src='" + src + '\'' +
                '}';
    }
}