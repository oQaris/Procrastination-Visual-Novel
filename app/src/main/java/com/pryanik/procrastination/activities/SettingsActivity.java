package com.pryanik.procrastination.activities;

import static com.pryanik.procrastination.App.isMute;
import static com.pryanik.procrastination.App.mp;
import static com.pryanik.procrastination.App.pauseMP;
import static com.pryanik.procrastination.App.playMP;
import static com.pryanik.procrastination.App.soundVolume;
import static com.pryanik.procrastination.activities.StartActivity.soundClickPlay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pryanik.procrastination.App;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.support.Service;

public class SettingsActivity extends AppCompatActivity {
    // Имена сохранений
    public static final String SETTINGS_NAME = "settings";
    public static final String TEXT_RATE = "rate";
    public static final String MUSIC_VOL = "music";
    public static final String SOUND_VOL = "sound";
    public static final String AUTO_SAVE = "auto_save";
    public static final String MUTE = "mute";
    public static final String INT_HEIGHT = "interface_height";
    public static final String TXT_SIZE = "txt_size";

    // Значения по-умолчанию
    public static final int MAX_VOLUME = 100;
    public static final int RATE_DEF = 20;
    public static final int MUS_DEF = 50;
    public static final int SOU_DEF = 50;
    public static final boolean IS_AUTO_DEF = true;
    public static final boolean IS_MUTE_DEF = false;
    // Постфиксы
    private static final String MILLIS_STR = " мс/символ";
    private static final String PERCENT_STR = "%";
    // Размерности устанавливаются после запуска
    public static int INT_HEIGHT_DEF;
    public static int TXT_SIZE_DEF;
    public static boolean isInterfaceCustomStart;
    private TextView txtRateVal;
    private TextView txtMusProgress;
    private TextView txtSouProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Service.hideNavigation(this);
        setContentView(R.layout.activity_settings);

        txtRateVal = findViewById(R.id.txt_rate_val);
        final SeekBar seekRate = findViewById(R.id.seek_rate);
        txtMusProgress = findViewById(R.id.txt_music_progress);
        final SeekBar seekMus = findViewById(R.id.seek_music);
        txtSouProgress = findViewById(R.id.txt_sound_progress);
        final SeekBar seekSou = findViewById(R.id.seek_sound);
        Switch swAuto = findViewById(R.id.sw_auto_save);
        CheckBox cbMute = findViewById(R.id.ch_mute);

        Button btnInterfaceCustom = findViewById(R.id.btn_interface_custom);
        btnInterfaceCustom.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(StartActivity.LAUNCH_MODE, StartActivity.INTERFACE_CUSTOM);
            startActivity(intent);
            isInterfaceCustomStart = true;
        });

        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        // Инициализируем (по-умолчанию, либо из сохранений)
        int curProgress = settings.getInt(TEXT_RATE, RATE_DEF);
        seekRate.setProgress(curProgress);
        txtRateVal.setText(curProgress + MILLIS_STR);
        curProgress = settings.getInt(MUSIC_VOL, MUS_DEF);
        seekMus.setProgress(curProgress);
        txtMusProgress.setText(curProgress + PERCENT_STR);
        curProgress = settings.getInt(SOUND_VOL, SOU_DEF);
        seekSou.setProgress(curProgress);
        txtSouProgress.setText(curProgress + PERCENT_STR);
        swAuto.setChecked(settings.getBoolean(AUTO_SAVE, IS_AUTO_DEF));
        isMute = settings.getBoolean(MUTE, IS_MUTE_DEF);
        cbMute.setChecked(isMute);
        seekMus.setEnabled(!isMute);
        seekSou.setEnabled(!isMute);

        seekRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtRateVal.setText(progress + MILLIS_STR);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settings.edit().putInt(TEXT_RATE, seekBar.getProgress()).apply();
            }
        });

        seekMus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtMusProgress.setText(progress + PERCENT_STR);
                //final float vol = (float) (1 - Math.log(MAX_VOLUME - progress) / Math.log(MAX_VOLUME));
                final float vol = (float) progress / MAX_VOLUME;
                mp.setVolume(vol, vol);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settings.edit().putInt(MUSIC_VOL, seekBar.getProgress()).apply();
            }
        });

        seekSou.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtSouProgress.setText(progress + PERCENT_STR);
                soundVolume = (float) progress / MAX_VOLUME;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settings.edit().putInt(SOUND_VOL, seekBar.getProgress()).apply();
                soundClickPlay();
            }
        });

        swAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean(AUTO_SAVE, isChecked).apply();
            soundClickPlay();
        });

        cbMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isMute = isChecked;
            seekMus.setEnabled(!isChecked);
            seekSou.setEnabled(!isChecked);
            settings.edit().putBoolean(MUTE, isChecked).apply();
            if (!isChecked) {
                soundClickPlay();
                playMP();
            } else pauseMP();
        });
    }


    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        //Service.hideNavigation(this);
        App.playMP();
        isInterfaceCustomStart = false;
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
        if (!isInterfaceCustomStart)
            App.pauseMP();
    }
}