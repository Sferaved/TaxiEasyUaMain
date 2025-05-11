package com.taxi.easy.ua.utils.animation.car;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.taxi.easy.ua.R;

public class CarProgressBar extends RelativeLayout {

    private ImageView carImage;
    private ObjectAnimator carAnimation;

    public CarProgressBar(Context context) {
        super(context);
        init(null);
    }

    public CarProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CarProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        // Initialize the ImageView
        carImage = new ImageView(getContext());

        // Set default car image and size
        @SuppressLint("UseCompatLoadingForDrawables") Drawable carDrawable = getResources().getDrawable(R.drawable.button_image_button2_old);
        int carWidth = 400;  // Default width in dp
        int carHeight = 200; // Default height in dp

        // If there are custom attributes, apply them
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CarProgressBar);
            carDrawable = a.getDrawable(R.styleable.CarProgressBar_carImage);
            carWidth = a.getDimensionPixelSize(R.styleable.CarProgressBar_carWidth, carWidth);
            carHeight = a.getDimensionPixelSize(R.styleable.CarProgressBar_carHeight, carHeight);
            a.recycle();
        }

        // Apply the drawable and size to the ImageView
        carImage.setImageDrawable(carDrawable);
        LayoutParams params = new LayoutParams(carWidth, carHeight);
        carImage.setLayoutParams(params);

        // Add the ImageView to the layout
        addView(carImage);

        // Start the animation after the layout has been drawn
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                startAnimation();
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public void startAnimation() {
        int screenWidth = getWidth();

        // Create the animation
        carAnimation = ObjectAnimator.ofFloat(carImage, "translationX", 0f, screenWidth - carImage.getWidth());
        carAnimation.setDuration(2000);
        carAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        carAnimation.setRepeatMode(ObjectAnimator.REVERSE);

        // Start the animation
        carAnimation.start();
    }

    // Method to stop the animation
    public void stopAnimation() {
        if (carAnimation != null && carAnimation.isRunning()) {
            carAnimation.cancel();
        }
    }

    // Method to start the animation (if previously stopped)
    public void resumeAnimation() {
        if (carAnimation != null && !carAnimation.isRunning()) {
            carAnimation.start();
        }
    }
}
