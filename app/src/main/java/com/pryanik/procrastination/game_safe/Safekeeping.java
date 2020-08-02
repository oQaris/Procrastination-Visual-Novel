package com.pryanik.procrastination.game_safe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.pryanik.procrastination.ConservationActivity;
import com.pryanik.procrastination.PreMainActivity;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.xml_parser.Node;
import com.pryanik.procrastination.xml_parser.XMLParseDialogues;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.pryanik.procrastination.PreMainActivity.frame;

//todo сделать без обращения к frame

public class Safekeeping {
    public static final String SAVE_PREFIX = "sv-";
    public static final String DEFAULT_SAVE = SAVE_PREFIX + "0";
    public static boolean isRequestLoad = false;
    // Сохранения
    public static int curIdx;                 // Текущий индекс в ветке
    public static String curId;               // Id текущего слайда
    public static String curImg;              // Текущий бэкграунд
    public static String curMus;              // Текущая музыка
    public static Set<String> passNodeId;     // Пройденные Id
    private static String pathData = "/data/data/com.pryanik.procrastination/shared_prefs/";

    public static void saveGame(@NotNull Context context, String strSave) {
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
        editor.apply();
        Toast.makeText(context.getApplicationContext(),
                "Игра сохранена!", Toast.LENGTH_SHORT).show();
    }

    @NotNull
    public static Node loadGame(@NotNull Context context, String strSave) {
        SharedPreferences saves = context.getSharedPreferences(strSave, Context.MODE_PRIVATE);
        curIdx = saves.getInt("index", 0);
        curId = saves.getString("frame", XMLParseDialogues.ENTRY);
        curImg = saves.getString("image", "error.png");
        curMus = saves.getString("music", null);
        passNodeId = saves.getStringSet("set", new HashSet<>());
        Node frame = PreMainActivity.getNode(curId, curIdx);
        frame.setBranchName(saves.getString("branch", ""));
        // Отдельно устанавливаем задний фон
        if (frame.getBackground() == null) {
            //todo не изменять frame
            frame.setBackground(curImg);
        }
        if (frame.getMusic() == null) {
            frame.setMusic(curMus);
        }
        Toast.makeText(context.getApplicationContext(),
                "Игра загружена!", Toast.LENGTH_SHORT).show();
        return frame;
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

    public static void loadClick(Context context) throws IOException {
        isRequestLoad = true;
        fillSaveName(context);
        SharedPreferences sp = context.getSharedPreferences(DEFAULT_SAVE, Context.MODE_PRIVATE);
        ConservationActivity.adapter.insert(new SaveItem(sp.getString("branch", null) + "  (auto-save)",
                sp.getString("date", null),
                BitmapFactory.decodeStream(context.getAssets().open("backgrounds/" + sp.getString("image", "error.png")))), 0);
    }

    public static void saveClick(Context context) throws IOException {
        isRequestLoad = false;
        fillSaveName(context);
        ConservationActivity.adapter.add(new SaveItem("Открыть новый слот сохранения?", "Нажми и посмотри рекламу!",
                BitmapFactory.decodeResource(context.getResources(), R.drawable.interface_save)));
    }

    public static void fillSaveName(Context context) throws IOException {
        //todo Костыли!
        ConservationActivity.adapter = new SaveAdapter(context,
                R.layout.list_item, new ArrayList<>());

        File savesDir = new File(pathData);
        String[] savesStr = savesDir.list((dir, name) -> name.matches(SAVE_PREFIX + "[1-9]\\d*\\.xml"));

        assert savesStr != null;
        Arrays.sort(savesStr, (o1, o2) -> {
            int dif = o1.length() - o2.length();
            return dif == 0 ? o1.compareTo(o2) : dif;
        });

        for (String svStr : savesStr) {
            SharedPreferences sp = context.getSharedPreferences(svStr.substring(0, svStr.length() - 4),
                    Context.MODE_PRIVATE);

            ConservationActivity.adapter.add(new SaveItem(sp.getString("branch", null),
                    sp.getString("date", null),
                    BitmapFactory.decodeStream(context.getAssets().open("backgrounds/" +
                            sp.getString("image", "error.png")))));
        }
    }
}