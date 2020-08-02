package com.pryanik.procrastination;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.pryanik.procrastination.support.Service;

public class IntroComicActivity extends AppCompatActivity {

    public static boolean isComicDisplayed;
    private int childCount;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_comic);
        isComicDisplayed = true;

        final ViewFlipper viewFlipper = findViewById(R.id.viewFlipper);
        childCount = viewFlipper.getChildCount();
        Animation inAnim = new AlphaAnimation(0, 1);
        inAnim.setDuration(500);
        //todo Надо ли анимацию out?
        Animation outAnim = new TranslateAnimation(0f, 0f, 0f, 0f);
        outAnim.setDuration(500);
        viewFlipper.setInAnimation(inAnim);
        viewFlipper.setOutAnimation(outAnim);

        viewFlipper.setOnClickListener(v -> {
            if (viewFlipper.isFlipping())
                viewFlipper.stopFlipping();
            if (count > childCount) {
                isComicDisplayed = false;
                this.finish();
            }
            viewFlipper.showNext();
            viewFlipper.startFlipping();
        });

        viewFlipper.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (count++ == childCount) {
                isComicDisplayed = false;
                viewFlipper.stopFlipping();
                Toast toast = Toast.makeText(this, "Нажмите на экран", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
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