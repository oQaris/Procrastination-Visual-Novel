package com.pryanik.procrastination;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pryanik.procrastination.game_safe.SaveAdapter;
import com.pryanik.procrastination.game_safe.SaveItem;
import com.pryanik.procrastination.support.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.pryanik.procrastination.game_safe.Safekeeping.SAVE_PREFIX;
import static com.pryanik.procrastination.game_safe.Safekeeping.curId;
import static com.pryanik.procrastination.game_safe.Safekeeping.curImg;
import static com.pryanik.procrastination.game_safe.Safekeeping.isRequestLoad;
import static com.pryanik.procrastination.game_safe.Safekeeping.loadGame;
import static com.pryanik.procrastination.game_safe.Safekeeping.removeSafe;
import static com.pryanik.procrastination.game_safe.Safekeeping.saveGame;
import static com.pryanik.procrastination.support.Service.showAdsDialog;

public class ConservationActivity extends AppCompatActivity {
    public static SaveAdapter adapter;
    public static boolean isSuccessLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Service.hideNavigation(this);
        setContentView(R.layout.activity_conservation);
        isSuccessLoad = false;

        TextView txtSaveLoad = findViewById(R.id.txt_save_load);
        if (isRequestLoad)
            txtSaveLoad.setText("Загрузка");
        else txtSaveLoad.setText("Сохранение");

        // получаем элемент ListView
        ListView listView = findViewById(R.id.countriesList);
        // устанавливаем для списка адаптер
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            // Загружаем
            if (isRequestLoad) {
                PreMainActivity.frame = loadGame(this, SAVE_PREFIX + position);
                isSuccessLoad = true;
                this.finish();

            }  // Сохраняем
            else {
                // Добавляем новый слот
                if (position == adapter.getCount() - 1) {
                    showAdsDialog(this);

                    // Далее - в случае согласия
                    saveGame(this, SAVE_PREFIX + adapter.getCount());
                    DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
                    try {
                        adapter.insert(new SaveItem(curId, dateFormat.format(new Date()),
                                        BitmapFactory.decodeStream(getAssets().open("backgrounds/" + curImg))),
                                adapter.getCount() - 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } // Заменяем
                else {
                    saveGame(this, SAVE_PREFIX + (position + 1));
                    removeSafe(position);

                    adapter.remove(adapter.getItem(position));
                    DateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
                    try {
                        adapter.insert(new SaveItem(curId, dateFormat.format(new Date()),
                                BitmapFactory.decodeStream(getAssets().open(
                                        "backgrounds/" + curImg))), position);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                adapter.notifyDataSetChanged();
            }
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