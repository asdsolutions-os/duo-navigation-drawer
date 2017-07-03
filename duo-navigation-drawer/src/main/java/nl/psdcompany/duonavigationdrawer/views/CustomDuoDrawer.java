package nl.psdcompany.duonavigationdrawer.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class CustomDuoDrawer extends ViewGroup {
    private static final String TAG = "DrawerLayout";

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

    private static final String TAG_CONTENT = "content";
    private static final String TAG_SIDE_MENU = "duo_side_menu";

    private static final int MIN_FLING_VELOCITY = 400;

    private final ViewDragHelper mViewDragHelperEnd;
    private final ViewDragCallback mViewDragCallbackEnd;

    private final ViewDragHelper mViewDragHelperStart;
    private final ViewDragCallback mViewDragCallbackStart;

    private View mContentView;
    private View mSideMenuView;

    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };

    public enum Edge {
        START,
        END
    }

    private enum DrawerState {
        OPENED_FROM_START,
        OPENED_FROM_END,
        CLOSED
    }

    private DrawerState mCurrentDrawerState;
    private int mDrawerState;

    public CustomDuoDrawer(Context context) {
        this(context, null);
    }

    public CustomDuoDrawer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomDuoDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;

        mViewDragCallbackStart = new ViewDragCallback(Edge.START);
        mViewDragHelperStart = ViewDragHelper.create(this, 1.f, mViewDragCallbackStart);
        mViewDragHelperStart.setMinVelocity(minVel);
        mViewDragCallbackStart.setViewDragHelper(mViewDragHelperStart);

        mViewDragCallbackEnd = new ViewDragCallback(Edge.END);
        mViewDragHelperEnd = ViewDragHelper.create(this, 1.f, mViewDragCallbackEnd);
        mViewDragHelperEnd.setMinVelocity(minVel);
        mViewDragCallbackEnd.setViewDragHelper(mViewDragHelperEnd);

//        mRightDragger = ViewDragHelper.create(this, 1.f, mRightCallback);
//        mRightDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
//        mRightDragger.setMinVelocity(minVel);

        // So that we can catch the back button
//        setFocusableInTouchMode(true);
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
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
                        "CustomDuoDrawer must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final CustomDuoDrawer.LayoutParams lp = (CustomDuoDrawer.LayoutParams) child.getLayoutParams();

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
        mContentView = findViewWithTag(TAG_CONTENT);
        mSideMenuView = findViewWithTag(TAG_SIDE_MENU);

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
    public void computeScroll() {
        if (mViewDragHelperStart.continueSettling(true) | mViewDragHelperEnd.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelperStart.shouldInterceptTouchEvent(ev) | mViewDragHelperEnd.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mViewDragHelperStart.processTouchEvent(ev);
        mViewDragHelperEnd.processTouchEvent(ev);
        return true;
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

    private boolean isContentView(View pView) {
        return pView != null && pView.getTag() != null && pView.getTag().equals(TAG_CONTENT);
    }

    private boolean isSideMenuView(View pView) {
        return pView != null && pView.getTag() != null && pView.getTag().equals(TAG_SIDE_MENU);
    }

    private float map(float x, float inMin, float inMax, float outMin, float outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    public void closeDrawer(Edge pEdge) {
        if (mContentView == null) {
            mContentView = findViewWithTag(TAG_CONTENT);
        }

        if (pEdge == Edge.START) {
            mViewDragHelperStart.smoothSlideViewTo(mContentView, 0, mContentView.getTop());
        } else if (pEdge == Edge.END) {
            mViewDragHelperEnd.smoothSlideViewTo(mSideMenuView, getWidth(), mContentView.getTop());
        }

        invalidate();
    }

    void updateDrawerState(Edge pEdge, int activeState, float pOffset) {
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

        if (activeState == STATE_IDLE) {
            if (pEdge == Edge.END && pOffset == 1) {
                mCurrentDrawerState = DrawerState.OPENED_FROM_END;
            } else if (pEdge == Edge.START && pOffset == 1) {
                mCurrentDrawerState = DrawerState.OPENED_FROM_START;
            } else if (pOffset == 0) {
                mCurrentDrawerState = DrawerState.CLOSED;
            } else {
                return;
            }
        }

        if (state != mDrawerState) {
            mDrawerState = state;
//            if (mListener != null) {
//                mListener.onDrawerStateChanged(state);
//            }
        }
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private final Edge mEdge;
        private float mViewOffset;
        private ViewDragHelper mViewDragHelper;

        ViewDragCallback(Edge pEdge) {
            mEdge = pEdge;
        }

        void setViewDragHelper(ViewDragHelper pViewDragHelper) {
            mViewDragHelper = pViewDragHelper;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (isContentView(child)) {
                if (mEdge == Edge.START) {
                    if (mViewDragHelperStart.isEdgeTouched(ViewDragHelper.EDGE_LEFT, pointerId) && mViewOffset != 1f) {
                        return true;
                    } else if (mViewOffset == 1f) {
                        return true;
                    }
                } else if (mEdge == Edge.END
                        && mViewDragHelperEnd.isEdgeTouched(ViewDragHelper.EDGE_RIGHT, pointerId)
                        && mViewOffset != 1f) {
                    mViewDragHelper.captureChildView(mSideMenuView, pointerId);
                    return false;
                }
            }

            if (isSideMenuView(child) && mEdge == Edge.END) {
                if (mViewDragHelperEnd.isEdgeTouched(ViewDragHelper.EDGE_RIGHT, pointerId) && mViewOffset != 1f) {
                    return true;
                } else if (mViewOffset == 1f) {
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
            updateDrawerState(mEdge, state, mViewOffset);
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (mEdge == Edge.START) {
                mViewOffset = map(left, 0, CustomDuoDrawer.this.getWidth() * 0.7f, 0, 1);

                float scaleFactorContent = map(mViewOffset, 0, 1, 1.f, 0.7f);
                mContentView.setScaleX(scaleFactorContent);
                mContentView.setScaleY(scaleFactorContent);
            } else {
                mViewOffset = map(CustomDuoDrawer.this.getWidth() - left, 0, changedView.getWidth(), 0, 1);
            }

            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int finalLeft;
            if (mEdge == Edge.START) {
                finalLeft = xvel > 0 || xvel == 0 && mViewOffset > 0.5f ? (int) (CustomDuoDrawer.this.getWidth() * 0.7) : 0;
            } else {
                finalLeft = xvel < 0 || xvel == 0 && mViewOffset > 0.5f ? CustomDuoDrawer.this.getWidth() - releasedChild.getWidth() : CustomDuoDrawer.this.getWidth();
            }

            mViewDragHelper.settleCapturedViewAt(finalLeft, getTopInset());
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return CustomDuoDrawer.this.getWidth();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int finalLeft;
            if (mEdge == Edge.START) {
                finalLeft = (int) (CustomDuoDrawer.this.getWidth() * 0.7);
                if (left < 0) return 0;
                if (left > finalLeft) return finalLeft;
            } else {
                finalLeft = CustomDuoDrawer.this.getWidth() - child.getWidth();
                if (left > CustomDuoDrawer.this.getWidth())
                    return CustomDuoDrawer.this.getWidth();
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

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = Gravity.NO_GRAVITY;
        float onScreen;
        boolean isPeeking;
        boolean knownOpen;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            this(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
}