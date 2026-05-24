package com.taxi.easy.ua.utils.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

/**
 * ProgressBar расчёта стоимости: пока идёт расчёт, внешний код не может скрыть индикатор.
 */
public class CostCalculationProgressBar extends ProgressBar {

    private static final String TAG = "CostCalcProgressBar";
    private static volatile boolean calculationInProgress = false;

    public static void setCalculationInProgress(boolean inProgress) {
        calculationInProgress = inProgress;
        Log.d(TAG, "calculationInProgress=" + inProgress);
    }

    public static boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    public CostCalculationProgressBar(Context context) {
        super(context);
    }

    public CostCalculationProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CostCalculationProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setVisibility(int visibility) {
        if (calculationInProgress && visibility != View.VISIBLE) {
            Log.d(TAG, "blocked setVisibility(" + visibility + ") during cost calculation");
            return;
        }
        super.setVisibility(visibility);
    }

    public void forceShow() {
        super.setVisibility(VISIBLE);
    }

    public void forceHide() {
        super.setVisibility(GONE);
    }
}
