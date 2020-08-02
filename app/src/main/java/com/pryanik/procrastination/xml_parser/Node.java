package com.pryanik.procrastination.xml_parser;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Node implements Serializable {
    private final String id;
    private final String next;
    private final String pers;
    private final String condition;
    private final List<String> phrases;
    private final List<Sprite> sprites;
    private String branchName;
    private String background;
    private String music;
    private Sound sound;
    private List<Choice> choices;
    private Map<Integer, String> eventsMap;
    private int numEvent;

    Node(String id, String next, String pers, String background, String music, String condition) {
        this.id = id;
        this.next = next;
        this.pers = pers;
        this.condition = condition;
        this.background = background;
        this.music = music;
        this.phrases = new ArrayList<>(XMLParseDialogues.INIT_NUM_PHRASES);
        this.sprites = new ArrayList<>(XMLParseDialogues.INIT_NUM_SPRITES);
        this.eventsMap = new HashMap<>();
        this.numEvent = 0;
    }

    public String getId() {
        return id;
    }

    public String getNext() {
        return next;
    }

    public String getPers() {
        return pers;
    }

    public String getCondition() {
        return condition;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getMusic() {
        return music;
    }

    public void setMusic(String music) {
        this.music = music;
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public List<String> getPhrases() {
        return phrases;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public List<Sprite> getSprites() {
        return sprites;
    }

    public Map<Integer, String> getEventsMap() {
        return eventsMap;
    }

    void addPhrase(@NotNull String phrase, Map<String, List<IEvent>> events) {
        String[] bufPhrs = phrase.split(XMLParseDialogues.SPLITTER);
        boolean flag = false;
        for (String item : bufPhrs) {
            if (!item.startsWith(" ") && flag) {
                String tag = item.split(" ")[0];
                if (events.containsKey(tag))
                    eventsMap.put(numEvent, tag);
                else
                    throw new IllegalArgumentException("Некорректный тег события в блоке node с ID=" + id);
                item = item.substring(tag.length());
            }
            phrases.add(item.trim());
            flag = true;
            numEvent++;
        }
    }

    void addChoice(Choice ch) {
        if (choices == null)
            choices = new ArrayList<>(XMLParseDialogues.MAX_NUM_CHOICE);
        choices.add(ch);
    }

    void addSprite(Sprite sp) {
        sprites.add(sp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return numEvent == node.numEvent &&
                Objects.equals(branchName, node.branchName) &&
                Objects.equals(id, node.id) &&
                Objects.equals(next, node.next) &&
                Objects.equals(pers, node.pers) &&
                Objects.equals(condition, node.condition) &&
                Objects.equals(phrases, node.phrases) &&
                Objects.equals(sprites, node.sprites) &&
                Objects.equals(background, node.background) &&
                Objects.equals(music, node.music) &&
                Objects.equals(sound, node.sound) &&
                Objects.equals(choices, node.choices) &&
                Objects.equals(eventsMap, node.eventsMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchName, id, next, pers, condition, phrases, sprites, background, music, sound, choices, eventsMap, numEvent);
    }

    @NotNull
    @Override
    public String toString() {
        return "Node{" +
                "branchName='" + branchName + '\'' +
                ", id='" + id + '\'' +
                ", next='" + next + '\'' +
                ", pers='" + pers + '\'' +
                ", condition='" + condition + '\'' +
                ", phrases=" + phrases +
                ", sprites=" + sprites +
                ", background='" + background + '\'' +
                ", music='" + music + '\'' +
                ", sound=" + sound +
                ", choices=" + choices +
                ", eventsMap=" + eventsMap +
                ", numEvent=" + numEvent +
                '}';
    }
}