package com.pryanik.procrastination.activities;

import static com.pryanik.procrastination.game_safe.Safekeeping.SAVE_PREFIX;
import static com.pryanik.procrastination.game_safe.Safekeeping.insertSaveName;
import static com.pryanik.procrastination.game_safe.Safekeeping.isRequestLoad;
import static com.pryanik.procrastination.game_safe.Safekeeping.loadGame;
import static com.pryanik.procrastination.game_safe.Safekeeping.removeSafe;
import static com.pryanik.procrastination.game_safe.Safekeeping.saveGame;
import static com.pryanik.procrastination.support.Service.showAdsDialog;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pryanik.procrastination.App;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.game_safe.SaveAdapter;
import com.pryanik.procrastination.support.Service;

import java.io.IOException;

public class ConservationActivity extends AppCompatActivity {
    public static SaveAdapter adapter;
    public static boolean isSuccessLoad;
    public static boolean isBtnSaveClick;

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

        ListView listView = findViewById(R.id.countriesList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            // Загружаем
            if (isRequestLoad) {
                try {
                    PreMainActivity.frame = loadGame(getApplicationContext(), SAVE_PREFIX + position);
                } catch (IOException e) {
                    Service.showErrorDialog(this, e.getLocalizedMessage());
                }
                isSuccessLoad = true;
                this.finish();

            }  // Сохраняем
            else {
                // Добавляем новый слот
                if (position == adapter.getCount() - 1) {
                    showAdsDialog(this);
                    // Далее - в случае согласия
                    saveGame(getApplicationContext(), SAVE_PREFIX + adapter.getCount());
                    insertSaveName(this, adapter.getCount() - 1);

                } // Заменяем
                else {
                    saveGame(getApplicationContext(), SAVE_PREFIX + (position + 1));
                    removeSafe(position);
                    adapter.remove(adapter.getItem(position));
                    insertSaveName(this, position);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        //Service.hideNavigation(this);
        App.playMP();
        isBtnSaveClick = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.pauseMP();
        isBtnSaveClick = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            Service.hideNavigation(this);
    }
}