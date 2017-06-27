package nl.psdcompany.duonavigationdrawer.views;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import nl.psdcompany.psd.duonavigationdrawer.R;

import static android.support.v4.widget.DrawerLayout.DrawerListener;

/**
 * Created by PSD on 28-02-17.
 */

public class DuoDrawerLayout extends RelativeLayout {
    /**
     * Indicates that any drawers are in an idle, settled state. No animation is in progress.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    /**
     * Indicates that a drawer is currently being dragged by the user.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    /**
     * Indicates that a drawer is in the process of settling to a final position.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;
    /**
     * The drawer is unlocked.
     */
    public static final int LOCK_MODE_UNLOCKED = 0;
    /**
     * The drawer is locked closed. The user may not open it, though
     * the app may open it programmatically.
     */
    public static final int LOCK_MODE_LOCKED_CLOSED = 1;
    /**
     * The drawer is locked open. The user may not close it, though the app
     * may close it programmatically.
     */
    public static final int LOCK_MODE_LOCKED_OPEN = 2;

    /**
     * Length of time to delay before peeking the drawer.
     */
    private static final int PEEK_DELAY = 160;

    private static final String TAG_MENU = "menu";
    private static final String TAG_CONTENT = "content";
    private static final String TAG_OVERLAY = "overlay";

    @LayoutRes
    private static final int DEFAULT_ATTRIBUTE_VALUE = -54321;
    private static final float CONTENT_SCALE_CLOSED = 1.0f;
    private static final float CONTENT_SCALE_OPEN = 0.7f;
    private static final float CLICK_TO_CLOSE_SCALE = 0.7f;
    private static final float MENU_SCALE_CLOSED = 1.1f;
    private static final float MENU_SCALE_OPEN = 1.0f;
    private static final float MENU_ALPHA_CLOSED = 0.0f;
    private static final float MENU_ALPHA_OPEN = 1.0f;
    private static final float MARGIN_FACTOR = 0.7f;

    private static final float MAX_ATTRIBUTE_MULTIPLIER = 100f;
    private static final float MAX_CLICK_RANGE = 300f;

    private float mContentScaleClosed = CONTENT_SCALE_CLOSED;
    private float mContentScaleOpen = CONTENT_SCALE_OPEN;
    private float mMenuScaleClosed = MENU_SCALE_CLOSED;
    private float mMenuScaleOpen = MENU_SCALE_OPEN;
    private float mMenuAlphaClosed = MENU_ALPHA_CLOSED;
    private float mMenuAlphaOpen = MENU_ALPHA_OPEN;
    private float mMarginFactor = MARGIN_FACTOR;
    private float mClickToCloseScale = CLICK_TO_CLOSE_SCALE;

    private float mDragOffset;
    private float mDraggedXOffset;
    private float mDraggedYOffset;

    @LockMode
    private int mLockMode;
    @State
    private int mDrawerState = STATE_IDLE;

    @LayoutRes
    private int mMenuViewId;
    @LayoutRes
    private int mContentViewId;

    private ViewDragHelper mViewDragHelper;
    private LayoutInflater mLayoutInflater;
    private DrawerListener mDrawerListener;
    private ViewDragCallback mViewDragCallback;

    private View mContentView;
    private View mMenuView;

    public DuoDrawerLayout(Context context) {
        this(context, null);
    }

