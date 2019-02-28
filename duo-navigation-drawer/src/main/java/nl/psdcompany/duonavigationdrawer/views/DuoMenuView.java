package nl.psdcompany.duonavigationdrawer.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import nl.psdcompany.psd.duonavigationdrawer.R;

/**
 * Created by PSD on 13-04-17.
 */

public class DuoMenuView extends RelativeLayout {
    private static final String TAG_FOOTER = "footer";
    private static final String TAG_HEADER = "header";

    @DrawableRes
    private static final int DEFAULT_DRAWABLE_ATTRIBUTE_VALUE = 0b11111111111111110010101111001111;
    @LayoutRes
    private static final int DEFAULT_LAYOUT_ATTRIBUTE_VALUE = 0b11111111111111110010101111010000;

    @DrawableRes
    private int mBackgroundDrawableId;
    @LayoutRes
    private int mHeaderViewId;
    @LayoutRes
    private int mFooterViewId;

    private OnMenuClickListener mOnMenuClickListener;
    private DataSetObserver mDataSetObserver;
    private MenuViewHolder mMenuViewHolder;
    private LayoutInflater mLayoutInflater;
    private Adapter mAdapter;

    public DuoMenuView(Context context) {
        this(context, null);
    }

    public DuoMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DuoMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttributes(attrs);
        initialize();
    }

    private void readAttributes(AttributeSet attributeSet) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.DuoMenuView);

        try {
            mBackgroundDrawableId = typedArray.getResourceId(R.styleable.DuoMenuView_background, DEFAULT_DRAWABLE_ATTRIBUTE_VALUE);
            mHeaderViewId = typedArray.getResourceId(R.styleable.DuoMenuView_header, DEFAULT_LAYOUT_ATTRIBUTE_VALUE);
            mFooterViewId = typedArray.getResourceId(R.styleable.DuoMenuView_footer, DEFAULT_LAYOUT_ATTRIBUTE_VALUE);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Initialize the menu view.
     */
    private void initialize() {
        ViewGroup rootView = (ViewGroup) inflate(getContext(), R.layout.duo_view_menu, this);

        mMenuViewHolder = new MenuViewHolder(rootView);
        mLayoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                handleOptions();
                postInvalidate();
                requestLayout();
            }
        };

        handleBackground();
        handleHeader();
        handleFooter();
    }

    /**
     * Handles the background.
     */
    private void handleBackground() {
        if (mMenuViewHolder.mMenuBackground == null) {
            return;
        }

        if (mBackgroundDrawableId != DEFAULT_DRAWABLE_ATTRIBUTE_VALUE) {
            Drawable backgroundDrawable = ContextCompat.getDrawable(getContext(), mBackgroundDrawableId);

            if (backgroundDrawable != null) {
                mMenuViewHolder.mMenuBackground.setImageDrawable(backgroundDrawable);
                return;
            }
        }

        mMenuViewHolder.mMenuBackground.setBackgroundColor(getPrimaryColor());
    }

    /**
     * Handles the header view.
     */
    private void handleHeader() {
        if (mHeaderViewId == DEFAULT_LAYOUT_ATTRIBUTE_VALUE || mMenuViewHolder.mMenuHeader == null) {
            return;
        }

        View view = mLayoutInflater.inflate(mHeaderViewId, null, false);

        if (view != null) {
            if (mMenuViewHolder.mMenuHeader.getChildCount() > 0) {
                mMenuViewHolder.mMenuHeader.removeAllViews();
            }

            mMenuViewHolder.mMenuHeader.addView(view);
            view.setTag(TAG_HEADER);
            view.bringToFront();
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnMenuClickListener != null) {
                        mOnMenuClickListener.onHeaderClicked();
                    }
                }
            });
        }
    }

    /**
     * Handles the footer view.
     */
    private void handleFooter() {
        if (mFooterViewId == DEFAULT_LAYOUT_ATTRIBUTE_VALUE || mMenuViewHolder.mMenuFooter == null) {
            return;
        }

        View view = mLayoutInflater.inflate(mFooterViewId, null, false);

        if (view != null) {
            if (mMenuViewHolder.mMenuFooter.getChildCount() > 0) {
                mMenuViewHolder.mMenuFooter.removeAllViews();
            }

            mMenuViewHolder.mMenuFooter.addView(view);
            view.setTag(TAG_FOOTER);
            view.bringToFront();

            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;

                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    if (viewGroup.getChildAt(i) instanceof Button) {
                        viewGroup.getChildAt(i).setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mOnMenuClickListener != null) {
                                    mOnMenuClickListener.onFooterClicked();
                                }
                            }
                        });
                        return;
                    }
                }
            }
        }
    }

    /**
     * Handles the menu options when adapter is set.
     */
    private void handleOptions() {
        if (mAdapter == null || mAdapter.isEmpty() || mMenuViewHolder.mMenuOptions == null) {
            return;
        }

        if (mMenuViewHolder.mMenuOptions.getChildCount() > 0) {
            mMenuViewHolder.mMenuOptions.removeAllViews();
        }

        for (int i = 0; i < mAdapter.getCount(); i++) {
            final int index = i;

            View optionView = mAdapter.getView(i, null, this);

            if (optionView != null) {
                mMenuViewHolder.mMenuOptions.addView(optionView);
                optionView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnMenuClickListener != null) {
                            mOnMenuClickListener.onOptionClicked(index, mAdapter.getItem(index));
                        }
                    }
                });
            }
        }
    }

    /**
     * Gets the primary color of this project.
     *
     * @return primary color of this project.
     */
    private int getPrimaryColor() {
        TypedArray typedArray = getContext().obtainStyledAttributes(new TypedValue().data, new int[]{R.attr.colorPrimary});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    /**
     * Sets the listener for menu clicks.
     *
     * @param onMenuClickListener Listener that registers menu clicks.
     */
    public void setOnMenuClickListener(OnMenuClickListener onMenuClickListener) {
        mOnMenuClickListener = onMenuClickListener;
    }

    /**
     * Returns the header.
     *
     * @return The current header view.
     */
    public View getHeaderView() {
        return findViewWithTag(TAG_HEADER);
    }

    /**
     * Sets the header view.
     *
     * @param headerViewId View that becomes the header.
     */
    public void setHeaderView(@LayoutRes int headerViewId) {
        mHeaderViewId = headerViewId;
        handleHeader();
    }

    /**
     * Returns the footer.
     *
     * @return The current footer view.
     */
    public View getFooterView() {
        return findViewWithTag(TAG_FOOTER);
    }

    /**
     * Sets the footer view.
     *
     * @param footerViewId View that becomes the footer.
     */
    public void setFooterView(@LayoutRes int footerViewId) {
        mFooterViewId = footerViewId;
        handleFooter();
    }

    /**
     * Sets the background drawable of the MenuView.
     *
     * @param backgroundDrawableId Drawable that becomes the background.
     */
    public void setBackground(@DrawableRes int backgroundDrawableId) {
        mBackgroundDrawableId = backgroundDrawableId;
        handleBackground();
    }

    /**
     * Returns the adapter currently in use.
     *
     * @return The adapter currently used to display data in the Menu.
     */
    public Adapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets the data behind this MenuView.
     *
     * @param adapter The Adapter which is responsible for maintaining the
     *                data backing this list and for producing a view to represent an
     *                item in that data set.
     * @see #getAdapter()
     */
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) mAdapter.unregisterDataSetObserver(mDataSetObserver);
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mDataSetObserver);
        handleOptions();
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
     * Holds the views in this menu
     */
    private class MenuViewHolder {
        private LinearLayout mMenuOptions;
        private ImageView mMenuBackground;
        private ViewGroup mMenuHeader;
        private ViewGroup mMenuFooter;

        MenuViewHolder(ViewGroup rootView) {
            this.mMenuOptions = (LinearLayout) rootView.findViewById(R.id.duo_view_menu_options_layout);
            this.mMenuBackground = (ImageView) rootView.findViewById(R.id.duo_view_menu_background);
            this.mMenuHeader = (ViewGroup) rootView.findViewById(R.id.duo_view_menu_header_layout);
            this.mMenuFooter = (ViewGroup) rootView.findViewById(R.id.duo_view_menu_footer_layout);
        }
    }

    /**
     * Listener that listens to menu click events.
     */
    public interface OnMenuClickListener {
        /**
         * Will be called when user pressed a button in the footer view.
         * Will only be called when footer contains a button.
         */
        void onFooterClicked();

        /**
         * Will be called when user pressed on the header view.
         */
        void onHeaderClicked();

        /**
         * Will be called when user pressed an option view.
         */
        void onOptionClicked(int position, Object objectClicked);
    }
}
