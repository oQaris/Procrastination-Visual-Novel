package com.pryanik.procrastination;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.transition.TransitionManager;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.pryanik.procrastination.game_safe.Safekeeping;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.Choice;
import com.pryanik.procrastination.xml_parser.IEvent;
import com.pryanik.procrastination.xml_parser.Sound;
import com.pryanik.procrastination.xml_parser.Sprite;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.pryanik.procrastination.game_safe.Safekeeping.DEFAULT_SAVE;
import static com.pryanik.procrastination.game_safe.Safekeeping.curId;
import static com.pryanik.procrastination.game_safe.Safekeeping.curIdx;
import static com.pryanik.procrastination.game_safe.Safekeeping.curImg;
import static com.pryanik.procrastination.game_safe.Safekeeping.curMus;
import static com.pryanik.procrastination.game_safe.Safekeeping.passNodeId;
import static com.pryanik.procrastination.game_safe.Safekeeping.saveGame;

public class MainActivity extends PreMainActivity {
    static long TEXT_TIMEOUT;
    private boolean isPhraseDisplayed = false;
    Runnable stopText = () -> isPhraseDisplayed = false;
    private String curPhrase;
    private int idxPhrase;
    private int idxChar;
    Runnable showText = () -> {
        tv.append(curPhrase.substring(idxChar, idxChar + 1));
        idxChar++;
    };
    private List<Choice> chsTrue;
    Runnable postTextOutput = () -> {
        isPhraseDisplayed = false;
        // Выводим кнопки выборов
        if (frame.getChoices() != null) {
            chsTrue = new ArrayList<>(frame.getChoices().size());
            for (Choice ch : frame.getChoices())
                if (interpretCondition(ch.getCondition()))
                    chsTrue.add(ch);
            // Запускаем анимацию
            changeConstraints();
        }
    };

    public void tvClick(View v) {
        debag.setText("Отладка:\n" +
                "\ncurID = " + curId +
                "\ncurIdx = " + curIdx +
                "\ncurMus = " + curMus +
                "\ncurImg = " + curImg +
                "\nbranch = " + frame.getBranchName() +
                "\nset: " + passNodeId);

        // Если нажали кнопку когда выводится текст
        if (isPhraseDisplayed) {
            h.removeCallbacks(showText);
            h.removeCallbacks(stopText);
            isPhraseDisplayed = false;
            tv.setText(merger(frame.getPhrases()));
            idxPhrase = frame.getPhrases().size();
            h.removeCallbacks(postTextOutput);
            h.post(postTextOutput);
            return;
        } // Или когда есть ещё фразы
        else if (idxPhrase < frame.getPhrases().size()) {
            if (frame.getEventsMap().containsKey(idxPhrase))
                showEvents(events.get(frame.getEventsMap().get(idxPhrase)));
            tv.append(" ");
            smoothlyPrint(frame.getPhrases());
            return;
        } // Или когда есть выборы
        else if (frame.getChoices() != null)
            return;

        // Делаем пройденным
        if (frame.getId() != null)
            passNodeId.add(frame.getId());
        // Получаем следующий узел
        frame = getNextNode();
        showFrame();
    }