    public DuoDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DuoDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readAttributes(attrs);
        initialize();
    }

    private void readAttributes(AttributeSet attributeSet) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.DuoDrawerLayout);

        try {
            mMenuViewId = typedArray.getResourceId(R.styleable.DuoDrawerLayout_menu, DEFAULT_ATTRIBUTE_VALUE);
            mContentViewId = typedArray.getResourceId(R.styleable.DuoDrawerLayout_content, DEFAULT_ATTRIBUTE_VALUE);
            mContentScaleClosed = typedArray.getFloat(R.styleable.DuoDrawerLayout_contentScaleClosed, CONTENT_SCALE_CLOSED);
            mContentScaleOpen = typedArray.getFloat(R.styleable.DuoDrawerLayout_contentScaleOpen, CONTENT_SCALE_OPEN);
            mMenuScaleClosed = typedArray.getFloat(R.styleable.DuoDrawerLayout_menuScaleClosed, MENU_SCALE_CLOSED);
            mMenuScaleOpen = typedArray.getFloat(R.styleable.DuoDrawerLayout_menuScaleOpen, MENU_SCALE_OPEN);
            mMenuAlphaClosed = typedArray.getFloat(R.styleable.DuoDrawerLayout_menuAlphaClosed, MENU_ALPHA_CLOSED);
            mMenuAlphaOpen = typedArray.getFloat(R.styleable.DuoDrawerLayout_menuAlphaOpen, MENU_ALPHA_OPEN);
            mMarginFactor = typedArray.getFloat(R.styleable.DuoDrawerLayout_marginFactor, MARGIN_FACTOR);
            mClickToCloseScale = typedArray.getFloat(R.styleable.DuoDrawerLayout_clickToCloseScale, CLICK_TO_CLOSE_SCALE);
        } finally {
            typedArray.recycle();
        }
    }

    private void initialize() {
        mLayoutInflater = LayoutInflater.from(getContext());
        mViewDragCallback = new ViewDragCallback();
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, mViewDragCallback);
        mViewDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);

        this.setFocusableInTouchMode(true);
        this.setClipChildren(false);
        this.requestFocus();
    }

    private float map(float x, float inMin, float inMax, float outMin, float outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        handleViews();

        mContentView.offsetLeftAndRight((int) mDraggedXOffset);
        mContentView.offsetTopAndBottom((int) mDraggedYOffset);
    }


    /**
     * Checks if it can find the menu & content views with their tags.
     * If this fails it will check for the corresponding attribute.
     * If this fails it wil throw an IllegalStateException.
     */
    private void handleViews() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);

            try {
                String tag = (String) view.getTag();
                if (tag.equals(TAG_CONTENT)) {
                    mContentView = view;
                } else if (tag.equals(TAG_MENU)) {
                    mMenuView = view;
                }
            } catch (Exception ignored) {
            }
            if (mContentView != null && mMenuView != null) break;
        }

        if (mMenuView == null) {
            checkForMenuAttribute();
        }

        if (mContentView == null) {
            checkForContentAttribute();
        }

        if (mDragOffset == 0) {
            setViewAndChildrenEnabled(mContentView, true);
            setViewAndChildrenEnabled(mMenuView, false);
        }
    }

    /**
     * Checks if it can inflate the menu view with its corresponding attribute.
     * If this fails it wil throw an IllegalStateException.
     */
    private void checkForMenuAttribute() {
        if (mMenuViewId == DEFAULT_ATTRIBUTE_VALUE) {
            throw new IllegalStateException("Missing menu layout. " +
                    "Set a \"menu\" tag on the menu layout (in XML android:xml=\"menu\"). " +
                    "Or set the \"app:menu\" attribute on the drawer layout.");
        }

        mMenuView = mLayoutInflater.inflate(mMenuViewId, this, false);

        if (mMenuView != null) {
            mMenuView.setTag(TAG_MENU);
            addView(mMenuView);
        }
    }

    /**
     * Checks if it can inflate the content view with its corresponding attribute.
     * If this fails it wil throw an IllegalStateException.
     */
    private void checkForContentAttribute() {
        if (mContentViewId == DEFAULT_ATTRIBUTE_VALUE) {
            throw new IllegalStateException("Missing content layout. " +
                    "Set a \"content\" tag on the content layout (in XML android:xml=\"content\"). " +
                    "Or set the \"app:content\" attribute on the drawer layout.");
        }

        mContentView = mLayoutInflater.inflate(mContentViewId, this, false);

        if (mContentView != null) {
            mContentView.setTag(TAG_CONTENT);
            addView(mContentView);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putFloat("dragOffset", mDragOffset);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = bundle.getParcelable("superState");
            if (bundle.getFloat("dragOffset", 0f) > .6f) {
                openDrawer();
            }
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isDrawerOpen() && keyCode == KeyEvent.KEYCODE_BACK) {
            closeDrawer();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            mDraggedXOffset = mContentView.getLeft();
            mDraggedYOffset = mContentView.getTop();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                mViewDragHelper.cancel();
                return false;
            }
        }

        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    /**
     * Hide the the touch interceptor.
     */
    private void hideTouchInterceptor() {
        if (findViewWithTag(TAG_OVERLAY) != null) {
            findViewWithTag(TAG_OVERLAY).setVisibility(INVISIBLE);
        }
    }

    /**
     * Show the the touch interceptor.
     */
    private void showTouchInterceptor() {
        if (findViewWithTag(TAG_OVERLAY) == null) {
            addTouchInterceptor();
        }

        if (mContentView == null) {
            mContentView = findViewWithTag(TAG_CONTENT);
        }

        float offset = map(mContentView.getLeft(), 0, DuoDrawerLayout.this.getWidth() * mMarginFactor, 0, 1);
        float scaleFactorContent = map(offset, 0, 1, mContentScaleClosed, mClickToCloseScale);

        View interceptor = findViewWithTag(TAG_OVERLAY);

        if (interceptor != null) {
            interceptor.setTranslationX(mContentView.getLeft());
            interceptor.setTranslationY(mContentView.getTop());
            interceptor.setScaleX(scaleFactorContent);
            interceptor.setScaleY(scaleFactorContent);
            interceptor.setVisibility(VISIBLE);
        }
    }

    /**
     * Boolean to check if a touch is a click.
     *
     * @return Returns true if a touch is a click.
     */
    private boolean touchIsClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > MAX_CLICK_RANGE || differenceY > MAX_CLICK_RANGE);
    }

    /**
     * Adds a touch interceptor to the layout when needed.
     * The interceptor wil take care of touch events occurring
     * on the content view when the drawer is open.
     */
    private void addTouchInterceptor() {
        View touchInterceptor = mLayoutInflater.inflate(R.layout.duo_overlay, this, false);
        touchInterceptor.setTag(TAG_OVERLAY);
        touchInterceptor.setOnTouchListener(new OnTouchListener() {
            float startX;
            float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLockMode != LOCK_MODE_LOCKED_OPEN) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            startY = event.getY();
                            break;
                        case MotionEvent.ACTION_UP:
                            float endX = event.getX();
                            float endY = event.getY();

                            if (touchIsClick(startX, endX, startY, endY)) {
                                closeDrawer();
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mViewDragCallback.mIsEdgeDrag = true;
                            int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                            mViewDragHelper.captureChildView(mContentView, pointerIndex);
                            break;
                    }
                    return true;
                } else return true;
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            touchInterceptor.setTranslationZ(MAX_ATTRIBUTE_MULTIPLIER);
        }

        addView(touchInterceptor);
    }

    /**
     * Disables/Enables a view and all of its child views.
     * Leaves the toolbar enabled at all times.
     *
     * @param view    The view to be disabled/enabled
     * @param enabled True or false, enabled/disabled
     */
    private void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof Toolbar) {
                    setViewAndChildrenEnabled(child, true);
                } else {
                    setViewAndChildrenEnabled(child, enabled);
                }
            }
        }
    }

    /**
     * Kept for compatibility. {@see #isDrawerOpen()}.
     *
     * @param gravity Ignored
     * @return true if the drawer view in in an open state
     */
    @SuppressWarnings("UnusedParameters")
    public boolean isDrawerOpen(int gravity) {
        return isDrawerOpen();
    }

    /**
     * Check if the drawer view is currently in an open state.
     * To be considered "open" the drawer must have settled into its fully
     * visible state.
     *
     * @return true if the drawer view is in an open state
     */
    public boolean isDrawerOpen() {
        return mDragOffset == 1;
    }

    /**
     * Kept for compatibility. {@see #openDrawer()}.
     *
     * @param gravity Ignored
     */
    @SuppressWarnings("UnusedParameters")
    public void openDrawer(int gravity) {
        openDrawer();
    }

    /**
     * Open the drawer animated.
     */
    public void openDrawer() {
        int drawerWidth = (int) (getWidth() * mMarginFactor);
        if (drawerWidth == 0) {
            getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    DuoDrawerLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (mViewDragHelper.smoothSlideViewTo(mContentView, ((int) (getWidth() * mMarginFactor)), mContentView.getTop())) {
                        ViewCompat.postInvalidateOnAnimation(DuoDrawerLayout.this);
                    }
                    return false;
                }
            });
        } else {
            if (mViewDragHelper.smoothSlideViewTo(mContentView, drawerWidth, mContentView.getTop())) {
                ViewCompat.postInvalidateOnAnimation(DuoDrawerLayout.this);
            }
        }
    }

    /**
     * Kept for compatibility. {@see #closeDrawer()}.
     *
     * @param gravity Ignored
     */
    @SuppressWarnings("UnusedParameters")
    public void closeDrawer(int gravity) {
        closeDrawer();
    }

    /**
     * Close the drawer animated.
     */
    public void closeDrawer() {
        if (mContentView == null) {
            mContentView = findViewWithTag(TAG_CONTENT);
        }

        if (mContentView != null) {
            if (mViewDragHelper.smoothSlideViewTo(mContentView, 0 - mContentView.getPaddingLeft(), mContentView.getTop())) {
                ViewCompat.postInvalidateOnAnimation(DuoDrawerLayout.this);
            }
        }
    }

    /**
     * Kept for compatibility. {@see #isDrawerVisible()}.
     *
     * @param gravity Ignored.
     */
    @SuppressWarnings("UnusedParameters")
    public boolean isDrawerVisible(int gravity) {
        return isDrawerVisible();
    }

    /**
     * Check if the drawer is visible on the screen.
     *
     * @return true if the drawer is visible.
     */
    public boolean isDrawerVisible() {
        return mDragOffset > 0;
    }

    /**
     * Enable or disable interaction the drawer.
     * <p>
     * This allows the application to restrict the user's ability to open or close
     * any drawer within this layout. DuloDrawerLayout will still respond to calls to
     * {@link #openDrawer()}, {@link #closeDrawer()} and friends if a drawer is locked.
     * <p>
     * Locking drawers open or closed will implicitly open or close
     * any drawers as appropriate.
     *
     * @param lockMode The new lock mode. One of {@link #LOCK_MODE_UNLOCKED},
     *                 {@link #LOCK_MODE_LOCKED_CLOSED} or {@link #LOCK_MODE_LOCKED_OPEN}.
     * @see #LOCK_MODE_UNLOCKED
     * @see #LOCK_MODE_LOCKED_CLOSED
     * @see #LOCK_MODE_LOCKED_OPEN
     */
    public void setDrawerLockMode(@LockMode int lockMode) {
        mLockMode = lockMode;
        switch (lockMode) {
            case LOCK_MODE_LOCKED_CLOSED:
                mViewDragHelper.cancel();
                closeDrawer();
                break;
            case LOCK_MODE_LOCKED_OPEN:
                mViewDragHelper.cancel();
                openDrawer();
                break;
            case LOCK_MODE_UNLOCKED:
                break;
        }
    }

    /**
     * Returns the menu view.
     *
     * @return The current menu view.
     */
    public View getMenuView() {
        if (mMenuView == null) {
            mMenuView = this.findViewWithTag(TAG_MENU);
        }
        return mMenuView;
    }

    /**
     * Sets the menu view.
     *
     * @param menuView View that becomes the menu view.
     */
    public void setMenuView(View menuView) {
        if (menuView.getParent() != null) {
            throw new IllegalStateException("Your menu view already has a parent. Please make sure your menu view does not have a parent.");
        }

        mMenuView = this.findViewWithTag(TAG_MENU);
        if (mMenuView != null) {
            this.removeView(mMenuView);
        }
        mMenuView = menuView;
        mMenuView.setTag(TAG_MENU);
        addView(mMenuView);
        invalidate();
        requestLayout();
    }

    /**
     * Returns the content view.
     *
     * @return The current content view.
     */
    public View getContentView() {
        if (mContentView == null) {
            mContentView = this.findViewWithTag(TAG_CONTENT);
        }
        return mContentView;
    }

    /**
     * Sets the content view.
     *
     * @param contentView View that becomes the content view.
     */
    public void setContentView(View contentView) {
        if (contentView.getParent() != null) {
            throw new IllegalStateException("Your content view already has a parent. Please make sure your content view does not have a parent.");
        }

        mContentView = this.findViewWithTag(TAG_CONTENT);
        if (mContentView != null) {
            this.removeView(mContentView);
        }
        mContentView = contentView;
        mContentView.setTag(TAG_CONTENT);
        addView(mContentView);
        invalidate();
        requestLayout();
    }

    /**
     * Set the scale of the content when the drawer is closed. 1.0f is the original size.
     *
     * @param contentScaleClosed Scale of the content if the drawer is closed.
     */
    public void setContentScaleClosed(float contentScaleClosed) {
        mContentScaleClosed = contentScaleClosed;
        invalidate();
        requestLayout();
    }

    /**
     * Set the scale of the content when the drawer is open. 1.0f is the original size.
     *
     * @param contentScaleOpen Scale of the content when the drawer is open.
     */
    public void setContentScaleOpen(float contentScaleOpen) {
        mContentScaleOpen = contentScaleOpen;
        invalidate();
        requestLayout();
    }

    /**
     * Set the scale of the menu when the drawer is closed. 1.0f is the original size.
     *
     * @param menuScaleClosed Scale of the menu when the drawer is closed.
     */
    public void setMenuScaleClosed(float menuScaleClosed) {
        mMenuScaleClosed = menuScaleClosed;
        invalidate();
        requestLayout();
    }

    /**
     * Set the scale of the menu when the drawer is open. 1.0f is the original size.
     *
     * @param menuScaleOpen Scale of the menu when the drawer is open.
     */
    public void setMenuScaleOpen(float menuScaleOpen) {
        mMenuScaleOpen = menuScaleOpen;
        invalidate();
        requestLayout();
    }

    /**
     * Set the scale of the click to close surface when the drawer is open. 0.7f is the original scaling.
     *
     * @param clickToCloseScale Scale of the click to close surface when the drawer is open.
     */
    public void setClickToCloseScale(float clickToCloseScale) {
        mClickToCloseScale = clickToCloseScale;
        invalidate();
        requestLayout();
    }

    /**
     * Set the alpha of the menu when the drawer is closed.
     * 0.0f is transparent, 1.0f is completely visible.
     *
     * @param menuAlphaClosed Alpha of the menu when the drawer is closed.
     */
    public void setMenuAlphaClosed(float menuAlphaClosed) {
        mMenuAlphaClosed = menuAlphaClosed;
        invalidate();
        requestLayout();
    }

    /**
     * Set the alpha of the menu when the drawer is open.
     * 0.0f is transparent, 1.0f is completely visible.
     *
     * @param menuAlphaOpen Alpha of the menu when the drawer is open.
     */
    public void setMenuAlphaOpen(float menuAlphaOpen) {
        mMenuAlphaOpen = menuAlphaOpen;
        invalidate();
        requestLayout();
    }

    /**
     * Set the amount of space of the content visible when the drawer is opened.
     * 1.0f will move the drawer completely of the screen. The default value is 0.7f.
     *
     * @param marginFactor Amount of space of the content when drawer is open.
     */
    public void setMarginFactor(float marginFactor) {
        mMarginFactor = marginFactor;
        invalidate();
        requestLayout();
    }

    /**
     * Set a listener to be notified of drawer events.
     *
     * @param drawerListener Listener to notify when drawer events occur
     * @see DrawerListener
     */
    public void setDrawerListener(DrawerListener drawerListener) {
        mDrawerListener = drawerListener;
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        boolean mIsEdgeDrag = false;

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return (mLockMode == LOCK_MODE_UNLOCKED || mLockMode == LOCK_MODE_LOCKED_OPEN) && child == mContentView && mIsEdgeDrag;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (left < 0) return 0;
            int width = (int) (getWidth() * mMarginFactor);
            if (left > width) return width;
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return getTopInset();
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            mIsEdgeDrag = true;

            if (tryCaptureView(mContentView, pointerId) && edgeFlags == ViewDragHelper.EDGE_LEFT) {
                mViewDragHelper.captureChildView(mContentView, pointerId);
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            super.onEdgeTouched(edgeFlags, pointerId);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return DuoDrawerLayout.this.getMeasuredWidth();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (xvel > 0 || xvel == 0 && mDragOffset > 0.5f) {
                openDrawer();
            } else {
                closeDrawer();
            }

            mIsEdgeDrag = false;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            mDragOffset = map(left, 0, DuoDrawerLayout.this.getWidth() * mMarginFactor, 0, 1);

            float scaleFactorContent = map(mDragOffset, 0, 1, mContentScaleClosed, mContentScaleOpen);
            mContentView.setScaleX(scaleFactorContent);
            mContentView.setScaleY(scaleFactorContent);

            float scaleFactorMenu = map(mDragOffset, 0, 1, mMenuScaleClosed, mMenuScaleOpen);
            mMenuView.setScaleX(scaleFactorMenu);
            mMenuView.setScaleY(scaleFactorMenu);

            float alphaValue = map(mDragOffset, 0, 1, mMenuAlphaClosed, mMenuAlphaOpen);
            mMenuView.setAlpha(alphaValue);

            if (mDrawerListener != null) {
                mDrawerListener.onDrawerSlide(DuoDrawerLayout.this, mDragOffset);
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);

            if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                    && mDragOffset >= .6f) {
                mDragOffset = 1;
            }

            mDraggedXOffset = mContentView.getLeft();
            mDraggedYOffset = mContentView.getTop();

            if (state == STATE_IDLE) {
                if (mDragOffset == 0) {
                    hideTouchInterceptor();
                    setViewAndChildrenEnabled(mMenuView, false);

                    if (mDrawerListener != null) {
                        mDrawerListener.onDrawerClosed(DuoDrawerLayout.this);
                    }
                } else if (mDragOffset == 1) {
                    showTouchInterceptor();
                    setViewAndChildrenEnabled(mMenuView, true);

                    if (mDrawerListener != null) {
                        mDrawerListener.onDrawerOpened(DuoDrawerLayout.this);
                    }
                }
            }

            if (state != mDrawerState) {
                mDrawerState = state;

                if (mDrawerListener != null) {
                    mDrawerListener.onDrawerStateChanged(state);
                }
            }
        }

        private int getTopInset() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
            if (!mContentView.getFitsSystemWindows()) return 0;

            int result = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        }
    }

    /**
     * @hide
     */
    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    /**
     * @hide
     */
    @IntDef({LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_CLOSED, LOCK_MODE_LOCKED_OPEN})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LockMode {
    }
}
