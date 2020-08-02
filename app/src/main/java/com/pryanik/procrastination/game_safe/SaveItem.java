package com.pryanik.procrastination.game_safe;

import android.graphics.Bitmap;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SaveItem {
    private String str;
    private String subStr;
    private Bitmap picture;

    public SaveItem(String str, String subStr, Bitmap picture) {
        this.str = str;
        this.subStr = subStr;
        this.picture = picture;
    }

    public String getStr() {
        return str;
    }

    public String getSubStr() {
        return subStr;
    }

    public Bitmap getPicture() {
        return picture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaveItem saveItem = (SaveItem) o;
        return Objects.equals(str, saveItem.str) &&
                Objects.equals(subStr, saveItem.subStr) &&
                Objects.equals(picture, saveItem.picture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(str, subStr, picture);
    }

    @NotNull
    @Override
    public String toString() {
        return "SaveItem{" +
                "str='" + str + '\'' +
                ", subStr='" + subStr + '\'' +
                ", picture=" + picture +
                '}';
    }
}