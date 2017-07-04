package nl.psdcompany.duonavigationdrawer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Px;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

public class DuoDrawerLayout extends ViewGroup {
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

    private static final String DUO_TAG_CONTENT = "duo_content";
    private static final String DUO_TAG_SIDE_MENU = "duo_side_menu";
    private static final String DUO_TAG_NAV_DRAWER_MENU = "duo_nav_drawer_menu";

    // Defaults
    private static final int MIN_FLING_VELOCITY = 400;
    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;

    // Variables
    private int mScrimColor = DEFAULT_SCRIM_COLOR;

    private boolean mChildrenCanceledTouch;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private float mScrimOpacity;
    private int mDrawerState;

    private ViewDragHelper mViewDragHelperEnd;
    private ViewDragHelper mViewDragHelperStart;
    private DrawerListener mDrawerListener;
    private Drawable mShadowRight;
    private Paint mScrimPaint = new Paint();
    private View mContentView;
    private View mSideMenuView;
    private View mDrawerMenuView;

    public enum Edge {
        START,
        END
    }

    /**
     * Listener for monitoring events about drawers.
     */
    public interface DrawerListener {
        /**
         * Called when a drawer's position changes.
         *
         * @param pDrawerView  The child view that was moved
         * @param pSlideOffset The new offset of this drawer within its range, from 0-1
         */
        public void onDrawerSlide(View pDrawerView, float pSlideOffset);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param pDrawerView Drawer view that is now open
         */
        public void onDrawerOpened(View pDrawerView);

        /**
         * Called when a drawer has settled in a completely closed state.
         *
         * @param pDrawerView Drawer view that is now closed
         */
        public void onDrawerClosed(View pDrawerView);

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         *
         * @param pNewState The new drawer motion state
         */
        public void onDrawerStateChanged(int pNewState);
    }

    /**
     * Stub/no-op implementations of all methods of {@link DrawerListener}.
     * Override this if you only care about a few of the available callback methods.
     */
    public static abstract class SimpleDrawerListener implements DrawerListener {
        @Override
        public void onDrawerSlide(View pDrawerView, float pSlideOffset) {
        }

        @Override
        public void onDrawerOpened(View pDrawerView) {
        }

        @Override
        public void onDrawerClosed(View pDrawerView) {
        }

        @Override
        public void onDrawerStateChanged(int pNewState) {
        }
    }

    public DuoDrawerLayout(Context context) {
        this(context, null);
    }

    public DuoDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DuoDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;

        ViewDragCallback viewDragCallbackStart = new ViewDragCallback(Edge.START);
        mViewDragHelperStart = ViewDragHelper.create(this, 1.f, viewDragCallbackStart);
        mViewDragHelperStart.setMinVelocity(minVel);
        viewDragCallbackStart.setViewDragHelper(mViewDragHelperStart);

        ViewDragCallback viewDragCallbackEnd = new ViewDragCallback(Edge.END);
        mViewDragHelperEnd = ViewDragHelper.create(this, 1.f, viewDragCallbackEnd);
        mViewDragHelperEnd.setMinVelocity(minVel);
        viewDragCallbackEnd.setViewDragHelper(mViewDragHelperEnd);

