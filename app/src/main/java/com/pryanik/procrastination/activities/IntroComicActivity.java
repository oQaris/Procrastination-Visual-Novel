package com.pryanik.procrastination.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.pryanik.procrastination.App;
import com.pryanik.procrastination.R;
import com.pryanik.procrastination.support.Service;

import java.io.IOException;
import java.util.Objects;

public class IntroComicActivity extends AppCompatActivity implements Animation.AnimationListener {
    public static int flipInterval = 2500;
    public static boolean isComicDisplayed;
    private ViewFlipper viewFlipper;
    private int childCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Service.hideNavigation(this);
        setContentView(R.layout.activity_intro_comic);
        isComicDisplayed = true;

        viewFlipper = findViewById(R.id.viewFlipper);
        viewFlipper.setFlipInterval(flipInterval);
        try {
            childCount = Objects.requireNonNull(getAssets().list("intro_comic")).length;
            for (int i = 0; i < childCount; i++)
                addNextView(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //viewFlipper.showNext();

        Animation inAnim = new AlphaAnimation(0, 1);
        inAnim.setDuration(500);
        //todo Надо ли анимацию out?
        Animation outAnim = new TranslateAnimation(0f, 0f, 0f, 0f);
        outAnim.setDuration(500);
        inAnim.setAnimationListener(this);
        viewFlipper.setInAnimation(inAnim);
        viewFlipper.setOutAnimation(outAnim);

        viewFlipper.setOnClickListener(v -> {
            if (viewFlipper.indexOfChild(viewFlipper.getCurrentView()) == childCount - 1)
                this.finish();
            else {
                viewFlipper.stopFlipping();
                viewFlipper.showNext();
                viewFlipper.startFlipping();
            }
        });

        viewFlipper.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
        });
    }

    private void addNextView(int item) {
        ImageView comicFrame = new ImageView(getApplicationContext());
        comicFrame.setScaleType(ImageView.ScaleType.CENTER_CROP);
        comicFrame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        try {
            comicFrame.setImageDrawable(Drawable.createFromStream(getAssets()
                    .open("intro_comic/" + item + ".jpg"), null));
        } catch (IOException e) {
            e.printStackTrace();
        }
        viewFlipper.addView(comicFrame);
        //viewFlipper.removeViewAt(0);
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (viewFlipper.indexOfChild(viewFlipper.getCurrentView()) == childCount - 1) {
            isComicDisplayed = false;
            viewFlipper.stopFlipping();
            Toast toast = Toast.makeText(this, "Нажмите на экран", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        } else {
            //todo что то не так
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    // ------------------------------------- Life Cycle ------------------------------------- //

    @Override
    protected void onResume() {
        super.onResume();
        //Service.hideNavigation(this);
        App.playMP();
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.pauseMP();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            Service.hideNavigation(this);
    }
}