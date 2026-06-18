package com.taxi.easy.ua.utils.phone;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.redmadrobot.inputmask.MaskedTextChangedListener;
import com.taxi.easy.ua.R;

import java.util.regex.Pattern;

/** Normalizes Ukrainian phone numbers and shows a confirmation dialog before save. */
public final class PhoneNumberHelper {

    public static final String PHONE_MASK = "+38 [000] [000] [00] [00]";
    public static final String PHONE_PATTERN = "\\+38 0\\d{2} \\d{3} \\d{2} \\d{2}";

    private PhoneNumberHelper() {
    }

    public interface ConfirmCallback {
        void onConfirmed(@NonNull String formattedPhone);
    }

    public interface EditCallback {
        void onEdit();
    }

    public static void setupPhoneInput(@NonNull EditText editText) {
        MaskedTextChangedListener maskListener = new MaskedTextChangedListener(
                PHONE_MASK,
                editText,
                null
        );
        editText.addTextChangedListener(maskListener);
        editText.setOnFocusChangeListener(maskListener);
        attachDuplicateDigitFix(editText);
    }

    @NonNull
    public static String fixMaskedInput(@Nullable String maskedInput) {
        if (maskedInput == null || maskedInput.isEmpty()) {
            return "";
        }

        String allDigits = maskedInput.replaceAll("[^\\d]", "");
        if (allDigits.length() <= 2 || !allDigits.startsWith("38")) {
            return maskedInput.trim();
        }

        String national = allDigits.substring(2);
        if (!national.startsWith("380")) {
            return maskedInput.trim();
        }

        national = "0" + national.substring(3);
        if (national.isEmpty() || !national.startsWith("0")) {
            return maskedInput.trim();
        }

        String intl = "380" + national.substring(1);
        if (intl.length() > 12) {
            intl = intl.substring(0, 12);
        }
        return formatPartialIntl(intl);
    }

    public static void attachDuplicateDigitFix(@NonNull EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean fixing;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fixing) {
                    return;
                }
                String raw = s.toString();
                String fixed = fixMaskedInput(raw);
                if (!fixed.equals(raw)) {
                    fixing = true;
                    editText.setText(fixed);
                    editText.setSelection(fixed.length());
                    fixing = false;
                }
            }
        });
    }

    @NonNull
    public static String normalizeAndFormat(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String fixedMasked = fixMaskedInput(input);
        String digits = fixDuplicateCountryCode(fixedMasked.replaceAll("[^\\d]", ""));
        if (digits.length() == 12 && digits.startsWith("380")) {
            return formatFromDigits380(digits);
        }
        return fixedMasked.trim();
    }

    @NonNull
    private static String fixDuplicateCountryCode(@NonNull String digits) {
        if (digits.length() == 12 && digits.startsWith("380")) {
            return digits;
        }
        if (digits.length() > 12 && digits.startsWith("38")) {
            String withoutLeading38 = digits.substring(2);
            if (withoutLeading38.length() == 12 && withoutLeading38.startsWith("380")) {
                return withoutLeading38;
            }
        }
        return digits;
    }

    @NonNull
    private static String formatPartialIntl(@NonNull String intl) {
        if (intl.length() == 12 && intl.startsWith("380")) {
            return formatFromDigits380(intl);
        }
        if (intl.length() < 3 || !intl.startsWith("380")) {
            return "+38";
        }
        StringBuilder out = new StringBuilder("+38");
        appendSpacedGroup(out, intl, 2, 5);
        appendSpacedGroup(out, intl, 5, 8);
        appendSpacedGroup(out, intl, 8, 10);
        appendSpacedGroup(out, intl, 10, 12);
        return out.toString().trim();
    }

    private static void appendSpacedGroup(StringBuilder out, String digits, int start, int end) {
        if (digits.length() <= start) {
            return;
        }
        out.append(' ');
        out.append(digits, start, Math.min(end, digits.length()));
    }

    @NonNull
    private static String formatFromDigits380(@NonNull String digits) {
        return "+38 " + digits.substring(2, 5) + " "
                + digits.substring(5, 8) + " "
                + digits.substring(8, 10) + " "
                + digits.substring(10, 12);
    }

    public static boolean isValid(@Nullable String phone) {
        return phone != null && !phone.isEmpty()
                && Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
    }

    public static void showConfirmDialog(@NonNull Context context,
                                         @NonNull String formattedPhone,
                                         @NonNull ConfirmCallback onConfirmed,
                                         @Nullable EditCallback onEdit) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_phone_confirm, null);
        TextView phoneView = view.findViewById(R.id.phoneConfirmNumber);
        phoneView.setText(formattedPhone);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.phoneConfirmBtnYes).setOnClickListener(v -> {
            dialog.dismiss();
            onConfirmed.onConfirmed(formattedPhone);
        });
        view.findViewById(R.id.phoneConfirmBtnEdit).setOnClickListener(v -> {
            dialog.dismiss();
            if (onEdit != null) {
                onEdit.onEdit();
            }
        });
        dialog.show();
    }
}
