package com.pryanik.procrastination;

import static com.pryanik.procrastination.activities.SettingsActivity.IS_MUTE_DEF;
import static com.pryanik.procrastination.activities.SettingsActivity.MAX_VOLUME;
import static com.pryanik.procrastination.activities.SettingsActivity.MUSIC_VOL;
import static com.pryanik.procrastination.activities.SettingsActivity.MUS_DEF;
import static com.pryanik.procrastination.activities.SettingsActivity.MUTE;
import static com.pryanik.procrastination.activities.SettingsActivity.RATE_DEF;
import static com.pryanik.procrastination.activities.SettingsActivity.SETTINGS_NAME;
import static com.pryanik.procrastination.activities.SettingsActivity.SOUND_VOL;
import static com.pryanik.procrastination.activities.SettingsActivity.SOU_DEF;
import static com.pryanik.procrastination.activities.SettingsActivity.TEXT_RATE;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;

import com.google.android.gms.ads.MobileAds;
import com.pryanik.procrastination.activities.MainActivity;
import com.pryanik.procrastination.activities.SettingsActivity;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.IEvent;
import com.pryanik.procrastination.xml_parser.Node;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class App extends Application {
    public static List<Node> nodes;                     // Список всех слайдов
    public static Map<String, List<IEvent>> events;     // Карта событий
    public static MediaPlayer mp;                       // Музыка
    public static SoundPool sp;                         // Звуки
    public static float soundVolume;                    // Громкость звука
    public static boolean isMute;
    public static Handler h;                            // Для многопоточности
    private static AudioAttributes attributes;          // Атрибуты для музыки и звуков

    public static void loadMusic(@NotNull Context context, String musStr) {
        try (AssetFileDescriptor afd = context.getAssets().openFd("sounds/" + musStr)) {
            mp.reset();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setAudioAttributes(attributes);
            mp.setLooping(true);
            //mp.setOnPreparedListener(MediaPlayer::start);
            mp.prepare();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static void playSound(@NotNull Context context, String souStr) {
        if (isMute)
            return;
        try (AssetFileDescriptor afd = context.getAssets().openFd("sounds/" + souStr)) {
            sp.play(sp.load(afd, 1),
                    soundVolume, soundVolume, 0, 0, 1);
            //todo выгрузить звук
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static void playMP() {
        if (mp != null && !isMute && !mp.isPlaying())
            mp.start();
    }

    public static void pauseMP() {
        if (mp != null && mp.isPlaying())
            mp.pause();
    }

    public static void releaseSMP() {
        if (mp != null) {
            mp.release();
            mp = null;
        }
        if (sp != null) {
            sp.release();
            sp = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Размерности
        SettingsActivity.INT_HEIGHT_DEF = (int) getResources().getDimension(R.dimen.interface_height);
        SettingsActivity.TXT_SIZE_DEF = (int) getResources().getDimension(R.dimen.text_size);

        // Музыка
        attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        mp = new MediaPlayer();
        sp = new SoundPool.Builder().setAudioAttributes(attributes).build();
        // Применяем настройки
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        MainActivity.TEXT_TIMEOUT = settings.getInt(TEXT_RATE, RATE_DEF);
        float vol = (float) settings.getInt(MUSIC_VOL, MUS_DEF) / MAX_VOLUME;
        mp.setVolume(vol, vol);
        soundVolume = (float) settings.getInt(SOUND_VOL, SOU_DEF) / MAX_VOLUME;
        isMute = settings.getBoolean(MUTE, IS_MUTE_DEF);

        // Реклама
        MobileAds.initialize(getApplicationContext(), initializationStatus -> {
        });
        Service.rewardedAd = Service.createAndLoadRewardedAd(getApplicationContext(),
                getString(R.string.ad_unit_id_test));
        h = new Handler();
    }
}