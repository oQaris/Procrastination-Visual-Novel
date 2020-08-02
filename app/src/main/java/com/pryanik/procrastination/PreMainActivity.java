package com.pryanik.procrastination;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.pryanik.procrastination.game_safe.Safekeeping;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.IEvent;
import com.pryanik.procrastination.xml_parser.Node;
import com.pryanik.procrastination.xml_parser.XMLParseDialogues;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.pryanik.procrastination.game_safe.Safekeeping.DEFAULT_SAVE;
import static com.pryanik.procrastination.game_safe.Safekeeping.curId;
import static com.pryanik.procrastination.game_safe.Safekeeping.curIdx;
import static com.pryanik.procrastination.game_safe.Safekeeping.loadGame;
import static com.pryanik.procrastination.game_safe.Safekeeping.passNodeId;
import static com.pryanik.procrastination.game_safe.Safekeeping.removeAllSafe;

public class PreMainActivity extends AppCompatActivity {
    // Игра
    public static Node frame;                 // Текущий слайд игры
    static List<Node> nodes;           // Список всех слайдов
    protected TextView debag;
    ConstraintLayout constraintLayout;
    Handler h;
    // Текст
    TextView tv;
    TextView prsName;
    Button[] btnsChoice;
    ImageView background;       // Задний фон
    Map<String, List<IEvent>> events;   // Карта событий
    // Спрайты
    Point size;
    Map<String, ImageSwitcher> spImgList;
    // Звуки
    MediaPlayer mp;
    SoundPool sp;

    protected static Node getNextNode() {
        if (frame.getNext() == null) {
            curIdx++;
            Node out = getNode(curId, curIdx);
            if (out.getId() != null) {
                curId = out.getId();
                curIdx = 0;
            }
            return out;
        } else {
            curId = frame.getNext();
            curIdx = 0;
            return getNode(curId, 0);
        }
    }

    public static Node getNode(@NotNull String id, int idx) {
        if (idx < 0 || idx > nodes.size() - 1)
            throw new IllegalArgumentException("Некорректное значение сдвига!");
        Iterator<Node> iter = nodes.iterator();
        while (iter.hasNext()) {
            Node out = iter.next();
            if (id.equals(out.getId())) {
                if (idx == 0)
                    return out;
                while (idx != 0 && iter.hasNext() && (out = iter.next()).getId() == null)
                    idx--;
                if (idx > 1)
                    throw new IllegalArgumentException("Сдвиг больше текущей ветки!");
                return out;
            }
        }
        throw new IllegalArgumentException("Не существует node с id=" + id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Service.hideNavigation(this);
        setContentView(R.layout.activity_main);

        // Привязываем объекты к нашей разметке
        constraintLayout = findViewById(R.id.activity_main);
        tv = findViewById(R.id.textView);
        prsName = findViewById(R.id.prsName);
        background = findViewById(R.id.background);
        btnsChoice = new Button[5];
        btnsChoice[0] = findViewById(R.id.btnChoice1);
        btnsChoice[1] = findViewById(R.id.btnChoice2);
        btnsChoice[2] = findViewById(R.id.btnChoice3);
        btnsChoice[3] = findViewById(R.id.btnChoice4);
        btnsChoice[4] = findViewById(R.id.btnChoice5);

        // Загрузка сохранений
        SharedPreferences settings = getSharedPreferences(SettingsActivity.SETTINGS_NAME, Context.MODE_PRIVATE);
        MainActivity.TEXT_TIMEOUT = settings.getInt(SettingsActivity.TEXT_RATE, 20);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        sp = new SoundPool.Builder().setAudioAttributes(attributes).build();
        mp = new MediaPlayer();
        mp.setLooping(true);

        h = new Handler();
        size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        spImgList = new HashMap<>();
        debag = findViewById(R.id.txt_debag);

        passNodeId = new HashSet<>();
        nodes = StartActivity.nodes;
        events = StartActivity.events;
        try {
            // Делаем новую игру или узнаём последнее сохранение
            int gameStart = getIntent().getIntExtra("game_start", 0);
            if (gameStart == StartActivity.NEW_GAME) {
                removeAllSafe();
                curId = XMLParseDialogues.ENTRY;
                frame = getNode(curId, 0);
                startActivity(new Intent(this, IntroComicActivity.class));
            } else if (gameStart == StartActivity.CONTINUE_GAME) {
                frame = loadGame(this, DEFAULT_SAVE);
            } else throw new IllegalArgumentException("Критическая ошибка!");
            showFrame();
        } catch (Exception e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
    }

    public void showFrame() {
    }

    protected void setBackground(String image, String music) {
    }

    /*protected static Node getNode(int idx) {
        if (idx < 0 || idx >= nodes.size())
            throw new IllegalArgumentException("Не существует node с индексом=" + idx);
        curIdx = idx;
        return nodes.get(idx);
    }*/

    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onPause() {
        super.onPause();
        if (mp != null && !IntroComicActivity.isComicDisplayed)
            mp.pause();
        //saveGame(this, DEFAULT_SAVE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mp != null)
            mp.start();
        if (Safekeeping.isRequestLoad && ConservationActivity.isSuccessLoad)
            showFrame();
        Service.hideNavigation(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Service.hideNavigation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mp != null)
            mp.stop();
        //saveGame(this, DEFAULT_SAVE);
    }
}