package com.taxi.easy.ua.ui.cities.check;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.button.MaterialButton;

/**
 * Движущийся индикатор на нажатой кнопке города. Живёт {@link CityPressIndicatorPolicy#DURATION_MS}
 * и не зависит от ответа сервера.
 */
public final class MovingCityButtonIndicator {

    private static final int ACTIVE_TINT = 0xFF456488;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopRunnable = this::stop;

    @Nullable
    private View button;
    @Nullable
    private Drawable savedForeground;
    @Nullable
    private ColorStateList savedTint;
    @Nullable
    private ValueAnimator animator;

    public void show(@Nullable View target) {
        if (target == null) {
            return;
        }
        if (target == button && animator != null && animator.isRunning()) {
            handler.removeCallbacks(stopRunnable);
            handler.postDelayed(stopRunnable, CityPressIndicatorPolicy.DURATION_MS);
            return;
        }
        stop();
        button = target;
        savedForeground = target.getForeground();
        if (target instanceof AppCompatButton) {
            savedTint = ((AppCompatButton) target).getSupportBackgroundTintList();
            ((AppCompatButton) target).setSupportBackgroundTintList(ColorStateList.valueOf(ACTIVE_TINT));
        }
        float radius = 12f * target.getResources().getDisplayMetrics().density;
        if (target instanceof MaterialButton) {
            radius = ((MaterialButton) target).getCornerRadius();
        }
        MovingBarDrawable drawable = new MovingBarDrawable(radius);
        target.setForeground(drawable);
        ValueAnimator running = ValueAnimator.ofFloat(0f, 1f);
        running.setDuration(1600L);
        running.setRepeatCount(ValueAnimator.INFINITE);
        running.setInterpolator(new LinearInterpolator());
        running.addUpdateListener(animation -> drawable.setPhase((float) animation.getAnimatedValue()));
        animator = running;
        running.start();
        handler.postDelayed(stopRunnable, CityPressIndicatorPolicy.DURATION_MS);
    }

    public void stop() {
        handler.removeCallbacks(stopRunnable);
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (button != null) {
            button.setForeground(savedForeground);
            if (button instanceof AppCompatButton) {
                ((AppCompatButton) button).setSupportBackgroundTintList(savedTint);
            }
        }
        button = null;
        savedForeground = null;
        savedTint = null;
    }

    public void release() {
        stop();
    }
}
