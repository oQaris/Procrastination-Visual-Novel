package com.pryanik.procrastination.xml_parser;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XMLParseDialogues {
    public static final int MAX_NUM_CHOICE = 4;         // Максимальное количество выборов (больше 4 на мелких экранах может отображаться некорректно)
    public static final int INIT_NUM_EVENTS = 5;        // Среднее количество событий в одном блоке node
    public static final int INIT_NUM_PHRASES = 8;       // Среднее количество фраз в одном блоке node
    public static final int INIT_NUM_SPRITES = 4;       // Среднее количество спайтов в одном блоке node
    // (больше - величится расход памяти, меньше - уменьшится скорость)
    public static final String ENTRY = "start";         // ID блока node с которога начнётся игра
    public static final String SPLITTER = "\\\\";       // Регулярное выражение - разделитель фраз

    private final List<Node> nodes;
    private final Map<String, List<IEvent>> events;
    private final String[] avlbImages;
    private final String[] avlbSprites;
    private final String[] avlbSounds;
    private final AssetManager manager;

    public XMLParseDialogues(InputStream xml, String[] backgrounds, String[] sprites, String[] sounds, AssetManager assetManager) {
        manager = assetManager;
        nodes = new ArrayList<>();
        events = new HashMap<>();
        avlbImages = backgrounds;
        Arrays.sort(avlbImages);
        avlbSprites = sprites;
        Arrays.sort(avlbSprites);
        avlbSounds = sounds;
        Arrays.sort(avlbSounds);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLHandler handler = new XMLHandler();
            parser.parse(xml, handler);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalArgumentException("Невозможно создать парсер! Ошибка:" + e.getLocalizedMessage());
        } catch (IOException e) {
            throw new IllegalArgumentException("Невозможно открыть xml-файл! Ошибка:" + e.getLocalizedMessage());
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public Map<String, List<IEvent>> getEvents() {
        return events;
    }

    private class XMLHandler extends DefaultHandler {
        Set<String> setId;
        StringBuilder phrase;
        String prewId;
        Node curNode;
        Choice curChoice;
        String curBranch;
        boolean aboveIsNext;
        boolean isHaveStart;
        String eventTag;

        @Override
        public void startDocument() {
            setId = new HashSet<>();
            phrase = new StringBuilder();
            aboveIsNext = false;
            isHaveStart = false;
            eventTag = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, @NotNull Attributes attributes) {
            // EVENT
            if ("event".equals(qName)) {
                eventTag = attributes.getValue("tag");
                if (eventTag == null)
                    throw new IllegalArgumentException("У событий обязательный атрибут tag!");
                if (eventTag.contains(" ") || Pattern.compile(SPLITTER).matcher(eventTag).find())
                    throw new IllegalArgumentException("Атрибут tag не может содержать разделитель или пробелы!");

            } // BRANCH
            else if ("branch".equals(qName)) {
                curBranch = attributes.getValue("name");
            } // NODE
            else if ("node".equals(qName)) {
                curNode = parseNode(attributes);
                if (curBranch == null)
                    throw new IllegalArgumentException("Блок node с ID=" + curNode.getId() + " находится вне ветки!");
                curNode.setBranchName(curBranch);
                nodes.add(curNode);
                // Обнуляем строку
                phrase = new StringBuilder();

            } // CHOICE
            else if ("choice".equals(qName)) {
                curChoice = parseChoice(attributes);
                curNode.addChoice(curChoice);
                // Обнуляем строку
                phrase = new StringBuilder();

            } // SPRITE
            else if ("sprite".equals(qName)) {
                if (eventTag != null)
                    putEvent(parseSprite(attributes));
                else
                    curNode.addSprite(parseSprite(attributes));

            } // SOUND
            else if ("sound".equals(qName)) {
                Sound s = new Sound(attributes.getValue("src"));
                if (eventTag != null)
                    putEvent(s);
                else
                    curNode.setSound(s);

            } else if (!"resources".equals(qName))
                throw new IllegalArgumentException(qName + " - Некорректный тег!");
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("event".equals(qName)) {
                eventTag = null;

            } // NODE
            else if ("node".equals(qName)) {
                if (curNode.getNext() == null && curNode.getCondition() != null)
                    throw new IllegalArgumentException("Блок node с ID=" + curNode.getId() + " содержит условие и не имеет атрибута next!");
                // Проверяем, чтоб хотя бы один выбор был без условия
                if (curNode.getChoices() != null) {
                    int i = 0;
                    for (Choice ch : curNode.getChoices()) {
                        if (ch.getCondition() != null)
                            i++;
                    }
                    if (i == curNode.getChoices().size())
                        throw new IllegalArgumentException("Блок node с ID=" + curNode.getId() + " содержит условия на всех выборах!");
                }
                if (curNode.getChoices() == null)
                    curNode.addPhrase(normalPhrase(), events);
                curNode = null;

            } // CHOICE
            else if ("choice".equals(qName)) {
                curChoice.setPhrase(normalPhrase());
                curChoice = null;

            } else if ("resources".equals(qName)) {
                if (!isHaveStart)
                    throw new IllegalArgumentException("Не найдена точка входа - отсутствует блок с ID=" + ENTRY);
            }
        }

        @Override
        public void characters(@NotNull char[] ch, int start, int length) {
            String addPhrase = new String(ch, start, length).trim();
            if (!addPhrase.equals(""))
                phrase.append(" ").append(addPhrase);
        }

        @NotNull
        private String normalPhrase() {
            if (phrase.length() == 0)    // Пробелов быть не может из за trim в characters()
                return "";
            return phrase.deleteCharAt(0).toString();
        }

        private void putEvent(IEvent e) {
            if (events.get(eventTag) == null)
                events.put(eventTag, new ArrayList<>(INIT_NUM_EVENTS));
            events.get(eventTag).add(e);
        }

        public Node parseNode(@NotNull Attributes attributes) {
            // Cond
            String cond = attributes.getValue("cond");
            // Id
            String id = attributes.getValue("id");
            if (id != null) {
                int preLen = setId.size();
                setId.add(id);
                if (preLen == setId.size())
                    throw new IllegalArgumentException("Невозможно создание блоков node с одинаковыми ID=" + id);
                if (id.equals(ENTRY))
                    isHaveStart = true;
                prewId = id;
            } else if (aboveIsNext) //TODO: опработать над ошибками
                throw new IllegalArgumentException("Недостижимый код ниже ветке с ID=" + prewId);
            // Next
            //todo проверка на переход в несуществующий node
            String next = attributes.getValue("next");
            aboveIsNext = next != null && cond == null;
            // Pers
            String pers = attributes.getValue("pers");
            // Back
            String back = attributes.getValue("rear");
            if (back != null && Arrays.binarySearch(avlbImages, back) < 0)       // Если картинки нет в массиве доступных
                throw new IllegalArgumentException("Блок node с ID=" + id + " содержит некорректное значение атрибута rear!");
            // Mus
            String mus = attributes.getValue("mus");
            if (mus != null && Arrays.binarySearch(avlbSounds, mus) < 0)
                throw new IllegalArgumentException("Блок node с ID=" + id + " содержит некорректное значение атрибута mus!");
            // Создаём node и добавляем в список
            return new Node(id, next, pers, back, mus, cond);
        }

        public Choice parseChoice(@NotNull Attributes attributes) {
            // Cond
            String cond = attributes.getValue("cond");
            // Next
            String next = attributes.getValue("next");
            if (curNode.getChoices() == null)
                curNode.addPhrase(normalPhrase(), events);
            // Choice
            if (curNode.getChoices() != null && curNode.getChoices().size() == MAX_NUM_CHOICE)
                throw new IllegalArgumentException("Невозможно добавить больше " + MAX_NUM_CHOICE + " выборов! в ветке с ID=" + prewId);
            return new Choice(next, cond);
        }

        public Sprite parseSprite(@NotNull Attributes attributes) {
            // Src
            String src = attributes.getValue("src");
            if (src == null)
                throw new IllegalArgumentException("У спрайтов обязательный атрибут src! в ветке с ID=" + prewId);
            // Feel
            String feel = attributes.getValue("feel");
            if (feel == null)
                throw new IllegalArgumentException("У спрайтов обязательный атрибут feel! в ветке с ID=" + prewId);
            if (Arrays.binarySearch(avlbSprites, src + "_" + feel + ".png") < 0)
                throw new IllegalArgumentException("Несуществующий спрайт! в ветке с ID=" + prewId);
            // Pos
            String posStr = attributes.getValue("pos");
            Float pos = null;
            if (posStr != null)
                pos = Float.parseFloat(posStr);
            // Act
            String actStr = attributes.getValue("act");
            Boolean act = actStr == null || Boolean.parseBoolean(actStr); // по-умолчанию - true
            Sprite sprite = null;
            try {
                Bitmap sp = BitmapFactory.decodeStream(manager.open("sprites/" + src + "_" + feel + ".png"));
                sprite = new Sprite(src, feel, pos, act, sp.getHeight(), sp.getWidth());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sprite;
        }
    }
}