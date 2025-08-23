package com.taxi.easy.ua.utils_test;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class DrawableMatcher {

    public static Matcher<View> withDrawable(final int resourceId) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has drawable resource " + resourceId);
            }

            @Override
            protected boolean matchesSafely(ImageView imageView) {
                if (resourceId < 0) {
                    return imageView.getDrawable() == null;
                }
                Drawable expectedDrawable = imageView.getContext().getDrawable(resourceId);
                Drawable actualDrawable = imageView.getDrawable();
                if (expectedDrawable == null || actualDrawable == null) {
                    return false;
                }
                // Сравним по id ресурса (упрощённо)
                return actualDrawable.getConstantState().equals(expectedDrawable.getConstantState());
            }
        };
    }
}
