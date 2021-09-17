package com.pryanik.procrastination.activities;

import static com.pryanik.procrastination.App.nodes;
import static com.pryanik.procrastination.activities.SettingsActivity.INT_HEIGHT;
import static com.pryanik.procrastination.activities.SettingsActivity.INT_HEIGHT_DEF;
import static com.pryanik.procrastination.activities.SettingsActivity.SETTINGS_NAME;
import static com.pryanik.procrastination.activities.SettingsActivity.TXT_SIZE;
import static com.pryanik.procrastination.activities.SettingsActivity.TXT_SIZE_DEF;
import static com.pryanik.procrastination.game_safe.Safekeeping.DEFAULT_SAVE;
import static com.pryanik.procrastination.game_safe.Safekeeping.curId;
import static com.pryanik.procrastination.game_safe.Safekeeping.curIdx;
import static com.pryanik.procrastination.game_safe.Safekeeping.isGameAutoSaved;
import static com.pryanik.procrastination.game_safe.Safekeeping.loadGame;
import static com.pryanik.procrastination.game_safe.Safekeeping.removeAllSafe;
import static com.pryanik.procrastination.game_safe.Safekeeping.resetVal;
import static com.pryanik.procrastination.game_safe.Safekeeping.saveGame;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.pryanik.procrastination.App;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.game_safe.Safekeeping;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.Node;
import com.pryanik.procrastination.xml_parser.XMLParseDialogues;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PreMainActivity extends AppCompatActivity {
    public static Node frame;           // Текущий слайд игры
    public static ConstraintLayout mainLayout;        // Текущий лайаут для анимации кнопок
    public static List<ImageSwitcher> spImgList;   // Список компонентов спрайта
    public static int interfaceHeight;                // Размер нижней панели текста (он же отступ для спрайтов)
    static int startBtnChID = 0x00FFFFFF + 1;   // Особый сдвиг по ID для присваивания кнопкам выбора
    // Спрайты
    static Point size;                      // Размеры экрана
    // Игра
    int launchMode;
    // Текст
    TextView tv;                // Главное поле вывода текста
    TextView prsName;           // Имя персонажа
    ImageView background;       // Задний фон
    ImageView btnMenu;          // Кнопка выхода
    ImageView btnLoad;          // Кнопка загрузки игры
    ImageView btnSave;          // Кнопка сохранения игры
    ImageView btnChapter;       // Кнопка дерева сюжета
    Button[] btnsChoice;        // Кнопки выборов

    protected static Node getNextNode(boolean skipping) {
        isGameAutoSaved = false;
        if (frame.getNext() == null || (frame.getCondition() != null && !skipping)) {
            curIdx++;
            Node out = getNode(curId, curIdx);
            if (out.getId() != null) {
                curId = out.getId();
                curIdx = 0;
            }
            return out;
        } else {
            curId = frame.getNext();
            curIdx = 0;
            return getNode(curId, 0);
        }
    }

    public static Node getNode(@NotNull String id, int idx) {
        if (idx < 0 || idx > nodes.size() - 1)
            throw new IllegalArgumentException("Некорректное значение сдвига!");
        Iterator<Node> iter = nodes.iterator();
        while (iter.hasNext()) {
            Node out = iter.next();
            if (id.equals(out.getId())) {
                if (idx == 0)
                    return out;
                while (idx != 0 && iter.hasNext() && (out = iter.next()).getId() == null)
                    idx--;
                if (idx > 1)
                    throw new IllegalArgumentException("Сдвиг больше текущей ветки! При id=" + id + " и idx=" + idx);
                return out;
            }
        }
        throw new IllegalArgumentException("Не существует node с id=" + id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Service.hideNavigation(this);
        setContentView(R.layout.activity_main);

        // Привязываем объекты к нашей разметке
        mainLayout = findViewById(R.id.activity_main);
        tv = findViewById(R.id.mainTextView);
        prsName = findViewById(R.id.prsName);
        background = findViewById(R.id.background);
        btnMenu = findViewById(R.id.btnMenu);
        btnLoad = findViewById(R.id.btnLoad);
        btnSave = findViewById(R.id.btnSave);
        btnChapter = findViewById(R.id.btnChapter);

        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/OrchestraRegular.ttf");
        tv.setTypeface(face);

        size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        spImgList = new LinkedList<>();
        resetVal();

        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        int textSize = settings.getInt(TXT_SIZE, TXT_SIZE_DEF);
        interfaceHeight = settings.getInt(INT_HEIGHT, INT_HEIGHT_DEF);
        setTxtSize(textSize);
        setInterfaceHeight(interfaceHeight);

        try {
            // Делаем новую игру, узнаём последнее сохранение или кастомизируем интерфейс
            launchMode = getIntent().getIntExtra(StartActivity.LAUNCH_MODE, 0);
            switch (launchMode) {
                case StartActivity.NEW_GAME:
                    removeAllSafe();
                    curId = XMLParseDialogues.ENTRY;
                    frame = getNode(curId, 0);
                    startActivity(new Intent(getApplicationContext(), IntroComicActivity.class));
                    setOnClickListeners();
                    showFrame();
                    break;
                case StartActivity.CONTINUE_GAME:
                    frame = loadGame(getApplicationContext(), DEFAULT_SAVE);
                    setOnClickListeners();
                    showFrame();
                    break;
                case StartActivity.INTERFACE_CUSTOM:
                    customizeInterface(textSize, interfaceHeight, settings);
                    break;
                default:
                    throw new IllegalArgumentException("Критическая ошибка!");
            }
        } catch (Exception e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
    }

    private void setOnClickListeners() {
        tv.setOnClickListener(this::tvClick);
        btnMenu.setOnClickListener(this::btnExitClick);
        btnLoad.setOnClickListener(this::btnLoadClick);
        btnSave.setOnClickListener(this::btnSaveClick);
        btnChapter.setOnClickListener(this::btnChapterClick);
    }

    private void customizeInterface(int initProgressTxt, int initProgressInt, SharedPreferences sets) {
        //todo вынести весь текст в strings.xml
        tv.setText("Я достал из мешка огромный револьвер, который начал переливаться матовым блеском в свете настенных ламп. " +
                "Мои шаловливые пальцы, начали ощупывать каждый изгиб в поисках кнопок или механизмов. А вот и оно...");

        SeekBar sbSize = findViewById(R.id.sb_size);
        sbSize.setMax(size.y / 2); // Максимальная высота - половина экрана
        sbSize.setProgress(initProgressInt);
        sbSize.setVisibility(View.VISIBLE);

        SeekBar sbTxt = findViewById(R.id.sb_txt); //todo создать
        sbTxt.setMax(TXT_SIZE_DEF * 2); // Макс. размер текста - в 2 раза больше обычного
        sbTxt.setProgress(initProgressTxt);
        sbTxt.setVisibility(View.VISIBLE);

        //todo создать
        findViewById(R.id.txt_int).setVisibility(View.VISIBLE);
        findViewById(R.id.txt_text).setVisibility(View.VISIBLE);

        fillBtnsChArr();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle("Предупреждение");
        builder.setMessage("• Двигайте левый ползунок, чтобы увеличить/уменьшить интерфейс,\n" +
                "а правый ползунок - для регулировки размера текста.\n" +
                "• Регулируйте так, чтобы кнопки выбора не заползали на текстовое поле и весь текст полностью помещался на экране!");
        builder.setNegativeButton("Понятно", (dialog, which) -> showBtnsChoice());
        AlertDialog dialog = builder.create();
        dialog.show();

        sbTxt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTxtSize(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                sets.edit().putInt(TXT_SIZE, seekBar.getProgress()).apply();
            }
        });
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setInterfaceHeight(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                sets.edit().putInt(INT_HEIGHT, seekBar.getProgress()).apply();
            }
        });
    }

    private void fillBtnsChArr() {
        btnsChoice = new Button[XMLParseDialogues.MAX_NUM_CHOICE];
        for (int i = 0; i < btnsChoice.length; i++) {
            btnsChoice[i] = new Button(getApplicationContext());
            btnsChoice[i].setId(startBtnChID + i);
            btnsChoice[i].setLayoutParams(new ConstraintLayout.LayoutParams(
                    0, ConstraintLayout.LayoutParams.WRAP_CONTENT));
            btnsChoice[i].setBackgroundColor(getResources().getColor(R.color.transparentGray));
            btnsChoice[i].setTextColor(getResources().getColor(R.color.white));
            btnsChoice[i].setVisibility(View.INVISIBLE);
            btnsChoice[i].setText("Выбор " + (i + 1));
            mainLayout.addView(btnsChoice[i]);

            ConstraintSet set = new ConstraintSet();
            set.clone(mainLayout);
            set.constrainPercentWidth(btnsChoice[i].getId(), 0.4f);
            set.connect(btnsChoice[i].getId(), ConstraintSet.BOTTOM, R.id.mainTextView, ConstraintSet.TOP);
            set.connect(btnsChoice[i].getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(btnsChoice[i].getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            set.applyTo(mainLayout);
        }
    }

    private void showBtnsChoice() {
        ConstraintSet set = new ConstraintSet();
        set.clone(mainLayout);
        // Отдельно привязываем верхнюю кнопку
        set.connect(btnsChoice[0].getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        // Затем все остальные к предыдущей
        for (int i = 1; i < btnsChoice.length; i++) {
            // Верх текущей к низу предыдущей
            set.connect(btnsChoice[i].getId(), ConstraintSet.TOP, btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM);
            // Низ предыдущей к верху текущей
            set.connect(btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM, btnsChoice[i].getId(), ConstraintSet.TOP);
        }
        set.applyTo(mainLayout);
        for (Button button : btnsChoice) button.setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(mainLayout);
    }


    //------------------------------------- Функции Игры -------------------------------------//

    private void setTxtSize(int txtSize) {
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtSize);
        prsName.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtSize);
    }

    private void setInterfaceHeight(int height) {
        tv.getLayoutParams().height = height;
        int heightForBtns = (int) (height * 0.4f);
        btnMenu.getLayoutParams().height = heightForBtns;
        btnLoad.getLayoutParams().height = heightForBtns;
        btnSave.getLayoutParams().height = heightForBtns;
        btnChapter.getLayoutParams().height = (int) (height * 0.25f);
        prsName.getLayoutParams().height = (int) (height * 0.35f);
        mainLayout.requestLayout();
    }

    public void tvClick(View v) {
    }

    public void showFrame() {
    }

    protected void setBackground(String image, String music) {
    }

    public void btnChapterClick(@NotNull View v) {
    }

    public void btnLoadClick(@NotNull View v) {
    }

    public void btnSaveClick(@NotNull View v) {
    }

    public void btnExitClick(@NotNull View v) {
    }


    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        App.playMP();
        if (launchMode != StartActivity.INTERFACE_CUSTOM &&
                Safekeeping.isRequestLoad &&
                ConservationActivity.isSuccessLoad)
            showFrame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!IntroComicActivity.isComicDisplayed &&
                !ConservationActivity.isBtnSaveClick &&
                !SettingsActivity.isInterfaceCustomStart)
            App.pauseMP();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (launchMode != StartActivity.INTERFACE_CUSTOM &&
                !isGameAutoSaved)
            saveGame(getApplicationContext(), DEFAULT_SAVE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            Service.hideNavigation(this);
    }

    /*@Override
    //todo надо ли?
    protected void onDestroy() {
        super.onDestroy();
        App.releaseSMP();
    }*/
}