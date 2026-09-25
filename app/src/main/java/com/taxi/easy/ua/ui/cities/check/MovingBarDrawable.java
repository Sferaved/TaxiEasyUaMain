package com.taxi.easy.ua.ui.cities.check;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Мягкий блик и тонкая бегущая полоска по скруглённой кнопке города. */
public final class MovingBarDrawable extends Drawable {

    private final float cornerRadius;
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF rect = new RectF();
    private float phase;

    public MovingBarDrawable(float cornerRadiusPx) {
        this.cornerRadius = cornerRadiusPx;
        trackPaint.setColor(0x33FFFFFF);
        pillPaint.setColor(0xFFF2FBFF);
    }

    public void setPhase(float phase) {
        this.phase = phase;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        android.graphics.Rect bounds = getBounds();
        float width = bounds.width();
        float height = bounds.height();
        if (width <= 0f || height <= 0f) {
            return;
        }
        rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
        clipPath.reset();
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);

        float sheenWidth = width * 0.42f;
        float travel = width + sheenWidth;
        float sheenLeft = bounds.left + travel * phase - sheenWidth;
        sheenPaint.setShader(new LinearGradient(
                sheenLeft,
                0f,
                sheenLeft + sheenWidth,
                0f,
                new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(sheenLeft, bounds.top, sheenLeft + sheenWidth, bounds.bottom, sheenPaint);

        float inset = Math.max(cornerRadius * 0.45f, 8f);
        float trackHeight = Math.max(height * 0.08f, 4f);
        float trackTop = bounds.bottom - inset * 0.35f - trackHeight;
        float trackLeft = bounds.left + inset;
        float trackRight = bounds.right - inset;
        rect.set(trackLeft, trackTop, trackRight, trackTop + trackHeight);
        canvas.drawRoundRect(rect, trackHeight, trackHeight, trackPaint);

        float pillWidth = Math.max((trackRight - trackLeft) * 0.28f, trackHeight * 3f);
        float pillTravel = (trackRight - trackLeft) - pillWidth;
        float pillLeft = trackLeft + pillTravel * phase;
        rect.set(pillLeft, trackTop, pillLeft + pillWidth, trackTop + trackHeight);
        pillPaint.setShader(new LinearGradient(
                pillLeft,
                0f,
                pillLeft + pillWidth,
                0f,
                new int[]{0x00F2FBFF, 0xFFF2FBFF, 0x00F2FBFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, trackHeight, trackHeight, pillPaint);

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        sheenPaint.setAlpha(alpha);
        trackPaint.setAlpha(alpha);
        pillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        sheenPaint.setColorFilter(colorFilter);
        trackPaint.setColorFilter(colorFilter);
        pillPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
