package com.taxi.easy.ua.utils.orders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;

public final class OrderHistoryStatusHelper {

    public enum StatusKind {
        CANCELED,
        WAITING_DISPATCH,
        IN_WORK,
        AT_START_POINT,
        IN_ROUTE,
        COMPLETED,
        CLIENT_REMOVED,
        NOT_DONE,
        DEFAULT
    }

    private OrderHistoryStatusHelper() {
    }

    @NonNull
    public static String resolveStatusText(
            @NonNull Context context,
            @Nullable String closeReason,
            @Nullable String executionStatus,
            @Nullable String requiredTime,
            @Nullable String orderUid
    ) {
        return context.getString(stringResForKind(
                resolveKind(closeReason, executionStatus, requiredTime, orderUid)));
    }

    @NonNull
    public static String resolveStatusText(
            @NonNull Context context,
            @Nullable String closeReason,
            @Nullable String executionStatus,
            @Nullable String requiredTime
    ) {
        return resolveStatusText(context, closeReason, executionStatus, requiredTime, null);
    }

    @NonNull
    public static StatusKind resolveKind(
            @Nullable String closeReason,
            @Nullable String executionStatus,
            @Nullable String requiredTime,
            @Nullable String orderUid
    ) {
        if (isCanceled(closeReason, executionStatus, orderUid)) {
            return StatusKind.CANCELED;
        }

        String cr = closeReason != null ? closeReason.trim() : "";

        if ("101".equals(cr) || "-1".equals(cr)) {
            if (isWaitingDispatch(executionStatus, requiredTime)) {
                return StatusKind.WAITING_DISPATCH;
            }
            return StatusKind.IN_WORK;
        }

        switch (cr) {
            case "102":
                return StatusKind.AT_START_POINT;
            case "103":
                return StatusKind.IN_ROUTE;
            case "104":
            case "8":
            case "0":
                return StatusKind.COMPLETED;
            case "1":
            case "6":
            case "7":
            case "9":
                return StatusKind.CLIENT_REMOVED;
            case "2":
            case "3":
            case "4":
            case "5":
                return StatusKind.NOT_DONE;
            default:
                return StatusKind.DEFAULT;
        }
    }

    @StringRes
    public static int stringResForKind(@NonNull StatusKind kind) {
        switch (kind) {
            case CANCELED:
                return R.string.close_resone_canceled;
            case WAITING_DISPATCH:
                return R.string.close_resone_waiting_dispatch;
            case IN_WORK:
                return R.string.close_resone_in_work;
            case AT_START_POINT:
                return R.string.close_resone_in_start_point;
            case IN_ROUTE:
                return R.string.close_resone_in_rout;
            case COMPLETED:
                return R.string.close_resone_8;
            case CLIENT_REMOVED:
                return R.string.close_resone_1;
            case NOT_DONE:
                return R.string.close_resone_2;
            case DEFAULT:
            default:
                return R.string.close_resone_def;
        }
    }

    public static boolean isCanceled(
            @Nullable String closeReason,
            @Nullable String executionStatus,
            @Nullable String orderUid
    ) {
        if (isCanceledExecutionStatus(executionStatus)) {
            return true;
        }

        String cr = closeReason != null ? closeReason.trim() : "";
        if ("1".equals(cr) || "6".equals(cr) || "7".equals(cr) || "9".equals(cr)) {
            return true;
        }

        if (orderUid != null && !orderUid.isEmpty()) {
            String canceledUid = ExecutionStatusViewModel.getCanceledOrderUid();
            if (orderUid.equals(canceledUid) && ExecutionStatusViewModel.isUserCanceledPref()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isWaitingDispatch(
            @Nullable String executionStatus,
            @Nullable String requiredTime
    ) {
        if ("WaitingCarSearch".equalsIgnoreCase(executionStatus)) {
            return true;
        }
        return isScheduledBooking(requiredTime);
    }

    public static boolean isScheduledBooking(@Nullable String requiredTime) {
        if (requiredTime == null) {
            return false;
        }
        String trimmed = requiredTime.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            return false;
        }
        if (trimmed.startsWith("01.01.1970") || trimmed.startsWith("1970-01-01")) {
            return false;
        }
        return trimmed.matches("\\d{2}\\.\\d{2}\\.\\d{4}.*")
                || trimmed.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    private static boolean isCanceledExecutionStatus(@Nullable String executionStatus) {
        if (executionStatus == null) {
            return false;
        }
        return "Canceled".equalsIgnoreCase(executionStatus)
                || "Cancelled".equalsIgnoreCase(executionStatus);
    }
}
