package com.pryanik.procrastination.activities;

import static com.pryanik.procrastination.App.events;
import static com.pryanik.procrastination.App.h;
import static com.pryanik.procrastination.game_safe.Safekeeping.DEFAULT_SAVE;
import static com.pryanik.procrastination.game_safe.Safekeeping.curId;
import static com.pryanik.procrastination.game_safe.Safekeeping.curIdx;
import static com.pryanik.procrastination.game_safe.Safekeeping.curImg;
import static com.pryanik.procrastination.game_safe.Safekeeping.curMus;
import static com.pryanik.procrastination.game_safe.Safekeeping.passNodeId;
import static com.pryanik.procrastination.game_safe.Safekeeping.saveGame;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.transition.TransitionManager;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.pryanik.procrastination.App;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.game_safe.Safekeeping;
import com.pryanik.procrastination.support.Service;
import com.pryanik.procrastination.xml_parser.Choice;
import com.pryanik.procrastination.xml_parser.IEvent;
import com.pryanik.procrastination.xml_parser.Sound;
import com.pryanik.procrastination.xml_parser.Sprite;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MainActivity extends PreMainActivity {
    public static long TEXT_TIMEOUT;
    private boolean isPhraseDisplayed;
    Runnable stopText = () -> isPhraseDisplayed = false;
    private String curPhrase;
    private int idxPhrase;
    private int idxChar;
    Runnable showText = () -> {
        tv.append(curPhrase.substring(idxChar, idxChar + 1));
        idxChar++;
    };
    private List<Choice> avlbChoices;
    Runnable postTextOutput = () -> {
        isPhraseDisplayed = false;
        // Выводим кнопки выборов
        if (frame.getChoices() != null) {
            avlbChoices = new ArrayList<>(frame.getChoices().size());
            for (Choice ch : frame.getChoices())
                if (interpretCondition(ch.getCondition()))
                    avlbChoices.add(ch);
            // Запускаем анимацию кнопок
            showBtnsChoice();
        }
    };

    private static boolean isSpritesContainsBySrs(@NotNull List<Sprite> spriteList, String spriteSrc) {
        for (Sprite sp : spriteList) {
            if (sp.getSrc().equals(spriteSrc))
                return true;
        }
        return false;
    }

    // ----------------------------- Методы показа нового фрэйма ----------------------------- //

    @Nullable
    private static ImageSwitcher getImgByTagSrc(String tagSrc) {
        for (ImageSwitcher sw : spImgList) {
            if (((Sprite) sw.getTag()).getSrc().equals(tagSrc))
                return sw;
        }
        return null;
    }

    private static float generateSpriteLayout(@NotNull Sprite sp) {
        return sp.getPos() * size.x - sp.getPos() *
                ((size.y - interfaceHeight) * (float) sp.getWidth() / sp.getHeight());
    }

    public static void spriteAnim(Context context, List<Sprite> spriteList, boolean isAbsolutePosition) throws IOException {
        // Удаляем неиспользуемые
        ListIterator<ImageSwitcher> i = spImgList.listIterator();
        while (i.hasNext()) {
            ImageSwitcher sw = i.next();
            String src = ((Sprite) sw.getTag()).getSrc();
            if (!isSpritesContainsBySrs(spriteList, src)) {
                mainLayout.removeView(sw);
                i.remove();
            }
        }
        // Преобразуем или добавляем спрайты
        for (Sprite sp : spriteList) {
            ImageSwitcher sprite = getImgByTagSrc(sp.getSrc());
            Drawable draw = Drawable.createFromStream(context.getAssets().open(sp.name()), null);
            // Добавлям новый спрайт
            if (sprite == null) {
                sprite = new ImageSwitcher(context);
                // Задаём параметры лайаута самого ImageSwitcher'a
                ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins(0, 0, 0, interfaceHeight);
                sprite.setLayoutParams(layoutParams);
                // Ставим основные параметры загружаемой картинки
                sprite.setFactory(() -> {
                    ImageView image = new ImageView(context);
                    image.setScaleType(ImageView.ScaleType.FIT_START);
                    image.setLayoutParams(new ImageSwitcher.LayoutParams(
                            ImageSwitcher.LayoutParams.MATCH_PARENT,
                            ImageSwitcher.LayoutParams.MATCH_PARENT));
                    return image;
                });
                sprite.setId(View.generateViewId());
                // Загружаем картинку спрайта
                sprite.setImageDrawable(draw);
                // Прикрепляем тег, чтобы можно было потом легко сравнить изображения
                sprite.setTag(sp);
                // Анимации смены одного изображения на другое
                Animation inAnim = new AlphaAnimation(0, 1);
                inAnim.setDuration(1000);
                Animation outAnim = new AlphaAnimation(1, 0);
                outAnim.setDuration(1000);
                sprite.setInAnimation(inAnim);
                sprite.setOutAnimation(outAnim);
                // Анимация перемещения
                if (sp.getPos() != null)
                    sprite.animate().translationX(isAbsolutePosition ? sp.getPos() : generateSpriteLayout(sp))
                            .setDuration(500).start();
                // Добавляем картинку на экран и имя спрайта - в Список
                mainLayout.addView(sprite);
                spImgList.add(sprite);
            }
            // Изменяем эмоцию
            else {
                // Если новая картинка точно такая же, то не меняем
                if (!((Sprite) sprite.getTag()).name().equals(sp.name())) {
                    sprite.setImageDrawable(draw);
                    sprite.setTag(sp);
                }
                // Если пустая позиция, оставляем спрайт на том же месте
                if (sp.getPos() != null)
                    sprite.animate().translationX(generateSpriteLayout(sp)).setDuration(500).start();
            }
            if (!sp.isAct()) {
                // Устанавливаем точку фокуса для анимаций уменьшения
                sprite.setPivotX(sprite.getMeasuredHeight() / 2.0f);
                sprite.setPivotY(sprite.getMeasuredHeight());
                sprite.animate().scaleX(0.75f).scaleY(0.75f).setDuration(500).start();
                //todo Сделать затемнение
                //((ImageView)sprite.getCurrentView()).getDrawable().setColorFilter(Color.parseColor("#99000000"), PorterDuff.Mode.DARKEN);
            } else sprite.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
        }
    }

    @Override
    public void tvClick(View v) {
        // Если нажали кнопку когда выводится текст
        if (isPhraseDisplayed) {
            h.removeCallbacks(showText);
            h.removeCallbacks(stopText);
            isPhraseDisplayed = false;
            tv.setText(Service.merger(frame.getPhrases()));
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
        /*Остальное - при первом нажатии текущего фрэйма!*/
        // Получаем следующий узел
        frame = getNextNode(false);
        // Отображаем
        showFrame();
    }

    private void showEvents(@NotNull List<IEvent> eventList) {
        List<Sprite> sprites = new ArrayList<>();
        try {
            for (IEvent event : eventList)
                if (event instanceof Sprite) {
                    sprites.add((Sprite) event);
                } else if (event instanceof Sound)
                    App.playSound(getApplicationContext(), ((Sound) event).getSrc());
            //todo выгрузить звук
            spriteAnim(getApplicationContext(), sprites, false);
        } catch (Exception e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
    }


    // --------------------------------- Работа со спрайтами --------------------------------- //

    @Override
    public void showFrame() {
        // Получаем следующий узел до тех пор, пока не будет положительное условие
        while (!interpretCondition(frame.getCondition()))
            frame = getNextNode(true);
        // Инициализируем кнопки выборов
        if (frame.getChoices() != null)
            fillBtnsChArr(frame.getChoices().size());
        // Сохраняем последний фон
        if (frame.getBackground() != null)
            curImg = frame.getBackground();
        // И последнюю музыку
        if (frame.getMusic() != null)
            curMus = frame.getMusic();
        // Делаем пройденным
        if (frame.getId() != null)
            passNodeId.add(frame.getId());
        // Устанавливаем музыку, задник, текст и выводи анимацию
        setBackground(frame.getBackground(), frame.getMusic());
        prsName.setText(frame.getPers());
        idxPhrase = 0;
        tv.setText("");
        smoothlyPrint(frame.getPhrases());
        try {
            spriteAnim(getApplicationContext(), frame.getSprites(), false);
        } catch (Exception e) {
            Service.showErrorDialog(this, e.getLocalizedMessage());
        }
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
                App.loadMusic(getApplicationContext(), music);
                App.playMP();
                curMus = music;
            }
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


    // ------------------------------ Работа с кнопками выборов ------------------------------ //

    private void fillBtnsChArr(int size) {
        btnsChoice = new Button[size];
        for (int i = 0; i < btnsChoice.length; i++) {
            btnsChoice[i] = new Button(getApplicationContext());
            btnsChoice[i].setId(startBtnChID + i);
            btnsChoice[i].setLayoutParams(new ConstraintLayout.LayoutParams(
                    0, ConstraintLayout.LayoutParams.WRAP_CONTENT));
            btnsChoice[i].setBackgroundColor(getResources().getColor(R.color.transparentGray));
            btnsChoice[i].setOnClickListener(this::btnChoiceClick);
            btnsChoice[i].setTextColor(getResources().getColor(R.color.white));
            btnsChoice[i].setVisibility(View.INVISIBLE);
            mainLayout.addView(btnsChoice[i]);

            ConstraintSet set = new ConstraintSet();
            set.clone(mainLayout);
            set.constrainPercentWidth(btnsChoice[i].getId(), 0.65f);
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
        btnsChoice[0].setText(avlbChoices.get(0).getPhrase());
        // Затем все остальные к предыдущей
        for (int i = 1; i < avlbChoices.size(); i++) {
            btnsChoice[i].setText(avlbChoices.get(i).getPhrase());
            // Верх текущей к низу предыдущей
            set.connect(btnsChoice[i].getId(), ConstraintSet.TOP, btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM);
            // Низ предыдущей к верху текущей
            set.connect(btnsChoice[i - 1].getId(), ConstraintSet.BOTTOM, btnsChoice[i].getId(), ConstraintSet.TOP);
        }
        set.applyTo(mainLayout);
        for (int i = 0; i < avlbChoices.size(); i++)
            btnsChoice[i].setVisibility(View.VISIBLE);
        TransitionManager.beginDelayedTransition(mainLayout);
    }

    private void hideBtnsChoice() {
        for (Button button : btnsChoice) mainLayout.removeView(button);
        btnsChoice = null;
    }

    public void btnChoiceClick(@NotNull View v) {
        // Убираем кнопки обратно
        hideBtnsChoice();
        // Добавляем в пройденные
        passNodeId.add(frame.getId());
        // Переходим по ID в выборе
        curId = avlbChoices.get(v.getId() - startBtnChID).getNext();
        frame = getNode(curId, 0);
        curIdx = 0;
        showFrame();
        saveGame(getApplicationContext(), DEFAULT_SAVE);
    }


    // ------------------------- Обработка нажатий кнопок интерфейса ------------------------- //

    public void btnChapterClick(@NotNull View v) {
        Toast.makeText(getApplicationContext(),
                "Дерево сюжета скоро будет доступно...", Toast.LENGTH_LONG).show();
    }

    public void btnLoadClick(@NotNull View v) {
        Safekeeping.loadClick(this);
        startActivity(new Intent(getApplicationContext(), ConservationActivity.class));
    }

    public void btnSaveClick(@NotNull View v) {
        Safekeeping.saveClick(this);
        startActivity(new Intent(getApplicationContext(), ConservationActivity.class));
    }

    public void btnExitClick(@NotNull View v) {
        this.finish();
    }
}