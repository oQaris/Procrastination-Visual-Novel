package com.pryanik.procrastination.xml_parser;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public class Sprite implements IEvent, Serializable {
    private final String src;
    private final String feel;
    private final Float pos;
    private final Boolean act;
    private final int height;
    private final int width;

    public Sprite(String src, String feel, Float pos, Boolean act, int height, int width) {
        this.src = src;
        this.feel = feel;
        this.pos = pos;
        this.act = act;
        this.height = height;
        this.width = width;
    }

    public String getSrc() {
        return src;
    }

    public String getFeel() {
        return feel;
    }

    public Float getPos() {
        return pos;
    }

    public Boolean isAct() {
        return act;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String name() {
        return "sprites/" + src + "_" + feel + ".png";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sprite sprite = (Sprite) o;
        return height == sprite.height &&
                width == sprite.width &&
                Objects.equals(src, sprite.src) &&
                Objects.equals(feel, sprite.feel) &&
                Objects.equals(pos, sprite.pos) &&
                Objects.equals(act, sprite.act);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, feel, pos, act, height, width);
    }

    @NotNull
    @Override
    public String toString() {
        return "Sprite{" +
                "src='" + src + '\'' +
                ", feel='" + feel + '\'' +
                ", pos=" + pos +
                ", act=" + act +
                ", height=" + height +
                ", width=" + width +
                '}';
    }
}