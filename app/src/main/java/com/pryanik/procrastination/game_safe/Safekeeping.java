package com.pryanik.procrastination.game_safe;

import static com.pryanik.procrastination.activities.ConservationActivity.adapter;
import static com.pryanik.procrastination.activities.PreMainActivity.frame;
import static com.pryanik.procrastination.activities.PreMainActivity.spImgList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.widget.ImageSwitcher;
import android.widget.Toast;

import com.pryanik.procrastination.R;
import com.pryanik.procrastination.activities.MainActivity;
import com.pryanik.procrastination.activities.PreMainActivity;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.Node;
import com.pryanik.procrastination.xml_parser.Sprite;
import com.pryanik.procrastination.xml_parser.XMLParseDialogues;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

//todo сделать без обращения к frame

public class Safekeeping {
    public static final String SAVE_PREFIX = "sv-";
    public static final String DEFAULT_SAVE = SAVE_PREFIX + "0";
    public static boolean isRequestLoad = false;
    private static final String pathData = "/data/data/com.pryanik.procrastination/shared_prefs/";
    public static boolean isGameAutoSaved = false;
    public static int curIdx;                 // Текущий индекс в ветке
    public static String curId;               // Id текущего слайда
    public static String curImg;              // Текущий бэкграунд
    public static String curMus;              // Текущая музыка
    public static Set<String> passNodeId;     // Пройденные Id
    // Сохранения
    public static String branch;              // Текущая ветка сохранений

    public static void saveGame(@NotNull Context context, String strSave) {
        if (DEFAULT_SAVE.equals(strSave))
            isGameAutoSaved = true;
        SharedPreferences saves = context.getSharedPreferences(strSave, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = saves.edit();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
        editor.putString("date", dateFormat.format(new Date()));
        editor.putString("frame", curId);
        editor.putInt("index", curIdx);
        editor.putString("image", curImg);
        editor.putString("music", curMus);
        editor.putStringSet("set", passNodeId);
        editor.putString("branch", frame.getBranchName());
        // Сохраняем позиции страйтов
        for (ImageSwitcher sw : spImgList)
            editor.putFloat(((Sprite) sw.getTag()).getSrc(), sw.getTranslationX());
        editor.apply();
        Toast.makeText(context.getApplicationContext(),
                "Игра сохранена!", Toast.LENGTH_SHORT).show();
    }

    @NotNull
    public static Node loadGame(@NotNull Context context, String strSave) throws IOException {
        isGameAutoSaved = false;
        SharedPreferences saves = context.getSharedPreferences(strSave, Context.MODE_PRIVATE);
        curIdx = saves.getInt("index", 0);
        curId = saves.getString("frame", XMLParseDialogues.ENTRY);
        curImg = saves.getString("image", "error.png");
        curMus = saves.getString("music", null);
        passNodeId = saves.getStringSet("set", new HashSet<>());
        Node frame = PreMainActivity.getNode(curId, curIdx);
        frame.setBranchName(saves.getString("branch", ""));
        // Отдельно устанавливаем задний фон
        if (frame.getBackground() == null)
            //todo не изменять frame
            frame.setBackground(curImg);
        if (frame.getMusic() == null)
            frame.setMusic(curMus);

        List<Sprite> spriteList = new LinkedList<>();
        for (Sprite sp : frame.getSprites())
            spriteList.add(new Sprite(sp.getSrc(), sp.getFeel(),
                    saves.getFloat(sp.getSrc(), 0), // Получаем из сохранений абсолютное положение картинки по ширине, а не в процентах
                    sp.isAct(), sp.getHeight(), sp.getWidth()));
        MainActivity.spriteAnim(context, spriteList, true);

        Toast.makeText(context.getApplicationContext(),
                "Игра загружена!", Toast.LENGTH_SHORT).show();
        return frame;
    }

    public static boolean isExistSave(String strSave) {
        File save = new File(pathData + strSave + ".xml");
        return save.exists();
    }

    public static boolean removeSafe(int numSv) {
        File f = new File(pathData + SAVE_PREFIX + numSv + ".xml");
        return f.delete();
    }

    public static boolean removeAllSafe() {
        File savesDir = new File(pathData);
        File[] arrSaves = savesDir.listFiles();
        assert arrSaves != null;
        boolean isAllDel = true;
        for (File f : arrSaves)
            if (f.getName().startsWith(SAVE_PREFIX))
                isAllDel &= f.delete();
        return isAllDel;
    }

    public static void resetVal() {
        curIdx = 0;
        curId = null;
        curImg = null;
        curMus = null;
        branch = null;
        passNodeId = new HashSet<>();
    }

    public static void loadClick(Activity activity) {
        isRequestLoad = true;
        fillSaveName(activity);
        SharedPreferences sp = activity.getSharedPreferences(DEFAULT_SAVE, Context.MODE_PRIVATE);
        adapter.insert(generateSaveItem(
                sp.getString("branch", null) + "  (auto-save)",
                sp.getString("date", null),
                sp.getString("image", "error.png"), activity), 0);
    }

    public static void saveClick(Activity activity) {
        isRequestLoad = false;
        fillSaveName(activity);
        adapter.add(new SaveItem("Открыть новый слот сохранения?", "Нажми и посмотри рекламу!",
                BitmapFactory.decodeResource(activity.getResources(), R.drawable.interface_save)));
    }

    public static void fillSaveName(Activity activity) {
        adapter = new SaveAdapter(activity,
                R.layout.list_item, new ArrayList<>());

        File savesDir = new File(pathData);
        String[] savesStr = savesDir.list((dir, name) -> name.matches(SAVE_PREFIX + "[1-9]\\d*\\.xml"));

        assert savesStr != null;
        //todo заменить на Comparator.comparingInt(String::length).thenComparing(o -> o) в новой версии
        Arrays.sort(savesStr, (o1, o2) -> {
            int dif = o1.length() - o2.length();
            return dif == 0 ? o1.compareTo(o2) : dif;
        });

        for (String svStr : savesStr) {
            SharedPreferences sp = activity.getSharedPreferences(svStr.substring(0, svStr.length() - 4),
                    Context.MODE_PRIVATE);

            adapter.add(generateSaveItem(
                    sp.getString("branch", null),
                    sp.getString("date", null),
                    sp.getString("image", "error.png"), activity));
        }
    }

    public static void insertSaveName(@NotNull Activity activity, int position) {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
        adapter.insert(generateSaveItem(
                frame.getBranchName(),
                dateFormat.format(new Date()),
                curImg, activity), position);
    }

    @Nullable
    @Contract("_, _, _, _ -> new")
    private static SaveItem generateSaveItem(String str, String subStr, String picAsset, @NotNull Activity activity) {
        try {
            return new SaveItem(str, subStr,
                    BitmapFactory.decodeStream(activity.getAssets().open("backgrounds/" + picAsset)));
        } catch (IOException e) {
            Service.showErrorDialog(activity, e.getLocalizedMessage());
        }
        return null;
    }
}