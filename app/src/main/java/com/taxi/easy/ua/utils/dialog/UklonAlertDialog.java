package com.taxi.easy.ua.utils.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.taxi.easy.ua.R;

/**
 * Единый стиль алерт-диалогов в духе кастомного диалога удаления карты:
 * скруглённая карточка, иконка в круге, заголовок, сообщение и кнопки-пилюли.
 * Фон приглушённый ({@code uklon_dialog_surface}), в тон приложению.
 */
public final class UklonAlertDialog {

    public interface OnClick {
        void onClick(@NonNull DialogInterface dialog);
    }

    private final Context context;
    private CharSequence title;
    private CharSequence message;
    @DrawableRes
    private int iconRes = 0;
    @ColorRes
    private int iconTintRes = R.color.zamov;
    private CharSequence positiveText;
    private OnClick positiveClick;
    private CharSequence negativeText;
    private OnClick negativeClick;
    private boolean cancelable = true;
    private boolean positiveDestructive = false;
    private DialogInterface.OnDismissListener onDismiss;

    public UklonAlertDialog(@NonNull Context context) {
        this.context = context;
    }

    public UklonAlertDialog setTitle(@Nullable CharSequence value) {
        this.title = value;
        return this;
    }

    public UklonAlertDialog setTitle(@StringRes int resId) {
        this.title = context.getString(resId);
        return this;
    }

    public UklonAlertDialog setMessage(@Nullable CharSequence value) {
        this.message = value;
        return this;
    }

    public UklonAlertDialog setMessage(@StringRes int resId) {
        this.message = context.getString(resId);
        return this;
    }

    public UklonAlertDialog setIcon(@DrawableRes int resId) {
        this.iconRes = resId;
        return this;
    }

    public UklonAlertDialog setIconTint(@ColorRes int colorRes) {
        this.iconTintRes = colorRes;
        return this;
    }

    public UklonAlertDialog setPositiveButton(@Nullable CharSequence text, @Nullable OnClick click) {
        this.positiveText = text;
        this.positiveClick = click;
        return this;
    }

    public UklonAlertDialog setPositiveButton(@StringRes int resId, @Nullable OnClick click) {
        return setPositiveButton(context.getString(resId), click);
    }

    public UklonAlertDialog setNegativeButton(@Nullable CharSequence text, @Nullable OnClick click) {
        this.negativeText = text;
        this.negativeClick = click;
        return this;
    }

    public UklonAlertDialog setNegativeButton(@StringRes int resId, @Nullable OnClick click) {
        return setNegativeButton(context.getString(resId), click);
    }

    public UklonAlertDialog setCancelable(boolean value) {
        this.cancelable = value;
        return this;
    }

    /** Окрашивает основную кнопку в красный (деструктивное действие), как в диалоге удаления. */
    public UklonAlertDialog setPositiveDestructive(boolean value) {
        this.positiveDestructive = value;
        return this;
    }

    public UklonAlertDialog setOnDismissListener(@Nullable DialogInterface.OnDismissListener listener) {
        this.onDismiss = listener;
        return this;
    }

    @NonNull
    public AlertDialog create() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null);

        View iconContainer = view.findViewById(R.id.alertIconContainer);
        ImageView icon = view.findViewById(R.id.alertIcon);
        TextView titleView = view.findViewById(R.id.alertTitle);
        TextView messageView = view.findViewById(R.id.alertMessage);
        TextView positiveView = view.findViewById(R.id.alertBtnPositive);
        TextView negativeView = view.findViewById(R.id.alertBtnNegative);

        if (iconRes != 0) {
            icon.setImageResource(iconRes);
            ImageViewCompat.setImageTintList(icon,
                    ColorStateList.valueOf(ContextCompat.getColor(context, iconTintRes)));
            iconContainer.setVisibility(View.VISIBLE);
        } else {
            iconContainer.setVisibility(View.GONE);
        }

        if (title != null && title.length() > 0) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        } else {
            titleView.setVisibility(View.GONE);
        }

        if (message != null && message.length() > 0) {
            messageView.setText(message);
            messageView.setVisibility(View.VISIBLE);
        } else {
            messageView.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(cancelable)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        if (onDismiss != null) {
            dialog.setOnDismissListener(onDismiss);
        }

        if (positiveText != null) {
            positiveView.setText(positiveText);
            positiveView.setVisibility(View.VISIBLE);
            if (positiveDestructive) {
                positiveView.setBackgroundResource(R.drawable.bg_dialog_btn_delete);
            }
            positiveView.setOnClickListener(v -> {
                if (positiveClick != null) {
                    positiveClick.onClick(dialog);
                }
                dialog.dismiss();
            });
        } else {
            positiveView.setVisibility(View.GONE);
        }

        if (negativeText != null) {
            negativeView.setText(negativeText);
            negativeView.setVisibility(View.VISIBLE);
            negativeView.setOnClickListener(v -> {
                if (negativeClick != null) {
                    negativeClick.onClick(dialog);
                }
                dialog.dismiss();
            });
        } else {
            negativeView.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) positiveView.getLayoutParams();
            lp.setMarginStart(0);
            positiveView.setLayoutParams(lp);
        }

        return dialog;
    }

    @NonNull
    public AlertDialog show() {
        AlertDialog dialog = create();
        dialog.show();
        return dialog;
    }
}
