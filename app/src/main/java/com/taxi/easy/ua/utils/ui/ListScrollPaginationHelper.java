package com.taxi.easy.ua.utils.ui;

import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Счётчик страниц, кнопки и вертикальная прокрутка для ListView с подсказками адресов.
 */
public final class ListScrollPaginationHelper {

    private final ListView listView;
    @Nullable
    private final TextView tvScrollPosition;
    private final View scrollButtonUp;
    private final View scrollButtonDown;
    @Nullable
    private final View scrollControlsRoot;

    public ListScrollPaginationHelper(ListView listView,
                                      TextView tvScrollPosition,
                                      View scrollButtonUp,
                                      View scrollButtonDown) {
        this(listView, tvScrollPosition, scrollButtonUp, scrollButtonDown, null);
    }

    public ListScrollPaginationHelper(ListView listView,
                                      @Nullable TextView tvScrollPosition,
                                      View scrollButtonUp,
                                      View scrollButtonDown,
                                      @Nullable View scrollControlsRoot) {
        this.listView = listView;
        this.tvScrollPosition = tvScrollPosition;
        this.scrollButtonUp = scrollButtonUp;
        this.scrollButtonDown = scrollButtonDown;
        this.scrollControlsRoot = scrollControlsRoot;
    }

    public void bind() {
        listView.setVerticalScrollBarEnabled(true);
        listView.setScrollbarFadingEnabled(false);
        listView.setNestedScrollingEnabled(true);
        listView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
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
        int totalItems = getItemCount();
        if (totalItems <= 0) {
            hideControls();
            return;
        }

        int firstVisible = Math.max(0, listView.getFirstVisiblePosition());
        int lastVisible = Math.max(firstVisible, listView.getLastVisiblePosition());
        int visibleOnScreen = Math.max(1, lastVisible - firstVisible + 1);

        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / visibleOnScreen));
        int currentPage = Math.min(totalPages, (firstVisible / visibleOnScreen) + 1);

        if (tvScrollPosition != null) {
            tvScrollPosition.setText(currentPage + " / " + totalPages);
        }

        boolean scrollNeeded = totalItems > visibleOnScreen || isContentTallerThanList();
        if (scrollNeeded) {
            setControlsRootVisible(true);
            if (tvScrollPosition != null) {
                tvScrollPosition.setVisibility(View.VISIBLE);
            }
            scrollButtonUp.setVisibility(View.VISIBLE);
            scrollButtonDown.setVisibility(View.VISIBLE);
            setButtonEnabled(scrollButtonUp, canScrollUp());
            setButtonEnabled(scrollButtonDown, canScrollDown());
        } else {
            setControlsRootVisible(totalItems > 0);
            if (tvScrollPosition != null) {
                tvScrollPosition.setVisibility(View.VISIBLE);
                tvScrollPosition.setText("1 / 1");
            }
            scrollButtonUp.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);
        }
    }

    public void scrollUp() {
        int pageSize = getVisiblePageSize();
        int target = Math.max(0, listView.getFirstVisiblePosition() - pageSize);
        listView.smoothScrollToPosition(target);
        listView.post(this::alignFirstVisibleItemToTop);
    }

    public void scrollDown() {
        int count = getItemCount();
        if (count == 0) {
            return;
        }
        int pageSize = getVisiblePageSize();
        int target = Math.min(count - 1, listView.getLastVisiblePosition() + pageSize);
        listView.smoothScrollToPosition(target);
        listView.post(() -> alignLastVisibleItemToBottom(false));
    }

    private int getVisiblePageSize() {
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();
        return Math.max(1, last - first + 1);
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
        if (tvScrollPosition != null) {
            tvScrollPosition.setVisibility(View.GONE);
        }
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
