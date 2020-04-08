package com.litao.android.lib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;

/**
 * Created by litao on 2020/3/26.
 */
public class TabItem extends View {
    public final CharSequence text;
    public final Drawable icon;
    public final int customLayout;

    private TypedArray mWrapped;

    public TabItem(Context context) {
        this(context, null);
    }

    public TabItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mWrapped =  context.obtainStyledAttributes(attrs, R.styleable.LTTabItem);
        text = mWrapped.getText(R.styleable.LTTabItem_android_text);
        icon = getDrawable(context,R.styleable.LTTabItem_android_icon);
        customLayout = mWrapped.getResourceId(R.styleable.LTTabItem_android_layout, 0);
        mWrapped.recycle();
    }

    public Drawable getDrawable(Context context, int index) {
        if (mWrapped.hasValue(index)) {
            final int resourceId = mWrapped.getResourceId(index, 0);
            if (resourceId != 0) {
                return AppCompatResources.getDrawable(context, resourceId);
            }
        }
        return mWrapped.getDrawable(index);
    }
}
