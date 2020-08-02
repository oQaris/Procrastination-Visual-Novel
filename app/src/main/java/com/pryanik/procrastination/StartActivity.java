package com.pryanik.procrastination;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.perf.metrics.AddTrace;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.IEvent;
import com.pryanik.procrastination.xml_parser.Node;
import com.pryanik.procrastination.xml_parser.XMLParseDialogues;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.pryanik.procrastination.game_safe.Safekeeping.DEFAULT_SAVE;

public class StartActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final int NEW_GAME = 1;
    public static final int CONTINUE_GAME = 2;
    public static final long TIME_ANIM = 100;

    public static List<Node> nodes;                   // Список всех слайдов
    public static Map<String, List<IEvent>> events;   // Карта событий
    public MediaPlayer mp;
    public SoundPool sp;
    Handler h;
    private ImageView btnContinue; //btnNewGame, btnSettings, btnChapter, btnExit;
    private int soundClick;
    private long startTimeAnim;
    private long timeClick;
    private boolean flag;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    @AddTrace(name = "onStartTrace")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //todo сразу получать сериальзиванные (парсер - отдельная прога)
        if (/*preferences.getBoolean("first_start", true)*/true)
            // При первом запуске
            try {
                XMLParseDialogues xParse = new XMLParseDialogues(
                        getAssets().open("dialogues.xml"),
                        getAssets().list("backgrounds"),
                        getAssets().list("sprites"),
                        getAssets().list("sounds"),
                        getAssets());
                nodes = xParse.getNodes();
                events = xParse.getEvents();
                //Service.serialize(nodes, new File(getFilesDir(), "serialization_nodes"));
                //Service.serialize(events, new File(getFilesDir(), "serialization_events"));
                preferences.edit().putBoolean("first_start", false).apply();
            } catch (IOException e) {
                Service.showErrorDialog(this, e.getLocalizedMessage());
            }
        else
            // При последующих запусках
            try {
                nodes = (List<Node>) Service.deserialize(new File(getFilesDir(), "serialization_nodes"));
                events = (Map<String, List<IEvent>>) Service.deserialize(new File(getFilesDir(), "serialization_events"));
            } catch (Exception e) {
                preferences.edit().putBoolean("first_start", true).apply();
                Service.showErrorDialog(this, e.getLocalizedMessage() +
                        "\nПерезагрузите устройство и повторно запустите приложение!");
            }
        btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnTouchListener(this);
        findViewById(R.id.btnNewGame).setOnTouchListener(this);
        findViewById(R.id.btnSettings).setOnTouchListener(this);
        findViewById(R.id.btnChapter).setOnTouchListener(this);
        findViewById(R.id.btnExit).setOnTouchListener(this);

        //todo звук что то не зацикливается
        mp = MediaPlayer.create(this, R.raw.start_menu_music);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        sp = new SoundPool.Builder().setAudioAttributes(attributes).build();
        soundClick = sp.load(this, R.raw.click_sound, 1);

        // Реклама
        MobileAds.initialize(getApplicationContext(), initializationStatus -> {
        });
        Service.rewardedAd = Service.createAndLoadRewardedAd(getApplicationContext(),
                getString(R.string.ad_unit_id_test));

        h = new Handler();
        flag = false;
    }

    public void onStartClick(@NotNull View view) {
        if (!flag)
            flag = true;
        else if (System.currentTimeMillis() - timeClick < 5000)
            return;
        timeClick = System.currentTimeMillis();

        soundClickPlay();
        int gameStart = 0;
        Intent intent = new Intent(this, MainActivity.class);
        switch (view.getId()) {
            case R.id.btnNewGame:
                gameStart = NEW_GAME;
                break;
            case R.id.btnContinue:
                gameStart = CONTINUE_GAME;
        }
        intent.putExtra("game_start", gameStart);
        startActivity(intent);
        mp.pause();
    }

    public void onSettingsClick(@NotNull View view) {
        soundClickPlay();
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void onChapterClick(@NotNull View view) {
        soundClickPlay();
    }

    public void onExitClick(@NotNull View view) {
        soundClickPlay();
        mp.stop();
        System.exit(0);
    }

    private void soundClickPlay() {
        sp.play(soundClick, 1, 1, 0, 0, 1);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, @NotNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTimeAnim = System.currentTimeMillis();
                h.post(() -> v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(TIME_ANIM).start());
                break;
            case MotionEvent.ACTION_UP:
                long curTimeAnim = System.currentTimeMillis() - startTimeAnim;
                curTimeAnim = TIME_ANIM - curTimeAnim;
                h.postDelayed(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(TIME_ANIM).start(), curTimeAnim < 0 ? 0 : curTimeAnim);
                break;
        }
        return false;
    }

    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        Service.hideNavigation(this);
        if (getSharedPreferences(DEFAULT_SAVE, Context.MODE_PRIVATE).getInt("index", -1) == -1)
            btnContinue.setVisibility(View.GONE);
        else btnContinue.setVisibility(View.VISIBLE);
        mp.start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Service.hideNavigation(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mp.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mp.release();
        sp.release();
    }
}