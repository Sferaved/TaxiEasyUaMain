package com.taxi.easy.ua.utils.ui;

import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Счётчик страниц и видимость кнопок прокрутки для ListView.
 */
public final class ListScrollPaginationHelper {

    private final ListView listView;
    private final TextView tvScrollPosition;
    private final View scrollButtonUp;
    private final View scrollButtonDown;
    private final View scrollControlsRoot;

    public ListScrollPaginationHelper(ListView listView,
                                      TextView tvScrollPosition,
                                      View scrollButtonUp,
                                      View scrollButtonDown) {
        this(listView, tvScrollPosition, scrollButtonUp, scrollButtonDown, null);
    }

    public ListScrollPaginationHelper(ListView listView,
                                      TextView tvScrollPosition,
                                      View scrollButtonUp,
                                      View scrollButtonDown,
                                      View scrollControlsRoot) {
        this.listView = listView;
        this.tvScrollPosition = tvScrollPosition;
        this.scrollButtonUp = scrollButtonUp;
        this.scrollButtonDown = scrollButtonDown;
        this.scrollControlsRoot = scrollControlsRoot;
    }

    public void bind() {
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                update();
            }
        });
        listView.post(this::update);
    }

    public void update() {
        int totalItems = listView.getAdapter() != null ? listView.getAdapter().getCount() : 0;
        if (totalItems <= 0) {
            hideControls();
            return;
        }

        int firstVisible = Math.max(0, listView.getFirstVisiblePosition());
        int lastVisible = Math.max(firstVisible, listView.getLastVisiblePosition());
        int visibleOnScreen = lastVisible - firstVisible + 1;
        if (visibleOnScreen <= 0) {
            visibleOnScreen = 1;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / visibleOnScreen));
        int currentPage = Math.min(totalPages, (firstVisible / visibleOnScreen) + 1);

        tvScrollPosition.setText(currentPage + " / " + totalPages);

        boolean scrollNeeded = totalItems > visibleOnScreen || isContentTallerThanList();
        if (scrollNeeded) {
            setControlsRootVisible(true);
            tvScrollPosition.setVisibility(View.VISIBLE);
            scrollButtonUp.setVisibility(View.VISIBLE);
            scrollButtonDown.setVisibility(View.VISIBLE);
            setButtonEnabled(scrollButtonUp, currentPage > 1);
            setButtonEnabled(scrollButtonDown, currentPage < totalPages);
        } else {
            setControlsRootVisible(totalItems > 0);
            tvScrollPosition.setVisibility(View.VISIBLE);
            tvScrollPosition.setText("1 / 1");
            scrollButtonUp.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);
        }
    }

    private boolean isContentTallerThanList() {
        int listHeight = listView.getHeight();
        if (listHeight <= 0) {
            return false;
        }
        int childrenHeight = 0;
        for (int i = 0; i < listView.getChildCount(); i++) {
            childrenHeight += listView.getChildAt(i).getHeight();
        }
        return childrenHeight > listHeight;
    }

    private void hideControls() {
        setControlsRootVisible(false);
        tvScrollPosition.setVisibility(View.GONE);
        scrollButtonUp.setVisibility(View.GONE);
        scrollButtonDown.setVisibility(View.GONE);
    }

    private void setControlsRootVisible(boolean visible) {
        if (scrollControlsRoot != null) {
            scrollControlsRoot.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private static void setButtonEnabled(View button, boolean enabled) {
        button.setEnabled(enabled);
        button.setClickable(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
    }
}