        // So that we can catch the back button
        setFocusableInTouchMode(true);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
    }

    /**
     * Set a listener to be notified of drawer events.
     *
     * @param pDrawerListener Listener to notify when drawer events occur
     * @see DrawerListener
     */
    public void setDrawerListener(DrawerListener pDrawerListener) {
        mDrawerListener = pDrawerListener;
    }

    /**
     * Set a simple drawable used for the right shadow.
     * The drawable provided must have a nonzero intrinsic width.
     *
     * @param shadowDrawable Shadow drawable to use at the edge of a drawer
     */
    public void setDrawerShadowEnd(Drawable shadowDrawable) {
        mShadowRight = shadowDrawable;
        invalidate();
    }

    /**
     * Set a simple drawable used for the right shadow.
     * The drawable provided must have a nonzero intrinsic width.
     *
     * @param resId Resource id of a shadow drawable to use at the edge of a drawer
     */
    public void setDrawerShadowEnd(int resId) {
        setDrawerShadowEnd(ContextCompat.getDrawable(getContext(), resId));
    }

    /**
     * Set a color to use for the scrim that obscures primary content while a drawer is open.
     *
     * @param color Color to use in 0xAARRGGBB format.
     */
    public void setScrimColor(int color) {
        mScrimColor = color;
        invalidate();
    }

    /**
     * Kept for compatibility. {@see isDrawerListener()}.
     *
     * @param gravity Ignored
     * @return true if the drawer menu view in in an open state
     */
    @SuppressWarnings("UnusedParameters")
    public boolean isDrawerMenuOpen(int gravity) {
        return isDrawerMenuOpen();
    }

    /**
     * Check if the drawer menu view is currently in an open state.
     * To be considered "open" the drawer must have settled into its fully
     * visible state.
     *
     * @return true if the drawer view is in an open state
     */
    public boolean isDrawerMenuOpen() {
        return getContentViewOffset() == 1;
    }

    /**
     * Check if the side menu view is currently in an open state.
     * To be considered "open" the drawer must have settled into its fully
     * visible state.
     *
     * @return true if the drawer view is in an open state
     */
    public boolean isSideMenuOpen() {
        return getSideMenuOffset() == 1;
    }

    /**
     * Kept for compatibility. {@see #isDrawerMenuVisible()}.
     *
     * @param gravity Ignored.
     */
    @SuppressWarnings("UnusedParameters")
    public boolean isDrawerMenuVisible(int gravity) {
        return isDrawerMenuVisible();
    }

    /**
     * Check if the drawer menu is visible on the screen.
     *
     * @return true if the drawer is visible.
     */
    public boolean isDrawerMenuVisible() {
        return getContentViewOffset() > 0;
    }

    /**
     * Check if the side menu is visible on the screen.
     *
     * @return true if the drawer is visible.
     */
    public boolean isSideMenuVisible() {
        return getSideMenuOffset() > 0;
    }

    /**
     * Kept for compatibility. {@see #closeDrawer()}.
     *
     * @param gravity Ignored
     */
    @SuppressWarnings("UnusedParameters")
    public void closeDrawer(int gravity) {
        final int absGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
        if (absGravity == Gravity.LEFT || absGravity == Gravity.START) {
            closeDrawer(Edge.START);
        } else if (absGravity == Gravity.RIGHT || absGravity == Gravity.END) {
            closeDrawer(Edge.END);
        }
    }

    /**
     * Close the specified drawer view by animating it into view.
     *
     * @param pEdge Edge of the drawer view that has to close
     */
    public void closeDrawer(Edge pEdge) {
        if (pEdge == Edge.START) {
            closeDrawer(mContentView);
        } else if (pEdge == Edge.END) {
            closeDrawer(mSideMenuView);
        }

        invalidate();
    }

    /**
     * Close the specified drawer view by animating it into view.
     *
     * @param pDrawerView Drawer view to close
     */
    public void closeDrawer(View pDrawerView) {
        if (isContentView(pDrawerView)) {
            mViewDragHelperStart.smoothSlideViewTo(mContentView, 0, mContentView.getTop());
        } else if (isSideMenuView(pDrawerView)) {
            mViewDragHelperEnd.smoothSlideViewTo(mSideMenuView, getWidth(), mContentView.getTop());
        }

        invalidate();
    }

    /**
     * Close all currently open drawer views by animating them out of view.
     */
    public void closeDrawers() {
        closeDrawer(Edge.END);
        closeDrawer(Edge.START);
        invalidate();
    }

    /**
     * Kept for compatibility. {@see #closeDrawer()}.
     *
     * @param gravity Ignored
     */
    @SuppressWarnings("UnusedParameters")
    public void openDrawer(int gravity) {
        final int absGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
        if (absGravity == Gravity.LEFT || absGravity == Gravity.START) {
            openDrawer(Edge.START);
        } else if (absGravity == Gravity.RIGHT || absGravity == Gravity.END) {
            openDrawer(Edge.END);
        }
    }

    /**
     * Open the specified drawer view by animating it into view.
     *
     * @param pEdge Edge of the drawer view that has to open
     */
    public void openDrawer(Edge pEdge) {
        if (pEdge == Edge.START) {
            openDrawer(mContentView);
        } else if (pEdge == Edge.END) {
            openDrawer(mSideMenuView);
        }

        invalidate();
    }

    /**
     * Open the specified drawer view by animating it into view.
     *
     * @param pDrawerView Drawer view to open
     */
    public void openDrawer(View pDrawerView) {
        if (isContentView(pDrawerView)) {
            mViewDragHelperStart.smoothSlideViewTo(mContentView, (int) (getWidth() * 0.7f), mContentView.getTop());
        } else if (isSideMenuView(pDrawerView)) {
            mViewDragHelperEnd.smoothSlideViewTo(mSideMenuView, getWidth() - mSideMenuView.getWidth(), mContentView.getTop());
        }

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            if (isInEditMode()) {
                if (widthMode == MeasureSpec.UNSPECIFIED) {
                    widthSize = 300;
                }
                if (heightMode == MeasureSpec.UNSPECIFIED) {
                    heightSize = 300;
                }
            } else {
                throw new IllegalArgumentException(
                        "DuoDrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final DuoDrawerLayout.LayoutParams lp = (DuoDrawerLayout.LayoutParams) child.getLayoutParams();

            if (isSideMenuView(child)) {
                final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                        lp.leftMargin + lp.rightMargin,
                        lp.width);
                final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                        lp.topMargin + lp.bottomMargin,
                        lp.height);
                child.measure(drawerWidthSpec, drawerHeightSpec);
            } else {
                final int contentWidthSpec =
                        MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec =
                        MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mContentView = findViewWithTag(DUO_TAG_CONTENT);
        mSideMenuView = findViewWithTag(DUO_TAG_SIDE_MENU);
        mDrawerMenuView = findViewWithTag(DUO_TAG_NAV_DRAWER_MENU);

        if (mContentView != null) {
            mContentView.bringToFront();
        }
        if (mSideMenuView != null) {
            mSideMenuView.bringToFront();
        }

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int childHeight = child.getMeasuredHeight();
            int childWidth = child.getMeasuredWidth();

            if (isSideMenuView(child)) {
                if (childWidth < getMeasuredWidth() * 0.3) {
                    childWidth = (int) (getMeasuredWidth() * 0.3);
                }

                child.layout(getMeasuredWidth() + lp.leftMargin, lp.topMargin,
                        getMeasuredWidth() + childWidth - lp.rightMargin,
                        childHeight - lp.bottomMargin);
            } else {
                child.layout(lp.leftMargin, lp.topMargin,
                        child.getMeasuredWidth() - lp.rightMargin,
                        child.getMeasuredHeight() - lp.bottomMargin);
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (isSideMenuView(child) && isSideMenuVisible()) {
            if (mScrimOpacity > 0 && mShadowRight == null) {
                final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
                final int imag = (int) (baseAlpha * mScrimOpacity);
                final int color = imag << 24 | (mScrimColor & 0xffffff);
                mScrimPaint.setColor(color);
                canvas.drawRect(0, 0, getWidth(), getHeight(), mScrimPaint);
            } else if (mShadowRight != null) {
                final int shadowWidth = mShadowRight.getIntrinsicWidth();
                final int childLeft = child.getLeft();
                final int showing = getWidth() - childLeft;
                final int drawerPeekDistance = mViewDragHelperEnd.getEdgeSize();
                final float alpha = Math.max(0, Math.min((float) showing / drawerPeekDistance, 1.f));

                mShadowRight.setBounds(childLeft - shadowWidth, child.getTop(), childLeft, child.getBottom());
                mShadowRight.setAlpha((int) (0xff * alpha));
                mShadowRight.draw(canvas);
            }
        }

        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void computeScroll() {
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            scrimOpacity = Math.max(scrimOpacity, ((LayoutParams) getChildAt(i).getLayoutParams()).getOffset());
        }

        mScrimOpacity = scrimOpacity;

        if (mViewDragHelperStart.continueSettling(true) | mViewDragHelperEnd.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);


        final boolean interceptForDrag = mViewDragHelperStart.shouldInterceptTouchEvent(ev) |
                mViewDragHelperEnd.shouldInterceptTouchEvent(ev);

        boolean interceptForTap = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (mScrimOpacity > 0 && isContentView(mViewDragHelperStart.findTopChildUnder((int) x, (int) y))) {
                    interceptForTap = true;
                }
                mChildrenCanceledTouch = false;
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                closeDrawers();
                mChildrenCanceledTouch = false;
            }
        }
        return interceptForDrag || interceptForTap || mChildrenCanceledTouch;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();

        mViewDragHelperStart.processTouchEvent(ev);
        mViewDragHelperEnd.processTouchEvent(ev);

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                mChildrenCanceledTouch = false;
                break;
            }
            case MotionEvent.ACTION_UP: {
                final float x = ev.getX();
                final float y = ev.getY();
                final View touchedView = mViewDragHelperStart.findTopChildUnder((int) x, (int) y);

                if (touchedView != null && isContentView(touchedView)) {
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    final int slop = mViewDragHelperStart.getTouchSlop();

                    if (dx * dx + dy * dy < slop * slop) {
                        closeDrawers();
                    }
                }

                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                closeDrawers();
                mChildrenCanceledTouch = false;
                break;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && findOpenDrawer() != null) {
            KeyEventCompat.startTracking(event);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final View visibleDrawer = findOpenDrawer();
            if (visibleDrawer != null) {
                closeDrawers();
            }
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    private float getSideMenuOffset() {
        if (mSideMenuView == null) {
            mSideMenuView = findViewWithTag(DUO_TAG_SIDE_MENU);
        }

        return ((LayoutParams) mSideMenuView.getLayoutParams()).getOffset();
    }

    private float getContentViewOffset() {
        if (mContentView == null) {
            mContentView = findViewWithTag(DUO_TAG_CONTENT);
        }

        return ((LayoutParams) mContentView.getLayoutParams()).getOffset();
    }

    private boolean isContentView(View pView) {
        return pView != null && pView.getTag() != null && pView.getTag().equals(DUO_TAG_CONTENT);
    }

    private boolean isSideMenuView(View pView) {
        return pView != null && pView.getTag() != null && pView.getTag().equals(DUO_TAG_SIDE_MENU);
    }

    private float map(float x, float inMin, float inMax, float outMin, float outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    private View findOpenDrawer() {
        if (((LayoutParams) mSideMenuView.getLayoutParams()).getOffset() == 1) {
            return mSideMenuView;
        } else if (((LayoutParams) mContentView.getLayoutParams()).getOffset() == 1) {
            return mSideMenuView;
        } else return null;
    }

    private void updateDrawerState(Edge pEdge, int activeState) {
        final int stateStart = mViewDragHelperStart.getViewDragState();
        final int stateEnd = mViewDragHelperEnd.getViewDragState();

        final int state;
        if (stateStart == STATE_DRAGGING || stateEnd == STATE_DRAGGING) {
            state = STATE_DRAGGING;
        } else if (stateStart == STATE_SETTLING || stateEnd == STATE_SETTLING) {
            state = STATE_SETTLING;
        } else {
            state = STATE_IDLE;
        }

        final LayoutParams layoutParamsEnd = (LayoutParams) mSideMenuView.getLayoutParams();
        final LayoutParams layoutParamsStart = (LayoutParams) mContentView.getLayoutParams();

        if (activeState == STATE_IDLE) {
            if (pEdge == Edge.END && layoutParamsEnd.getOffset() == 1) {
                dispatchOnDrawerOpened(mSideMenuView);
            } else if (pEdge == Edge.START && layoutParamsStart.getOffset() == 1) {
                dispatchOnDrawerOpened(mContentView);
            } else if (pEdge == Edge.END && layoutParamsEnd.getOffset() == 0) {
                dispatchOnDrawerClosed(mSideMenuView);
            } else if (pEdge == Edge.START && layoutParamsStart.getOffset() == 0) {
                dispatchOnDrawerClosed(mContentView);
            }
        }

        if (state != mDrawerState) {
            mDrawerState = state;
            if (mDrawerListener != null) {
                mDrawerListener.onDrawerStateChanged(state);
            }
        }
    }

    private void dispatchOnDrawerClosed(View drawerView) {
        if (mDrawerListener != null) {
            mDrawerListener.onDrawerClosed(drawerView);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnDrawerOpened(View drawerView) {
        if (mDrawerListener != null) {
            mDrawerListener.onDrawerOpened(drawerView);
        }
        drawerView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void dispatchOnMenuDrawerSlide(View drawerView, float slideOffset) {
        if (mDrawerListener != null) {
            mDrawerListener.onDrawerSlide(drawerView, slideOffset);
        }
    }

    private void setViewOffset(View pView, float pSlideOffset) {
        final LayoutParams lp = (LayoutParams) pView.getLayoutParams();
        if (pSlideOffset == lp.getOffset()) {
            return;
        }
        lp.setOffset(pSlideOffset);
        dispatchOnMenuDrawerSlide(pView, pSlideOffset);
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private final Edge mEdge;
        private ViewDragHelper mViewDragHelper;

        ViewDragCallback(Edge pEdge) {
            mEdge = pEdge;
        }

        void setViewDragHelper(ViewDragHelper pViewDragHelper) {
            mViewDragHelper = pViewDragHelper;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (isContentView(child) && getSideMenuOffset() == 0) {
                if (mEdge == Edge.START) {
                    if (mViewDragHelperStart.isEdgeTouched(ViewDragHelper.EDGE_LEFT, pointerId) && getContentViewOffset() != 1f) {
                        return true;
                    } else if (getContentViewOffset() == 1f) {
                        return true;
                    }
                } else if (mEdge == Edge.END
                        && mViewDragHelperEnd.isEdgeTouched(ViewDragHelper.EDGE_RIGHT, pointerId)
                        && getSideMenuOffset() != 1f) {
                    if (getContentViewOffset() == 0) {
                        mViewDragHelper.captureChildView(mSideMenuView, pointerId);
                    }
                    return false;
                }
            }

            if (isSideMenuView(child) && mEdge == Edge.END && !isDrawerMenuOpen()) {
                if (mViewDragHelperEnd.isEdgeTouched(ViewDragHelper.EDGE_RIGHT, pointerId) && getSideMenuOffset() != 1f) {
                    return true;
                } else if (getSideMenuOffset() == 1f) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            closeOtherDrawer();
        }

        private void closeOtherDrawer() {
            final Edge otherEdge = mEdge == Edge.START ? Edge.END : Edge.START;
            closeDrawer(otherEdge);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateDrawerState(mEdge, state);
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset;

            if (mEdge == Edge.START) {
                offset = map(left, 0, DuoDrawerLayout.this.getWidth() * 0.7f, 0, 1);

                float scaleFactorContent = map(offset, 0, 1, 1.f, 0.7f);
                mContentView.setScaleX(scaleFactorContent);
                mContentView.setScaleY(scaleFactorContent);

                float scaleFactorMenu = map(offset, 0, 1, 1.f, 0.7f);
                mDrawerMenuView.setScaleX(scaleFactorMenu);
                mDrawerMenuView.setScaleY(scaleFactorMenu);

                float alphaValue = map(offset, 0, 1, 1.f, 0.7f);
                mDrawerMenuView.setAlpha(alphaValue);
            } else {
                offset = map(DuoDrawerLayout.this.getWidth() - left, 0, changedView.getWidth(), 0, 1);
            }

            setViewOffset(changedView, offset);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final LayoutParams layoutParams = (LayoutParams) releasedChild.getLayoutParams();

            int finalLeft;
            if (mEdge == Edge.START) {
                finalLeft = xvel > 0 || xvel == 0 && layoutParams.getOffset() > 0.5f ? (int) (DuoDrawerLayout.this.getWidth() * 0.7) : 0;
            } else {
                finalLeft = xvel < 0 || xvel == 0 && layoutParams.getOffset() > 0.5f ? DuoDrawerLayout.this.getWidth() - releasedChild.getWidth() : DuoDrawerLayout.this.getWidth();
            }

            mViewDragHelper.settleCapturedViewAt(finalLeft, getTopInset());
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return DuoDrawerLayout.this.getWidth();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int finalLeft;
            if (mEdge == Edge.START) {
                finalLeft = (int) (DuoDrawerLayout.this.getWidth() * 0.7);
                if (left < 0) return 0;
                if (left > finalLeft) return finalLeft;
            } else {
                finalLeft = DuoDrawerLayout.this.getWidth() - child.getWidth();
                if (left > DuoDrawerLayout.this.getWidth())
                    return DuoDrawerLayout.this.getWidth();
                if (left < finalLeft) return finalLeft;
            }
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return getTopInset();
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

    private static class LayoutParams extends MarginLayoutParams {
        private float mOffset;

        LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        LayoutParams(@Px int width, @Px int height) {
            super(width, height);
        }

        LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        float getOffset() {
            return mOffset;
        }

        void setOffset(float pOffset) {
            mOffset = pOffset;
        }
    }

    private class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            final AccessibilityNodeInfoCompat superNode = AccessibilityNodeInfoCompat.obtain(info);
            super.onInitializeAccessibilityNodeInfo(host, superNode);
            info.setSource(host);
            final ViewParent parent = ViewCompat.getParentForAccessibility(host);
            if (parent instanceof View) {
                info.setParent((View) parent);
            }
            copyNodeInfoNoChildren(info, superNode);
            superNode.recycle();
            addChildrenForAccessibility(info, (ViewGroup) host);
        }

        private void addChildrenForAccessibility(AccessibilityNodeInfoCompat info, ViewGroup v) {
            final int childCount = v.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = v.getChildAt(i);
                if (filter(child)) {
                    continue;
                }
                // Adding children that are marked as not important for
                // accessibility will break the hierarchy, so we need to check
                // that value and re-parent views if necessary.
                final int importance = ViewCompat.getImportantForAccessibility(child);
                switch (importance) {
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:
                        // Always skip NO_HIDE views and their descendants.
                        break;
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO:
                        // Re-parent children of NO view groups, skip NO views.
                        if (child instanceof ViewGroup) {
                            addChildrenForAccessibility(info, (ViewGroup) child);
                        }
                        break;
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO:
                        // Force AUTO views to YES and add them.
                        ViewCompat.setImportantForAccessibility(
                                child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES:
                        info.addChild(child);
                        break;
                }
            }
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            return !filter(child) && super.onRequestSendAccessibilityEvent(host, child, event);
        }

        boolean filter(View child) {
            final View openDrawer = findOpenDrawer();
            return openDrawer != null && openDrawer != child;
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                                            AccessibilityNodeInfoCompat src) {
            final Rect rect = mTmpRect;
            src.getBoundsInParent(rect);
            dest.setBoundsInParent(rect);
            src.getBoundsInScreen(rect);
            dest.setBoundsInScreen(rect);
            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());
            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());
            dest.addAction(src.getActions());
        }
    }
}