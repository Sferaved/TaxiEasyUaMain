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

    /**
     * Подключает стрелки вверх/вниз к {@link #scrollUp()} и {@link #scrollDown()}.
     */
    public void wireScrollButtons() {
        scrollButtonDown.setOnClickListener(v -> {
            scrollDown();
            listView.postDelayed(this::update, 300);
        });
        scrollButtonUp.setOnClickListener(v -> {
            scrollUp();
            listView.postDelayed(this::update, 300);
        });
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
            setButtonEnabled(scrollButtonUp, canScrollUp());
            setButtonEnabled(scrollButtonDown, canScrollDown());
        } else {
            setControlsRootVisible(totalItems > 0);
            tvScrollPosition.setVisibility(View.VISIBLE);
            tvScrollPosition.setText("1 / 1");
            scrollButtonUp.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);
        }
    }

    /** Прокрутка на одну «страницу» вверх (к началу списка). */
    public void scrollUp() {
        int prev = listView.getFirstVisiblePosition() - 1;
        if (prev >= 0) {
            listView.smoothScrollToPosition(prev);
        } else {
            listView.smoothScrollToPosition(0);
        }
        listView.post(this::alignFirstVisibleItemToTop);
    }

    /** Прокрутка на одну «страницу» вниз (к концу списка). */
    public void scrollDown() {
        int count = getItemCount();
        if (count == 0) {
            return;
        }
        int next = listView.getLastVisiblePosition() + 1;
        if (next < count) {
            listView.smoothScrollToPosition(next);
        }
        listView.post(() -> alignLastVisibleItemToBottom(false));
    }

    private int getItemCount() {
        return listView.getAdapter() != null ? listView.getAdapter().getCount() : 0;
    }

    private boolean canScrollUp() {
        if (listView.canScrollVertically(-1)) {
            return true;
        }
        if (listView.getFirstVisiblePosition() > 0) {
            return true;
        }
        View firstChild = listView.getChildAt(0);
        if (firstChild == null) {
            return false;
        }
        return firstChild.getTop() < listView.getPaddingTop();
    }

    private boolean canScrollDown() {
        int count = getItemCount();
        if (count == 0) {
            return false;
        }
        if (listView.canScrollVertically(1)) {
            return true;
        }
        if (listView.getLastVisiblePosition() < count - 1) {
            return true;
        }
        View lastChild = getLastVisibleChild();
        if (lastChild == null) {
            return false;
        }
        int listBottom = listView.getHeight() - listView.getPaddingBottom();
        return lastChild.getBottom() > listBottom;
    }

    private View getLastVisibleChild() {
        int lastIndex = listView.getLastVisiblePosition() - listView.getFirstVisiblePosition();
        if (lastIndex < 0 || lastIndex >= listView.getChildCount()) {
            return null;
        }
        return listView.getChildAt(lastIndex);
    }

    private void alignFirstVisibleItemToTop() {
        if (!canScrollUp()) {
            update();
            return;
        }
        View firstChild = listView.getChildAt(0);
        if (firstChild == null) {
            update();
            return;
        }
        int delta = firstChild.getTop() - listView.getPaddingTop();
        if (delta < 0) {
            listView.smoothScrollBy(delta, 250);
        }
        listView.post(this::update);
    }

    private void alignLastVisibleItemToBottom(boolean retried) {
        int count = getItemCount();
        if (count == 0) {
            update();
            return;
        }
        int lastPosition = count - 1;
        int childIndex = lastPosition - listView.getFirstVisiblePosition();
        if (childIndex < 0 || childIndex >= listView.getChildCount()) {
            if (!retried) {
                listView.smoothScrollToPosition(lastPosition);
                listView.post(() -> alignLastVisibleItemToBottom(true));
            } else {
                listView.post(this::update);
            }
            return;
        }
        View lastChild = listView.getChildAt(childIndex);
        int listBottom = listView.getHeight() - listView.getPaddingBottom();
        int delta = lastChild.getBottom() - listBottom;
        if (delta > 0) {
            listView.smoothScrollBy(delta, 250);
        }
        listView.post(this::update);
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
