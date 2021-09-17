package com.pryanik.procrastination.activities;

import static com.pryanik.procrastination.App.events;
import static com.pryanik.procrastination.App.h;
import static com.pryanik.procrastination.App.isMute;
import static com.pryanik.procrastination.App.nodes;
import static com.pryanik.procrastination.App.soundVolume;
import static com.pryanik.procrastination.App.sp;
import static com.pryanik.procrastination.game_safe.Safekeeping.DEFAULT_SAVE;
import static com.pryanik.procrastination.game_safe.Safekeeping.isExistSave;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.perf.metrics.AddTrace;
import com.pryanik.procrastination.App;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.IEvent;
import com.pryanik.procrastination.xml_parser.Node;
import com.pryanik.procrastination.xml_parser.XMLParseDialogues;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StartActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final String LAUNCH_MODE = "launch_mode";
    public static final int NEW_GAME = 1;
    public static final int CONTINUE_GAME = 2;
    public static final int INTERFACE_CUSTOM = 3;
    public static final long TIME_ANIM = 100;
    public static int soundClick;
    private ImageView btnContinue;
    private long startTimeAnim;
    private long timeClick;
    private boolean flag;
    private boolean cntPlayMus;
    private boolean isClickStart;
    private boolean isExistSave;

    public static void soundClickPlay() {
        if (isMute)
            return;
        sp.play(soundClick, soundVolume, soundVolume, 0, 0, 1);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    @AddTrace(name = "onStartTrace")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Service.hideNavigation(this);
        setContentView(R.layout.activity_menu);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //todo сразу получать сериальзиванные (парсер - отдельная прога)
        boolean isFirstStart = true /*preferences.getBoolean("first_start", true) пока не работает*/;
        if (isFirstStart)
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
                Service.serialize(nodes, new File(getFilesDir(), "serialization_nodes"));
                Service.serialize(events, new File(getFilesDir(), "serialization_events"));
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

        App.loadMusic(getApplicationContext(), "start_menu_music.mp3");
        soundClick = sp.load(getApplicationContext(), R.raw.click_sound, 1);
    }

    public void onStartClick(@NotNull View view) {
        // Колхозные штуки с задержкой, чтобы не было даблклика на некоторых устройствах
        if (!flag)
            flag = true;
        else if (System.currentTimeMillis() - timeClick < 2000)
            return;
        timeClick = System.currentTimeMillis();

        soundClickPlay();
        Intent intent = new Intent(this, MainActivity.class);
        //todo сделать красиво
        switch (view.getId()) {
            case R.id.btnNewGame:
                if (isExistSave) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setCancelable(true);
                    builder.setTitle("Новая Игра");
                    builder.setMessage("Хотите начать новую игру? Все сохранения будут утеряны!");
                    builder.setPositiveButton("Продолжить", (dialog, which) -> {
                        intent.putExtra(LAUNCH_MODE, NEW_GAME);
                        startActivity(intent);
                        App.pauseMP();
                        isClickStart = true;
                    });
                    builder.setNegativeButton("Назад", (dialog, which) -> {
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    intent.putExtra(LAUNCH_MODE, NEW_GAME);
                    startActivity(intent);
                    App.pauseMP();
                    isClickStart = true;
                }
                break;
            case R.id.btnContinue:
                intent.putExtra(LAUNCH_MODE, CONTINUE_GAME);
                startActivity(intent);
                App.pauseMP();
                isClickStart = true;
        }
    }

    public void onSettingsClick(@NotNull View view) {
        soundClickPlay();
        startActivity(new Intent(this, SettingsActivity.class));
        cntPlayMus = true;
    }

    public void onChapterClick(@NotNull View view) {
        soundClickPlay();
        cntPlayMus = true;
    }

    public void onExitClick(@NotNull View view) {
        soundClickPlay();
        App.releaseSMP();
        System.exit(0);
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
                // Задержка для того, чтобы анимация проигралась до конца
                h.postDelayed(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(TIME_ANIM).start(), curTimeAnim < 0 ? 0 : curTimeAnim);
                break;
        }
        return false;
    }


    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        //Service.hideNavigation(this);

        isExistSave = isExistSave(DEFAULT_SAVE);
        if (isExistSave)
            btnContinue.setVisibility(View.VISIBLE);
        else btnContinue.setVisibility(View.GONE);

        // Если вернулись из игры
        if (isClickStart) {
            App.loadMusic(getApplicationContext(), "start_menu_music.mp3");
            isClickStart = false;
        }
        App.playMP();
        cntPlayMus = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            Service.hideNavigation(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!cntPlayMus)
            App.pauseMP();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //App.releaseSMP();
    }
}