    private void showEvents(@NotNull List<IEvent> eventList) {
        List<Sprite> sprites = new ArrayList<>();
        try {
            for (IEvent event : eventList)
                if (event instanceof Sprite) {
                    sprites.add((Sprite) event);
                } else if (event instanceof Sound)
                    sp.play(sp.load(getAssets().openFd("sounds/" + ((Sound) event).getSrc()), 1),
                            1, 1, 0, 0, 1);
            //todo выгрузить звук
            spriteAnim(sprites);
        } catch (IOException e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
    }

    private boolean containsSrc(String src, @NotNull List<Sprite> sprites) {
        for (Sprite sp : sprites) {
            if (sp.getSrc().equals(src))
                return true;
        }
        return false;
    }

    private float generateSpriteLayout(@NotNull Sprite sp) {
        return sp.getPos() * size.x - sp.getPos() * ((size.y - tv.getHeight()) * (float) sp.getWidth() / sp.getHeight());
    }

    private void spriteAnim(List<Sprite> spriteList) throws IOException {
        // Удаляем неиспользуемые
        Iterator<Map.Entry<String, ImageSwitcher>> i = spImgList.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, ImageSwitcher> entry = i.next();
            if (!containsSrc(entry.getKey(), spriteList)) {
                constraintLayout.removeView(entry.getValue());
                i.remove();
            }
        }
        // Преобразуем или добавляем спрайты
        for (Sprite sp : spriteList) {
            ImageSwitcher sprite = spImgList.get(sp.getSrc());
            Drawable draw = Drawable.createFromStream(getAssets().open(sp.name()), null);
            if (sprite == null) {
                // Добавлям новый спрайт
                sprite = new ImageSwitcher(getApplicationContext());
                // Задаём параметры лайаута самого ImageSwitcher'a
                ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.margin_bottom));
                sprite.setLayoutParams(layoutParams);
                // Ставим основные параметры загружаемой картинки
                sprite.setFactory(() -> {
                    ImageView image = new ImageView(getApplicationContext());
                    image.setScaleType(ImageView.ScaleType.FIT_START);
                    image.setLayoutParams(new ImageSwitcher.LayoutParams(
                            ImageSwitcher.LayoutParams.MATCH_PARENT,
                            ImageSwitcher.LayoutParams.MATCH_PARENT));
                    return image;
                });
                sprite.setId(View.generateViewId());
                // Загружаем картинку спрайта
                sprite.setImageDrawable(draw);
                // Анимации смены одного изображения на другое
                Animation inAnim = new AlphaAnimation(0, 1);
                inAnim.setDuration(1000);
                Animation outAnim = new AlphaAnimation(1, 0);
                outAnim.setDuration(1000);
                sprite.setInAnimation(inAnim);
                sprite.setOutAnimation(outAnim);
                // Анимация перемещения
                if (sp.getPos() != null)
                    sprite.animate().translationX(generateSpriteLayout(sp)).setDuration(500).start();
                // Добавляем картинку на экран и имя спрайта - в Map
                constraintLayout.addView(sprite);
                spImgList.put(sp.getSrc(), sprite);
            } else {
                // Изменяем эмоцию
                if (!draw.equals((((ImageView) sprite.getCurrentView())).getDrawable()))
                    sprite.setImageDrawable(draw);
                //todo если на месте - анимку не надо
                if (sp.getPos() != null)
                    sprite.animate().translationX(generateSpriteLayout(sp)).setDuration(500).start();
            }
        }
    }

    @Override
    public void showFrame() {
        // Получаем следующий узел до тех пор, пока не будет положительное условие
        while (!interpretCondition(frame.getCondition()))
            frame = getNextNode();
        // Сохраняем последний фон
        if (frame.getBackground() != null)
            curImg = frame.getBackground();
        // И последнюю музыку
        if (frame.getMusic() != null)
            curMus = frame.getMusic();
        setBackground(frame.getBackground(), frame.getMusic());
        prsName.setText(frame.getPers());
        tv.setText("");
        idxPhrase = 0;
        smoothlyPrint(frame.getPhrases());
        try {
            //todo выгрузить звук
            spriteAnim(frame.getSprites());
        } catch (Exception e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
    }

    private void smoothlyPrint(@NotNull List<String> phrases) {
        curPhrase = phrases.get(idxPhrase);
        idxChar = 0;
        isPhraseDisplayed = true;
        for (int i = 0; i < curPhrase.length(); i++)
            h.postDelayed(showText, TEXT_TIMEOUT * i);
        h.postDelayed(stopText, TEXT_TIMEOUT * curPhrase.length());
        idxPhrase++;
        if (idxPhrase == frame.getPhrases().size())
            h.postDelayed(postTextOutput, TEXT_TIMEOUT * curPhrase.length());
    }

    public void btnChoiceClick(@NotNull View v) {
        // Убираем кнопки обратно
        changeConstraintsOff();
        // Делаем что надо
        passNodeId.add(frame.getId());
        switch (v.getId()) {
            case R.id.btnChoice1:
                curId = chsTrue.get(0).getNext();
                break;
            case R.id.btnChoice2:
                curId = chsTrue.get(1).getNext();
                break;
            case R.id.btnChoice3:
                curId = chsTrue.get(2).getNext();
                break;
            case R.id.btnChoice4:
                curId = chsTrue.get(3).getNext();
                break;
            case R.id.btnChoice5:
                curId = chsTrue.get(4).getNext();
                break;
        }
        frame = getNode(curId, 0);
        curIdx = 0;
        showFrame();
        saveGame(this, DEFAULT_SAVE);
    }

    @NotNull
    private String merger(@NotNull List<String> list) {
        StringBuilder stb = new StringBuilder(list.size());
        for (String str : list) {
            stb.append(" ").append(str);
        }
        return stb.deleteCharAt(0).toString();
    }

    private void changeConstraints() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        // Отдельно привязываем верхнюю кнопку
        set.clear(btnsChoice[0].getId(), ConstraintSet.TOP);
        set.connect(btnsChoice[0].getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        btnsChoice[0].setText(chsTrue.get(0).getPhrase());
        // Затем все остальные к предыдущей
        for (int i = 1; i < chsTrue.size(); i++) {
            btnsChoice[i].setText(chsTrue.get(i).getPhrase());
            // Верх текущей к низу предыдущей
            set.clear(btnsChoice[i].getId(), ConstraintSet.TOP);
            set.connect(btnsChoice[i].getId(), ConstraintSet.TOP, btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM);
            // Низ предыдущей к верху текущей
            set.clear(btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM);
            set.connect(btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM, btnsChoice[i].getId(), ConstraintSet.TOP);
        }
        set.applyTo(constraintLayout);
        for (int i = 0; i < chsTrue.size(); i++)
            btnsChoice[i].setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(constraintLayout);
    }

    private void changeConstraintsOff() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        for (int i = 0; i < Math.min(btnsChoice.length, frame.getChoices().size()); i++) {
            btnsChoice[i].setText("");
            // По-умолчанию
            set.clear(btnsChoice[i].getId(), ConstraintSet.TOP);
            set.clear(btnsChoice[i].getId(), ConstraintSet.BOTTOM);
            set.connect(btnsChoice[i].getId(), ConstraintSet.BOTTOM, R.id.prsName, ConstraintSet.TOP);
        }
        set.applyTo(constraintLayout);
        for (int i = 0; i < Math.min(btnsChoice.length, frame.getChoices().size()); i++)
            btnsChoice[i].setVisibility(View.INVISIBLE);
        TransitionManager.beginDelayedTransition(constraintLayout);
    }

    public boolean interpretCondition(String condition) {
        if (condition == null)
            return true;
        String[] tokens = condition.split("\\|");
        boolean binVar = true;
        boolean out = false;
        for (String str : tokens) {
            String[] var = str.split("\\$");

            for (int i = 0; i < var.length; i++) {
                var[i] = var[i].trim();
                if (var[i].equals("T"))
                    binVar = true;
                else if (var[i].equals("F"))
                    binVar = false;
                else {
                    if (var[i].startsWith("!"))
                        binVar = binVar && !passNodeId.contains(getNode(var[i].substring(1), 0).getId());
                    else
                        binVar = binVar && passNodeId.contains(getNode(var[i], 0).getId());
                }
            }
            out = out || binVar;
        }
        return out;
    }

    @Override
    protected void setBackground(String image, String music) {
        try {
            if (image != null) {
                background.setImageBitmap(BitmapFactory.decodeStream(getAssets()
                        .open("backgrounds/" + image)));
                curImg = image;
            }
            if (music != null) {
                mp.reset();
                AssetFileDescriptor desc = getAssets().openFd("sounds/" + music);
                mp.setDataSource(desc.getFileDescriptor(), desc.getStartOffset(), desc.getLength());
                mp.setLooping(true);
                desc.close();
                mp.prepare();
                mp.start();
                curMus = music;
            }
            //todo проиграть музон
        } catch (IOException e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
    }

    public void btnChapterClick(@NotNull View v) {
        /*Button ok = findViewById(R.id.button);
        ok.setVisibility(View.VISIBLE);
        EditText txt = findViewById(R.id.editText);
        txt.setVisibility(View.VISIBLE);
        ok.setOnClickListener(v1 -> {
            try {
                XMLParseDialogues xParse = new XMLParseDialogues(
                        new ByteArrayInputStream(txt.getText().toString().getBytes()),
                        getAssets().list("backgrounds"),
                        getAssets().list("sprites"),
                        getAssets().list("sounds"),
                        getAssets());
                nodes = xParse.getNodes();
                frame = getNode(XMLParseDialogues.ENTRY);
                showFrame();
                ok.setVisibility(View.INVISIBLE);
                txt.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                Service.showErrorDialog(this, "Уважаемый, ваш сюжет оставляет желать лучшего...\n" + e.getLocalizedMessage());
            }
        });*/
        Toast.makeText(getApplicationContext(),
                "Дерево сюжета скоро будет доступно...", Toast.LENGTH_LONG).show();
    }

    public void btnLoadClick(@NotNull View v) throws IOException {
        Safekeeping.loadClick(this);
        startActivity(new Intent(getApplicationContext(), ConservationActivity.class));
    }

    public void btnSaveClick(@NotNull View v) throws IOException {
        Safekeeping.saveClick(this);
        startActivity(new Intent(getApplicationContext(), ConservationActivity.class));
    }
}