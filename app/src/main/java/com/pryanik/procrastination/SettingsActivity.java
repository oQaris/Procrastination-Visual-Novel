package com.pryanik.procrastination;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pryanik.procrastination.support.Service;

public class SettingsActivity extends AppCompatActivity {
    // Имена сохранений
    public static final String SETTINGS_NAME = "Settings";
    public static final String TEXT_RATE = "rate";
    public static final String MUSIC_VOL = "music";
    public static final String SOUND_VOL = "sound";
    public static final String ADS = "ads";

    // Значения по-умолчанию
    public static final int RATE_DEF = 20;
    public static final int MUS_DEF = 50;
    public static final int SOU_DEF = 50;
    public static final boolean IS_ADS = true;

    // Постфиксы
    private static final String MILLIS_STR = " мс/символ";
    private static final String PERCENT_STR = "%";

    private TextView txtRateVal;
    private TextView txtMusProgress;
    private TextView txtSouProgress;
    private Switch swAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        txtRateVal = findViewById(R.id.txt_rate_val);
        final SeekBar seekRate = findViewById(R.id.seek_rate);
        txtMusProgress = findViewById(R.id.txt_music_progress);
        final SeekBar seekMus = findViewById(R.id.seek_music);
        txtSouProgress = findViewById(R.id.txt_sound_progress);
        final SeekBar seekSou = findViewById(R.id.seek_sound);
        swAds = findViewById(R.id.sw_ads);

        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        // Инициализируем (по-умолчанию, либо из сохранений)
        int curProgress = settings.getInt(SettingsActivity.TEXT_RATE, RATE_DEF);
        seekRate.setProgress(curProgress);
        txtRateVal.setText(curProgress + MILLIS_STR);
        curProgress = settings.getInt(SettingsActivity.MUSIC_VOL, MUS_DEF);
        seekMus.setProgress(curProgress);
        txtMusProgress.setText(curProgress + PERCENT_STR);
        curProgress = settings.getInt(SettingsActivity.SOUND_VOL, SOU_DEF);
        seekSou.setProgress(curProgress);
        txtSouProgress.setText(curProgress + PERCENT_STR);
        swAds.setChecked(settings.getBoolean(SettingsActivity.ADS, IS_ADS));

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
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settings.edit().putInt(SOUND_VOL, seekBar.getProgress()).apply();
            }
        });

        swAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean(ADS, isChecked).apply();
        });
    }


    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        Service.hideNavigation(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Service.hideNavigation(this);
    }
}