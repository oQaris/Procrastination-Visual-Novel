package com.pryanik.procrastination.xml_parser;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public class Choice implements Serializable {
    private final String next;
    private final String condition;
    private String phrase;

    Choice(String next, String condition) {
        if (next == null)
            throw new IllegalArgumentException("В choice обязательный атрибут next!");
        this.next = next;
        this.condition = condition;
    }

    public String getNext() {
        return next;
    }

    public String getCondition() {
        return condition;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Choice choice = (Choice) o;
        return Objects.equals(next, choice.next) &&
                Objects.equals(condition, choice.condition) &&
                Objects.equals(phrase, choice.phrase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(next, condition, phrase);
    }

    @NotNull
    @Override
    public String toString() {
        return "Choice{" +
                "next='" + next + '\'' +
                ", condition='" + condition + '\'' +
                ", phrase='" + phrase + '\'' +
                '}';
    }
}