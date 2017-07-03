package nl.psdcompany.duonavigationdrawer.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.Px;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by PSD on 28-06-17.
 */

public class CustomDrawer extends ViewGroup {
    private static final String TAG = CustomDrawer.class.getSimpleName();
    private static final String TAG_MENU = "menu";
    private static final String TAG_CONTENT = "content";

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
    private static final int PEEK_DELAY = 160; // ms
    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second
    /**
     * Experimental feature.
     */
    private static final boolean ALLOW_EDGE_LOCK = false;

    private static final boolean CHILDREN_DISALLOW_INTERCEPT = true;
    private static final float TOUCH_SLOP_SENSITIVITY = 1.f;
    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
    private static final int MIN_DRAWER_MARGIN = 64; // dp

    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };

    private View mContentView;
    private View mDrawerView;

    private int mMinDrawerMargin;
    private boolean mInLayout;
    private boolean mFirstLayout = true;
    private boolean mDisallowInterceptRequested;
    private boolean mChildrenCanceledTouch;
    private float mInitialMotionX;
    private float mInitialMotionY;

    // My variables
    private ViewDragHelper mViewDragHelperRight;
    private ViewDragHelper mViewDragHelperLeft;
    private ViewDragCallback mViewDragCallbackRight;
    private ViewDragCallback mViewDragCallbackLeft;

    private ContentListener mContentListener;

    public enum ContentState {
        OPENED_LEFT,
        OPENED_RIGHT,
        CLOSED
    }

    private ContentState mCurrentContentState;
    private int mContentState;

    public enum Edge {
        RIGHT,
        LEFT,
    }

    private float mSlideMarginFactor = 0.7f;
    private float mContentViewOffset = 0.f;

    public CustomDrawer(Context context) {
        this(context, null);
    }

    public CustomDrawer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        final float density = getResources().getDisplayMetrics().density;
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * density + 0.5f);
        final float minVel = MIN_FLING_VELOCITY * density;

        mViewDragCallbackLeft = new ViewDragCallback(Edge.LEFT);
        mViewDragHelperLeft = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mViewDragCallbackLeft);
        mViewDragHelperLeft.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
        mViewDragHelperLeft.setMinVelocity(minVel);
        mViewDragCallbackLeft.setViewDragHelper(mViewDragHelperLeft);

        mViewDragCallbackRight = new ViewDragCallback(Edge.RIGHT);
        mViewDragHelperRight = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mViewDragCallbackRight);
        mViewDragHelperRight.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
        mViewDragHelperRight.setMinVelocity(minVel);
        mViewDragCallbackRight.setViewDragHelper(mViewDragHelperRight);

        setFocusableInTouchMode(true);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
    }

    private float map(float x, float inMin, float inMax, float outMin, float outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContentView = findViewWithTag("content");
        mDrawerView = findViewWithTag("menu");
        bringChildToFront(mContentView);
        LayoutParams layoutParams = (LayoutParams) mContentView.getLayoutParams();
    }

    /**
     * Resolve the shared state of the content from the component ViewDragHelpers.
     * Should be called whenever a ViewDragHelper's state changes.
     */
    void updateContentState(Edge pEdge, int activeState, ViewDragHelper pViewDragHelper) {
        final int viewDragState = pViewDragHelper.getViewDragState();

        final int state;
        if (viewDragState == STATE_DRAGGING) {
            state = STATE_DRAGGING;
        } else if (viewDragState == STATE_SETTLING) {
            state = STATE_SETTLING;
        } else {
            state = STATE_IDLE;
        }

        if (mContentView != null && activeState == STATE_IDLE) {
            if (pEdge == Edge.LEFT) {
                dispatchOnContentOpened(ContentState.OPENED_RIGHT);
            } else if (pEdge == Edge.RIGHT) {
                dispatchOnContentOpened(ContentState.OPENED_LEFT);
            }
        }

        if (state != mContentState) {
            mContentState = state;
            if (mContentListener != null) {
                mContentListener.onContentStateChanged(state);
            }
        }
    }

    void dispatchOnContentClosed() {
        mCurrentContentState = ContentState.CLOSED;

        if (mContentListener != null) {
            mContentListener.onContentClosed();
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnContentOpened(ContentState pContentState) {
        mCurrentContentState = pContentState;

        if (mContentListener != null) {
            mContentListener.onContentOpened(pContentState);
        }
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnContentSlide(float slideOffset) {
        if (mContentListener != null) {
            mContentListener.onContentSlide(slideOffset);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
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
                        "DrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (isContentView(child)) {
                final int contentWidthSpec =
                        MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec =
                        MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            } else if (isDrawerView(child)) {
                final int drawerWidthSpec =
                        MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int drawerHeightSpec =
                        MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(drawerWidthSpec, drawerHeightSpec);
            } else {
                Log.e(TAG, "Child at index " + i + "is not content and not drawer");
//                throw new IllegalStateException("Child " + child + " at index " + i +
//                        " does not have a valid layout_gravity - must be Gravity.LEFT, " +
//                        "Gravity.RIGHT or Gravity.NO_GRAVITY");
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        final int width = r - l;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            child.layout(lp.leftMargin, lp.topMargin,
                    lp.leftMargin + child.getMeasuredWidth(),
                    lp.topMargin + child.getMeasuredHeight());

//            if (isDrawerView(child)) {
//                child.layout(lp.leftMargin, lp.topMargin,
//                        lp.leftMargin + child.getMeasuredWidth(),
//                        lp.topMargin + child.getMeasuredHeight());
//            }
//            else {
//                final int childWidth = child.getMeasuredWidth();
//                final int childHeight = child.getMeasuredHeight();
//
//                int childLeft;
//                final float newOffset;
//                if (checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
//                    childLeft = -childWidth + (int) (childWidth * lp.onScreen);
//                    newOffset = (float) (childWidth + childLeft) / childWidth;
//                } else { // Right; onMeasure checked for us.
//                    childLeft = width - (int) (childWidth * lp.onScreen);
//                    newOffset = (float) (width - childLeft) / childWidth;
//                }
//                final boolean changeOffset = newOffset != lp.onScreen;
//                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
//                switch (vgrav) {
//                    default:
//                    case Gravity.TOP: {
//                        child.layout(childLeft, lp.topMargin, childLeft + childWidth,
//                                lp.topMargin + childHeight);
//                        break;
//                    }
//                    case Gravity.BOTTOM: {
//                        final int height = b - t;
//                        child.layout(childLeft,
//                                height - lp.bottomMargin - child.getMeasuredHeight(),
//                                childLeft + childWidth,
//                                height - lp.bottomMargin);
//                        break;
//                    }
//                    case Gravity.CENTER_VERTICAL: {
//                        final int height = b - t;
//                        int childTop = (height - childHeight) / 2;
//                        // Offset for margins. If things don't fit right because of
//                        // bad measurement before, oh well.
//                        if (childTop < lp.topMargin) {
//                            childTop = lp.topMargin;
//                        } else if (childTop + childHeight > height - lp.bottomMargin) {
//                            childTop = height - lp.bottomMargin - childHeight;
//                        }
//                        child.layout(childLeft, childTop, childLeft + childWidth,
//                                childTop + childHeight);
//                        break;
//                    }
//                }
//                if (changeOffset) {
//                    setDrawerViewOffset(child, newOffset);
//                }
//                final int newVisibility = lp.onScreen > 0 ? VISIBLE : INVISIBLE;
//                if (child.getVisibility() != newVisibility) {
//                    child.setVisibility(newVisibility);
//                }
//            }
        }
        mInLayout = false;
        mFirstLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelperLeft.continueSettling(true) | mViewDragHelperRight.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    boolean isContentView(View child) {
        return child.getTag().equals(TAG_CONTENT);
    }

    boolean isDrawerView(View child) {
        return child.getTag().equals(TAG_MENU);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        final boolean interceptForDrag = mViewDragHelperLeft.shouldInterceptTouchEvent(ev)
                | mViewDragHelperRight.shouldInterceptTouchEvent(ev);

//        boolean interceptForTap = false;

        switch (action) {
//            case MotionEvent.ACTION_DOWN: {
//                final float x = ev.getX();
//                final float y = ev.getY();
//                mInitialMotionX = x;
//                mInitialMotionY = y;
//                if (isContentView(mViewDragHelperLeft.findTopChildUnder((int) x, (int) y))) {
//                    interceptForTap = true;
//                }
//                mDisallowInterceptRequested = false;
//                mChildrenCanceledTouch = false;
//                break;
//            }
//            case MotionEvent.ACTION_MOVE: {
//                // If we cross the touch slop, don't perform the delayed peek for an edge touch.
//                if (mViewDragHelperLeft.checkTouchSlop(ViewDragHelper.DIRECTION_ALL)) {
//                    mViewDragCallbackLeft.removeCallbacks();
//                }
//                break;
//            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                closeDrawer();
//                mDisallowInterceptRequested = false;
//                mChildrenCanceledTouch = false;
            }
        }
        return interceptForDrag;// || interceptForTap || mChildrenCanceledTouch;// || hasPeekingDrawer() || mChildrenCanceledTouch;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mViewDragHelperLeft.processTouchEvent(ev);
        mViewDragHelperRight.processTouchEvent(ev);

//        final int action = ev.getAction();
//
//        switch (action & MotionEventCompat.ACTION_MASK) {
//            case MotionEvent.ACTION_DOWN: {
//                final float x = ev.getX();
//                final float y = ev.getY();
//                mInitialMotionX = x;
//                mInitialMotionY = y;
//                mDisallowInterceptRequested = false;
//                mChildrenCanceledTouch = false;
//                break;
//            }
//            case MotionEvent.ACTION_UP: {
//                final float x = ev.getX();
//                final float y = ev.getY();
//                boolean peekingOnly = true;
//                final View touchedView = mViewDragHelperLeft.findTopChildUnder((int) x, (int) y);
//                if (touchedView != null && isContentView(touchedView)) {
//                    final float dx = x - mInitialMotionX;
//                    final float dy = y - mInitialMotionY;
//                    final int slop = mViewDragHelperLeft.getTouchSlop();
//                    if (dx * dx + dy * dy < slop * slop) {
//                        // Taps close a dimmed open drawer but only if it isn't locked open.
//                        peekingOnly = false;
//                    }
//                }
//                closeDrawer();
//                mDisallowInterceptRequested = false;
//                break;
//            }
//            case MotionEvent.ACTION_CANCEL: {
//                closeDrawer();
//                mDisallowInterceptRequested = false;
//                mChildrenCanceledTouch = false;
//                break;
//            }
//        }
        return true;
    }

//    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
//        if (CHILDREN_DISALLOW_INTERCEPT
//                || (!mViewDragHelperLeft.isEdgeTouched(ViewDragHelper.EDGE_LEFT)
//                && !mViewDragHelperRight.isEdgeTouched(ViewDragHelper.EDGE_RIGHT))) {
//            // If we have an edge touch we want to skip this and track it for later instead.
//            super.requestDisallowInterceptTouchEvent(disallowIntercept);
//        }
//        mDisallowInterceptRequested = disallowIntercept;
//        if (disallowIntercept) {
//            closeDrawer();
//        }
//    }

    /**
     * Open the drawer by animating the content view out of the screen.
     *
     * @param pEdge The edge the content has to start animating from.
     */
    public void openDrawer(Edge pEdge) {
        if (mContentView == null) {
            mContentView = findViewWithTag(TAG_CONTENT);
        }

        if (pEdge.equals(Edge.LEFT)) {
            // Left edge so we animate the content to the right
            mViewDragHelperLeft.smoothSlideViewTo(mContentView, (int) (getWidth() * mSlideMarginFactor), mContentView.getTop());
        } else {
            // Right edge so we animate the content to the left
            mViewDragHelperRight.smoothSlideViewTo(mContentView, (int) (getWidth() - (getWidth() * mSlideMarginFactor)), mContentView.getTop());
        }
        invalidate();
    }

    /**
     * Close the drawer view by animating it back into the view.
     */
    public void closeDrawer() {
        if (mContentView == null) {
            mContentView = findViewWithTag(TAG_CONTENT);
        }

        if (mCurrentContentState == ContentState.OPENED_LEFT) {
            // Content is at the left of the screen so animate the content back to the middle
            mViewDragHelperLeft.smoothSlideViewTo(mContentView, CustomDrawer.this.getWidth(), mContentView.getTop());
        } else if (mCurrentContentState == ContentState.OPENED_RIGHT) {
            // Content is at the right of the screen so animate the content back to the middle
            mViewDragHelperRight.smoothSlideViewTo(mContentView, 0, mContentView.getTop());
        }
        invalidate();
    }

    /**
     * Check if the content view is currently in an open state.
     * To be considered "open" the drawer must have settled into its fully
     * visible state.
     *
     * @return true if the given drawer view is in an open state
     */
    public boolean isDrawerOpen() {
        return mCurrentContentState != ContentState.CLOSED;
    }

//    private boolean hasPeekingDrawer() {
//        final int childCount = getChildCount();
//        for (int i = 0; i < childCount; i++) {
//            final LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
//            if (lp.isPeeking) {
//                return true;
//            }
//        }
//        return false;
//    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof ViewGroup.MarginLayoutParams
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

    void cancelChildViewTouch() {
        // Cancel child touches
        if (!mChildrenCanceledTouch) {
            final long now = SystemClock.uptimeMillis();
            final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).dispatchTouchEvent(cancelEvent);
            }
            cancelEvent.recycle();
            mChildrenCanceledTouch = true;
        }
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

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private Edge mEdge = null;
        private ViewDragHelper mViewDragHelper;

        private final Runnable mPeekRunnable = new Runnable() {
            @Override
            public void run() {
                peekDrawer();
            }
        };

        public ViewDragCallback(Edge pEdge) {
            mEdge = pEdge;
        }

        public void setViewDragHelper(ViewDragHelper pViewDragHelper) {
            mViewDragHelper = pViewDragHelper;
        }

        public void removeCallbacks() {
            CustomDrawer.this.removeCallbacks(mPeekRunnable);
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return isContentView(child)
                    && mViewDragHelper.isEdgeTouched(ViewDragHelper.EDGE_LEFT)
                    || mViewDragHelper.isEdgeTouched(ViewDragHelper.EDGE_RIGHT);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateContentState(mEdge, state, mViewDragHelper);
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mContentViewOffset = map(left, 0, CustomDrawer.this.getWidth() * mSlideMarginFactor, 0, 1);

//            Log.e("JOE", "mContentViewOffset: " + mContentViewOffset);

            float scaleFactorContent = map(mContentViewOffset, 0, 1, 1f, 0.7f);
            mContentView.setScaleX(scaleFactorContent);
            mContentView.setScaleY(scaleFactorContent);

            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
//            final LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
//            lp.isPeeking = false;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int settleAt = 0;
            if (mContentViewOffset > 0.5f) {
                settleAt = (int) (getWidth() * mSlideMarginFactor);
            }
            mViewDragHelper.settleCapturedViewAt(settleAt, releasedChild.getTop());
            invalidate();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            Log.d("JOE", "onEdgeTouched");
//            postDelayed(mPeekRunnable, PEEK_DELAY);
        }

        private void peekDrawer() {
//            final View toCapture;
//            final int childLeft;
//            final int peekDistance = mViewDragHelper.getEdgeSize();
//            final boolean leftEdge = mAbsGravity == Gravity.LEFT;
//            if (leftEdge) {
//                toCapture = findDrawerWithGravity(Gravity.LEFT);
//                childLeft = (toCapture != null ? -toCapture.getWidth() : 0) + peekDistance;
//            } else {
//                toCapture = findDrawerWithGravity(Gravity.RIGHT);
//                childLeft = getWidth() - peekDistance;
//            }
//            // Only peek if it would mean making the drawer more visible and the drawer isn't locked
//            if (toCapture != null && ((leftEdge && toCapture.getLeft() < childLeft) ||
//                    (!leftEdge && toCapture.getLeft() > childLeft)) &&
//                    getDrawerLockMode(toCapture) == LOCK_MODE_UNLOCKED) {
//                final LayoutParams lp = (LayoutParams) toCapture.getLayoutParams();
//                mViewDragHelper.smoothSlideViewTo(toCapture, childLeft, toCapture.getTop());
//                lp.isPeeking = true;
//                invalidate();
//                closeOtherDrawer();
//                cancelChildViewTouch();
//            }
        }

//        @Override
//        public boolean onEdgeLock(int edgeFlags) {
//            if (ALLOW_EDGE_LOCK) {
//                final View drawer = findDrawerWithGravity(mAbsGravity);
//                if (drawer != null && !isDrawerOpen(drawer)) {
////                    closeDrawer(drawer);
//                }
//                return true;
//            }
//            return false;
//        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            super.onEdgeDragStarted(edgeFlags, pointerId);
            Log.e("JOE", "mEdge: " + mEdge);
            Log.e("JOE", "edgeFlags: " + edgeFlags + " | RIGHT: " + ViewDragHelper.EDGE_RIGHT + " | LEFT: " + ViewDragHelper.EDGE_LEFT);
            Log.e("JOE", "===========================");
            mViewDragHelper.captureChildView(mContentView, pointerId);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return CustomDrawer.this.getWidth();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (left < 0) return 0;
            int width = (int) (getWidth() * mSlideMarginFactor);
            if (left > width) return width;
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
//
//    @Override
//    protected void onRestoreInstanceState(Parcelable state) {
//        final SavedState ss = (SavedState) state;
//        super.onRestoreInstanceState(ss.getSuperState());
//        if (ss.openDrawerGravity != Gravity.NO_GRAVITY) {
//            final View toOpen = findDrawerWithGravity(ss.openDrawerGravity);
//            if (toOpen != null) {
////                openDrawer(toOpen);
//            }
//        }
//        setDrawerLockMode(ss.lockModeLeft, Gravity.LEFT);
//        setDrawerLockMode(ss.lockModeRight, Gravity.RIGHT);
//    }
//
//    @Override
//    protected Parcelable onSaveInstanceState() {
//        final Parcelable superState = super.onSaveInstanceState();
//        final SavedState ss = new SavedState(superState);
//        final int childCount = getChildCount();
//        for (int i = 0; i < childCount; i++) {
//            final View child = getChildAt(i);
//            if (!isDrawerView(child)) {
//                continue;
//            }
//            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
//            if (lp.knownOpen) {
//                ss.openDrawerGravity = lp.gravity;
//                // Only one drawer can be open at a time.
//                break;
//            }
//        }
//        ss.lockModeLeft = mLockModeLeft;
//        ss.lockModeRight = mLockModeRight;
//        return ss;
//    }

    /**
     * State persisted across instances
     */
    protected static class SavedState extends BaseSavedState {
        int openDrawerGravity = Gravity.NO_GRAVITY;
        int lockModeLeft = LOCK_MODE_UNLOCKED;
        int lockModeRight = LOCK_MODE_UNLOCKED;

        public SavedState(Parcel in) {
            super(in);
            openDrawerGravity = in.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(openDrawerGravity);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(@Px int width, @Px int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
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
                final int importance = ViewCompat.getImportantForAccessibility(child);
                switch (importance) {
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:
                        break;
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO:
                        if (child instanceof ViewGroup) {
                            addChildrenForAccessibility(info, (ViewGroup) child);
                        }
                        break;
                    case ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO:
                        ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
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

        public boolean filter(View child) {
            return mContentView != null && mContentView != child;
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest, AccessibilityNodeInfoCompat src) {
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

    /**
     * Stub/no-op implementations of all methods of {@link ContentListener}.
     * Override this if you only care about a few of the available callback methods.
     */
    public static abstract class SimpleContentListener implements ContentListener {
        @Override
        public void onContentSlide(float pSlideOffset) {

        }

        @Override
        public void onContentOpened(ContentState pContentState) {

        }

        @Override
        public void onContentClosed() {

        }

        @Override
        public void onContentStateChanged(int pNewState) {

        }
    }

    /**
     * Listener for monitoring events about the content.
     */
    public interface ContentListener {
        /**
         * Called when the content's position changes.
         *
         * @param pSlideOffset The new offset of the content within its range, from -1 to 1.
         */
        void onContentSlide(float pSlideOffset);

        /**
         * Called when the content has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param pContentState The state the content view is in.
         *                      Either left or right.
         */
        void onContentOpened(ContentState pContentState);

        /**
         * Called when the content has settled in a completely closed state.
         */
        void onContentClosed();

        /**
         * Called when the content motion state changes. The new state will
         * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         *
         * @param pNewState The new drawer motion state.
         */
        void onContentStateChanged(int pNewState);
    }
}
