package com.pryanik.procrastination.support;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.pryanik.procrastination.R;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Service {
    public static RewardedAd rewardedAd;

    public static void showErrorDialog(Activity activity, String error) {
        final AlertDialog aboutDialog = new AlertDialog.Builder(activity)
                .setMessage(error)
                .setPositiveButton("OK", (dialog, which) -> activity.finish()).create();
        aboutDialog.show();
    }

    public static void showAdsDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle("Реклама");
        builder.setMessage("Хотите посмотреть рекламу, чтобы активировать слот сохранения?\n(игра уже сохранена, ибо это тестовая версия, но потом так низя буит)");
        builder.setPositiveButton("Смотреть", (dialog, which) -> Service.showAd(activity));
        builder.setNegativeButton("Отменить", (dialog, which) -> {
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void hideNavigation(@NotNull Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    public static void serialize(Object obj, File f) throws IOException {
        try (ObjectOutput out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(f)))) {
            out.writeObject(obj);
        }
    }

    public static Object deserialize(File f) throws IOException, ClassNotFoundException {
        try (ObjectInput in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            return in.readObject();
        }
    }

    @NotNull
    public static RewardedAd createAndLoadRewardedAd(Context context, String adUnitId) {
        RewardedAd rewardedAd = new RewardedAd(context, adUnitId);
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                // Ad successfully loaded.
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                if (errorCode == AdRequest.ERROR_CODE_NETWORK_ERROR)
                    Toast.makeText(context,
                            "Отсутствует подключение к сети!", Toast.LENGTH_SHORT).show();
                else if (errorCode == AdRequest.ERROR_CODE_NO_FILL)
                    Toast.makeText(context,
                            "Вы посмотрели всю доступную рекламу!", Toast.LENGTH_SHORT).show();
                /*else Toast.makeText(context,
                            "Что-то пошло не так, попробуйте позже...", Toast.LENGTH_SHORT).show();*/
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
        return rewardedAd;
    }

    public static void showAd(Activity activity) {
        if (rewardedAd.isLoaded()) {
            RewardedAdCallback adCallback = new RewardedAdCallback() {
                @Override
                public void onRewardedAdOpened() {
                    // Приостановить звук и т.п.
                }

                @Override
                public void onRewardedAdClosed() {
                    // Включить всё обратно
                    // Рекомендуется начать загружать следующее объявление
                    rewardedAd = Service.createAndLoadRewardedAd(activity,
                            activity.getString(R.string.ad_unit_id_test));
                }

                @Override
                public void onUserEarnedReward(@NonNull RewardItem reward) {
                    // Тут вознаграждение
                }

                @Override
                public void onRewardedAdFailedToShow(int errorCode) {
                    // В случае ошибки
                    Toast.makeText(activity,
                            "Какая то ошибка!", Toast.LENGTH_SHORT).show();
                }
            };
            rewardedAd.show(activity, adCallback);
        } else
            Toast.makeText(activity,
                    "Реклама ещё не загружена!", Toast.LENGTH_SHORT).show();
    }
}