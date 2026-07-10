package com.taxi.easy.ua.ui.landing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

/**
 * Стартова сторінка PAS_1: теплий hero, швидке замовлення та сітка сервісів.
 */
public class LandingFragment extends Fragment {

    public interface LandingHost {
        boolean isLandingLoginRequired();

        @Nullable
        String getLandingWelcomeName();

        void onLandingLoginRequested();

        void onLandingProtectedAction(@NonNull LandingAction action);

        void onLandingOperatorCall();

        void onLandingExitRequested();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_landing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (MainActivity.button1 != null) {
            MainActivity.button1.setVisibility(View.GONE);
        }

        MaterialButton quickOrder = view.findViewById(R.id.btnLandingQuickOrder);
        quickOrder.setOnClickListener(v -> getLandingHost().onLandingProtectedAction(LandingAction.ORDER));

        MaterialButton login = view.findViewById(R.id.btnLandingLogin);
        login.setOnClickListener(v -> getLandingHost().onLandingLoginRequested());

        bindAction(view, R.id.actionOrder, R.drawable.ic_shortcut_order, R.string.landing_action_order,
                LandingAction.ORDER, true);
        bindAction(view, R.id.actionCalculation, R.drawable.ic_map, R.string.landing_action_calculation,
                LandingAction.CALCULATION, false);
        bindAction(view, R.id.actionCity, R.drawable.ic_city_weather, R.string.landing_action_city,
                LandingAction.CITY, false);
        bindAction(view, R.id.actionPayment, R.drawable.ic_credit_card, R.string.landing_action_payment,
                LandingAction.PAYMENT, false);
        bindAction(view, R.id.actionLanguage, R.drawable.ic_menu_slideshow, R.string.landing_action_language,
                LandingAction.LANGUAGE, false);
        View operatorRoot = view.findViewById(R.id.actionOperator);
        ImageView operatorIcon = operatorRoot.findViewById(R.id.ivLandingIcon);
        TextView operatorLabel = operatorRoot.findViewById(R.id.tvLandingLabel);
        operatorIcon.setImageResource(R.drawable.ic_notification_bell_blue);
        operatorIcon.setBackgroundResource(R.drawable.bg_landing_icon_soft);
        operatorIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.zamov));
        operatorLabel.setText(R.string.landing_action_operator);
        operatorRoot.setClickable(true);
        operatorRoot.setFocusable(true);
        operatorRoot.setOnClickListener(v -> getLandingHost().onLandingOperatorCall());
        bindAction(view, R.id.actionDriver, R.drawable.ic_shortcut_driver, R.string.landing_action_driver,
                LandingAction.DRIVER, false);
        bindPlainAction(view, R.id.actionExit, R.drawable.ic_close_24, R.string.landing_action_exit,
                v -> getLandingHost().onLandingExitRequested());

        refreshLandingState();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLandingState();
    }

    private void refreshLandingState() {
        View root = getView();
        if (root == null) {
            return;
        }
        View loginCard = root.findViewById(R.id.landingLoginCard);
        TextView greeting = root.findViewById(R.id.tvLandingGreeting);
        TextView subtitle = root.findViewById(R.id.tvLandingSubtitle);
        boolean loginRequired = getLandingHost().isLandingLoginRequired();
        if (loginCard != null) {
            loginCard.setVisibility(loginRequired ? View.VISIBLE : View.GONE);
        }
        if (greeting == null || subtitle == null) {
            return;
        }
        if (loginRequired) {
            greeting.setText(R.string.landing_greeting);
            subtitle.setText(R.string.landing_greeting_subtitle_guest);
            return;
        }
        String welcomeName = getLandingHost().getLandingWelcomeName();
        if (welcomeName != null && !welcomeName.isEmpty()) {
            greeting.setText(getString(R.string.landing_greeting_named, welcomeName));
        } else {
            greeting.setText(R.string.landing_greeting_back);
        }
        subtitle.setText(R.string.landing_greeting_subtitle_user);
    }

    private void bindPlainAction(@NonNull View root, int includeId, int iconRes, int labelRes,
                                 @NonNull View.OnClickListener listener) {
        View item = root.findViewById(includeId);
        if (item == null) {
            return;
        }
        item.setClickable(true);
        item.setFocusable(true);
        ImageView icon = item.findViewById(R.id.ivLandingIcon);
        TextView label = item.findViewById(R.id.tvLandingLabel);
        if (icon != null) {
            icon.setImageResource(iconRes);
            icon.setBackgroundResource(R.drawable.bg_landing_icon_soft);
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.zamov));
        }
        if (label != null) {
            label.setText(labelRes);
        }
        item.setOnClickListener(listener);
    }

    private void bindAction(@NonNull View root, int includeId, int iconRes, int labelRes,
                            @NonNull LandingAction action, boolean primary) {
        View item = root.findViewById(includeId);
        if (item == null) {
            return;
        }
        item.setClickable(true);
        item.setFocusable(true);
        ImageView icon = item.findViewById(R.id.ivLandingIcon);
        TextView label = item.findViewById(R.id.tvLandingLabel);
        if (icon != null) {
            icon.setImageResource(iconRes);
            if (primary) {
                icon.setBackgroundResource(R.drawable.bg_landing_icon_primary);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.uklon_on_primary));
            } else {
                icon.setBackgroundResource(R.drawable.bg_landing_icon_soft);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.zamov));
            }
        }
        if (label != null) {
            label.setText(labelRes);
        }
        item.setOnClickListener(v -> getLandingHost().onLandingProtectedAction(action));
    }

    @NonNull
    private LandingHost getLandingHost() {
        if (!(requireActivity() instanceof LandingHost)) {
            throw new IllegalStateException("Activity must implement LandingHost");
        }
        return (LandingHost) requireActivity();
    }
}
