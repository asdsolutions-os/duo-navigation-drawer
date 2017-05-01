package nl.psdcompany.duonavigationdrawer.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import nl.psdcompany.psd.duonavigationdrawer.R;

/**
 * Created by PSD on 13-04-17.
 */

public class DuoOptionView extends RelativeLayout {
    private OptionViewHolder mOptionViewHolder;

    private static final float ALPHA_CHECKED = 1f;
    private static final float ALPHA_UNCHECKED = 0.5f;

    public DuoOptionView(Context context) {
        this(context, null);
    }

    public DuoOptionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DuoOptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        ViewGroup rootView = (ViewGroup) inflate(getContext(), R.layout.view_option, this);

        mOptionViewHolder = new OptionViewHolder(rootView);
    }

    public void setSelected(boolean selected) {
        if (selected) {
            mOptionViewHolder.mTextViewOption.setAlpha(ALPHA_CHECKED);
            if (mOptionViewHolder.mImageViewSelector.getVisibility() == VISIBLE) {
                mOptionViewHolder.mImageViewSelector.setAlpha(ALPHA_CHECKED);
            }
        } else {
            mOptionViewHolder.mTextViewOption.setAlpha(ALPHA_UNCHECKED);
            if (mOptionViewHolder.mImageViewSelector.getVisibility() == VISIBLE) {
                mOptionViewHolder.mImageViewSelector.setAlpha(ALPHA_UNCHECKED);
            }
        }
    }

    public boolean isSelected() {
        return mOptionViewHolder.mTextViewOption.getAlpha() == ALPHA_CHECKED
                && mOptionViewHolder.mImageViewSelector.getAlpha() == ALPHA_CHECKED
                || mOptionViewHolder.mTextViewOption.getAlpha() == ALPHA_CHECKED;
    }

    public void bind(String optionText) {
        mOptionViewHolder.mTextViewOption.setText(optionText);
        mOptionViewHolder.mTextViewOption.setAlpha(ALPHA_UNCHECKED);
        mOptionViewHolder.mImageViewSelector.setVisibility(GONE);
    }

    public void bind(String optionText, Drawable selectorDrawable) {
        mOptionViewHolder.mTextViewOption.setText(optionText);
        mOptionViewHolder.mTextViewOption.setAlpha(ALPHA_UNCHECKED);
        mOptionViewHolder.mImageViewSelector.setImageDrawable(selectorDrawable);
        mOptionViewHolder.mImageViewSelector.setAlpha(ALPHA_UNCHECKED);
    }

    private class OptionViewHolder {
        private TextView mTextViewOption;
        private ImageView mImageViewSelector;

        OptionViewHolder(ViewGroup rootView) {
            mTextViewOption = (TextView) rootView.findViewById(R.id.view_option_text);
            mImageViewSelector = (ImageView) rootView.findViewById(R.id.view_option_selector);
        }
    }
}
