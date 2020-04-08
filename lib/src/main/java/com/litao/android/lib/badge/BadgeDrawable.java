package com.litao.android.lib.badge;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.litao.android.lib.R;
import com.litao.android.lib.TabUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Created by litao on 2020/3/26.
 */
public class BadgeDrawable extends Drawable {

    /** Position the badge can be set to. */
    @IntDef({
            TOP_END,
            TOP_START,
            BOTTOM_END,
            BOTTOM_START,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BadgeGravity {}

    /** The badge is positioned along the top and end edges of its anchor view */
    public static final int TOP_END = Gravity.TOP | Gravity.END;

    /** The badge is positioned along the top and start edges of its anchor view */
    public static final int TOP_START = Gravity.TOP | Gravity.START;

    /** The badge is positioned along the bottom and end edges of its anchor view */
    public static final int BOTTOM_END = Gravity.BOTTOM | Gravity.END;

    /** The badge is positioned along the bottom and start edges of its anchor view */
    public static final int BOTTOM_START = Gravity.BOTTOM | Gravity.START;

    /**
     * Maximum number of characters a badge supports displaying by default. It could be changed using
     * BadgeDrawable#setMaxBadgeCount.
     */
    private static final int DEFAULT_MAX_BADGE_CHARACTER_COUNT = 4;

    /** Value of -1 denotes a numberless badge. */
    private static final int BADGE_NUMBER_NONE = -1;

    /** Maximum value of number that can be displayed in a circular badge. */
    private static final int MAX_CIRCULAR_BADGE_NUMBER_COUNT = 9;

    @StyleRes private static final int DEFAULT_STYLE = R.style.LTWidget_TabLayout_Tab_Badge;
    @AttrRes private static final int DEFAULT_THEME_ATTR = R.attr.ltBadgeStyle;

    /**
     * If the badge number exceeds the maximum allowed number, append this suffix to the max badge
     * number and display is as the badge text instead.
     */
    static final String DEFAULT_EXCEED_MAX_BADGE_NUMBER_SUFFIX = "+";

    @NonNull private final WeakReference<Context> contextRef;
    @NonNull private final ShapeDrawable shapeDrawable;
    @NonNull private final Rect badgeBounds;
    private final float badgeRadius;
    private final float badgeWithTextRadius;
    private final float badgeWidePadding;
    @NonNull private final SavedState savedState;

    private float badgeCenterX;
    private float badgeCenterY;
    private int maxBadgeNumber;
    private float cornerRadius;
    private float halfBadgeWidth;
    private float halfBadgeHeight;

    // Need to keep a local reference in order to support updating badge gravity.
    @Nullable private WeakReference<View> anchorViewRef;
    @Nullable private WeakReference<ViewGroup> customBadgeParentRef;


    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);


    private float textWidth;
    private boolean textWidthDirty = true;


    /**
     * A {@link Parcelable} implementation used to ensure the state of BadgeDrawable is saved.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final class SavedState implements Parcelable {

        @ColorInt private int backgroundColor;
        @ColorInt private int badgeTextColor;
        private int alpha = 255;
        private int number = BADGE_NUMBER_NONE;
        private int maxCharacterCount;
        @BadgeDrawable.BadgeGravity
        private int badgeGravity;

        public SavedState(@NonNull Context context) {
            badgeTextColor = Color.WHITE;
        }

        protected SavedState(@NonNull Parcel in) {
            backgroundColor = in.readInt();
            badgeTextColor = in.readInt();
            alpha = in.readInt();
            number = in.readInt();
            maxCharacterCount = in.readInt();
            badgeGravity = in.readInt();
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @NonNull
                    @Override
                    public SavedState createFromParcel(@NonNull Parcel in) {
                        return new SavedState(in);
                    }

                    @NonNull
                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(backgroundColor);
            dest.writeInt(badgeTextColor);
            dest.writeInt(alpha);
            dest.writeInt(number);
            dest.writeInt(maxCharacterCount);
            dest.writeInt(badgeGravity);
        }
    }

    @NonNull
    public SavedState getSavedState() {
        return savedState;
    }

    /** Creates an instance of BadgeDrawable with the provided {@link com.google.android.material.badge.BadgeDrawable.SavedState}. */
    @NonNull
    static BadgeDrawable createFromSavedState(
            @NonNull Context context, @NonNull SavedState savedState) {
        BadgeDrawable badge = new BadgeDrawable(context);
        badge.restoreFromSavedState(savedState);
        return badge;
    }

    /** Creates an instance of BadgeDrawable with default values. */
    @NonNull
    public static BadgeDrawable create(@NonNull Context context) {
        return createFromAttributes(context, /* attrs= */ null, DEFAULT_THEME_ATTR, DEFAULT_STYLE);
    }


    /** Returns a BadgeDrawable from the given attributes. */
    @NonNull
    private static BadgeDrawable createFromAttributes(
            @NonNull Context context,
            AttributeSet attrs,
            @AttrRes int defStyleAttr,
            @StyleRes int defStyleRes) {
        BadgeDrawable badge = new BadgeDrawable(context);
        badge.loadDefaultStateFromAttributes(context, attrs, defStyleAttr, defStyleRes);
        return badge;
    }

    /**
     * Convenience wrapper method for {@link Drawable#setVisible(boolean, boolean)} with the {@code
     * restart} parameter hardcoded to false.
     */
    public void setVisible(boolean visible) {
        setVisible(visible, /* restart= */ false);
    }

    private void restoreFromSavedState(@NonNull SavedState savedState) {
        setMaxCharacterCount(savedState.maxCharacterCount);

        // Only set the badge number if it exists in the style.
        // Defaulting it to 0 means the badge will incorrectly show text when the user may want a
        // numberless badge.
        if (savedState.number != BADGE_NUMBER_NONE) {
            setNumber(savedState.number);
        }

        setBackgroundColor(savedState.backgroundColor);

        setBadgeTextColor(savedState.badgeTextColor);

        setBadgeGravity(savedState.badgeGravity);
    }

    private void loadDefaultStateFromAttributes(
            Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LTBadge, defStyleAttr, defStyleRes);

        setMaxCharacterCount(
                a.getInt(R.styleable.LTBadge_ltMaxCharacterCount, DEFAULT_MAX_BADGE_CHARACTER_COUNT));

        // Only set the badge number if it exists in the style.
        // Defaulting it to 0 means the badge will incorrectly show text when the user may want a
        // numberless badge.
        if (a.hasValue(R.styleable.LTBadge_ltNumber)) {
            setNumber(a.getInt(R.styleable.LTBadge_ltNumber, 0));
        }


        setBackgroundColor(a.getColor(R.styleable.LTBadge_ltBackgroundColor, Color.RED));

        if (a.hasValue(R.styleable.LTBadge_ltBadgeTextColor)) {
            setBadgeTextColor(a.getColor(R.styleable.LTBadge_ltBadgeTextColor, Color.WHITE));
        }

        setBadgeGravity(a.getInt(R.styleable.LTBadge_ltBadgeGravity, TOP_END));
        a.recycle();
    }

    private static int readColorFromAttributes(
            Context context, @NonNull TypedArray a, @StyleableRes int index) {
        return TabUtils.getColorStateList(context, a, index).getDefaultColor();
    }

    private BadgeDrawable(@NonNull Context context) {
        this.contextRef = new WeakReference<>(context);
        Resources res = context.getResources();
        badgeBounds = new Rect();


        badgeRadius = res.getDimensionPixelSize(R.dimen.tab_badge_radius);
        badgeWidePadding = res.getDimensionPixelSize(R.dimen.tab_badge_text_padding);
        badgeWithTextRadius = res.getDimensionPixelSize(R.dimen.tab_badge_with_text_radius);

        float[] externalRound = {badgeWithTextRadius, badgeWithTextRadius, badgeWithTextRadius, badgeWithTextRadius, badgeWithTextRadius, badgeWithTextRadius, badgeWithTextRadius, badgeWithTextRadius};
        shapeDrawable = new ShapeDrawable(new RoundRectShape(externalRound, null, null));

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(res.getDimension(R.dimen.tab_badge_text_size));
        this.savedState = new SavedState(context);
    }

    /**
     * Calculates and updates this badge's center coordinates based on its anchor's bounds. Internally
     * also updates this BadgeDrawable's bounds, because they are dependent on the center coordinates.
     * For pre API-18, coordinates will be calculated relative to {@code customBadgeParent} because
     * the BadgeDrawable will be set as the parent's foreground.
     *
     * @param anchorView This badge's anchor.
     * @param customBadgeParent An optional parent view that will set this BadgeDrawable as its
     *     foreground.
     */
    public void updateBadgeCoordinates(
            @NonNull View anchorView, @Nullable ViewGroup customBadgeParent) {
        this.anchorViewRef = new WeakReference<>(anchorView);
        this.customBadgeParentRef = new WeakReference<>(customBadgeParent);
        updateCenterAndBounds();
        invalidateSelf();
    }

    /**
     * Returns this badge's background color.
     *
     * @see #setBackgroundColor(int)
     * @attr ref com.google.android.material.R.styleable#Badge_backgroundColor
     */
    @ColorInt
    public int getBackgroundColor() {
        return shapeDrawable.getPaint().getColor();
    }

    /**
     * Sets this badge's background color.
     *
     * @param backgroundColor This badge's background color.
     * @attr ref com.google.android.material.R.styleable#Badge_backgroundColor
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        savedState.backgroundColor = backgroundColor;
//        ColorStateList backgroundColorStateList = ColorStateList.valueOf(backgroundColor);
//        if (shapeDrawable.getPaint().getColor() != backgroundColorStateList) {
//            shapeDrawable.setFillColor(backgroundColorStateList);
            shapeDrawable.getPaint().setColor(backgroundColor);
            invalidateSelf();
//        }
    }

    /**
     * Returns this badge's text color.
     *
     * @see #setBadgeTextColor(int)
     * @attr ref com.google.android.material.R.styleable#Badge_badgeTextColor
     */
    @ColorInt
    public int getBadgeTextColor() {
        return textPaint.getColor();
    }

    /**
     * Sets this badge's text color.
     *
     * @param badgeTextColor This badge's text color.
     * @attr ref com.google.android.material.R.styleable#Badge_badgeTextColor
     */
    public void setBadgeTextColor(@ColorInt int badgeTextColor) {
        savedState.badgeTextColor = badgeTextColor;
        if (textPaint.getColor() != badgeTextColor) {
            textPaint.setColor(badgeTextColor);
            invalidateSelf();
        }
    }

    /** Returns whether this badge will display a number. */
    public boolean hasNumber() {
        return savedState.number != BADGE_NUMBER_NONE;
    }

    /**
     * Returns this badge's number. Only non-negative integer numbers will be returned because the
     * setter clamps negative values to 0.
     *
     * <p>WARNING: Do not call this method if you are planning to compare to BADGE_NUMBER_NONE
     *
     * @see #setNumber(int)
     * @attr ref com.google.android.material.R.styleable#Badge_number
     */
    public int getNumber() {
        if (!hasNumber()) {
            return 0;
        }
        return savedState.number;
    }

    /**
     * Sets this badge's number. Only non-negative integer numbers are supported. If the number is
     * negative, it will be clamped to 0. The specified value will be displayed, unless its number of
     * digits exceeds {@code maxCharacterCount} in which case a truncated version will be shown.
     *
     * @param number This badge's number.
     * @attr ref com.google.android.material.R.styleable#Badge_number
     */
    public void setNumber(int number) {
        number = Math.max(0, number);
        if (this.savedState.number != number) {
            this.savedState.number = number;
            textWidthDirty = true;
            updateCenterAndBounds();
            invalidateSelf();
        }
    }

    /** Resets any badge number so that a numberless badge will be displayed. */
    public void clearNumber() {
        savedState.number = BADGE_NUMBER_NONE;
        invalidateSelf();
    }

    /**
     * Returns this badge's max character count.
     *
     * @see #setMaxCharacterCount(int)
     * @attr ref com.google.android.material.R.styleable#Badge_maxCharacterCount
     */
    public int getMaxCharacterCount() {
        return savedState.maxCharacterCount;
    }

    /**
     * Sets this badge's max character count.
     *
     * @param maxCharacterCount This badge's max character count.
     * @attr ref com.google.android.material.R.styleable#Badge_maxCharacterCount
     */
    public void setMaxCharacterCount(int maxCharacterCount) {
        if (this.savedState.maxCharacterCount != maxCharacterCount) {
            this.savedState.maxCharacterCount = maxCharacterCount;
            updateMaxBadgeNumber();
            textWidthDirty = true;
            updateCenterAndBounds();
            invalidateSelf();
        }
    }

    @BadgeDrawable.BadgeGravity
    public int getBadgeGravity() {
        return savedState.badgeGravity;
    }

    /**
     * Sets this badge's gravity with respect to its anchor view.
     *
     * @param gravity Constant representing one of 4 possible {@link com.google.android.material.badge.BadgeDrawable.BadgeGravity} values.
     */
    public void setBadgeGravity(@BadgeDrawable.BadgeGravity int gravity) {
        if (savedState.badgeGravity != gravity) {
            savedState.badgeGravity = gravity;
            if (anchorViewRef != null && anchorViewRef.get() != null) {
                updateBadgeCoordinates(
                        anchorViewRef.get(), customBadgeParentRef != null ? customBadgeParentRef.get() : null);
            }
        }
    }

    @Override
    public boolean isStateful() {
        return false;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Intentionally empty.
    }

    @Override
    public int getAlpha() {
        return savedState.alpha;
    }

    @Override
    public void setAlpha(int alpha) {
        this.savedState.alpha = alpha;
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /** Returns the height at which the badge would like to be laid out. */
    @Override
    public int getIntrinsicHeight() {
        return badgeBounds.height();
    }

    /** Returns the width at which the badge would like to be laid out. */
    @Override
    public int getIntrinsicWidth() {
        return badgeBounds.width();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || getAlpha() == 0 || !isVisible()) {
            return;
        }
        shapeDrawable.draw(canvas);
        if (hasNumber()) {
            drawText(canvas);
        }
    }


    @Override
    public boolean onStateChange(int[] state) {
        return super.onStateChange(state);
    }


    private void updateCenterAndBounds() {
        Context context = contextRef.get();
        View anchorView = anchorViewRef != null ? anchorViewRef.get() : null;
        if (context == null || anchorView == null) {
            return;
        }
        Rect tmpRect = new Rect();
        tmpRect.set(badgeBounds);

        Rect anchorRect = new Rect();
        // Retrieves the visible bounds of the anchor view.
        anchorView.getDrawingRect(anchorRect);

        ViewGroup customBadgeParent = customBadgeParentRef != null ? customBadgeParentRef.get() : null;
        if (customBadgeParent != null || BadgeUtils.USE_COMPAT_PARENT) {
            // Calculates coordinates relative to the parent.
            ViewGroup viewGroup =
                    customBadgeParent == null ? (ViewGroup) anchorView.getParent() : customBadgeParent;
            viewGroup.offsetDescendantRectToMyCoords(anchorView, anchorRect);
        }

        calculateCenterAndBounds(context, anchorRect, anchorView);

        BadgeUtils.updateBadgeBounds(badgeBounds, badgeCenterX, badgeCenterY, halfBadgeWidth, halfBadgeHeight);

//        shapeDrawable.setCornerSize(cornerRadius);
        if (!tmpRect.equals(badgeBounds)) {
            shapeDrawable.setBounds(badgeBounds);
        }
    }

    private void calculateCenterAndBounds(
            @NonNull Context context, @NonNull Rect anchorRect, @NonNull View anchorView) {
        switch (savedState.badgeGravity) {
            case BOTTOM_END:
            case BOTTOM_START:
                badgeCenterY = anchorRect.bottom;
                break;
            case TOP_END:
            case TOP_START:
            default:
                badgeCenterY = anchorRect.top;
                break;
        }

        if (getNumber() <= MAX_CIRCULAR_BADGE_NUMBER_COUNT) {
            cornerRadius = !hasNumber() ? badgeRadius : badgeWithTextRadius;
            halfBadgeHeight = cornerRadius;
            halfBadgeWidth = cornerRadius;
        } else {
            cornerRadius = badgeWithTextRadius;
            halfBadgeHeight = cornerRadius;
            String badgeText = getBadgeText();
            halfBadgeWidth = getTextWidth(badgeText) / 2f + badgeWidePadding;
        }

        int inset =
                context
                        .getResources()
                        .getDimensionPixelSize(
                                hasNumber()
                                        ? R.dimen.tab_badge_offset_has_number
                                        : R.dimen.tab_badge_offset_none_number);
        // Update the centerX based on the badge width and 'inset' from start or end boundary of anchor.
        switch (savedState.badgeGravity) {
            case BOTTOM_START:
            case TOP_START:
                badgeCenterX = anchorRect.left - halfBadgeWidth + inset;
                break;
            case BOTTOM_END:
            case TOP_END:
            default:
                badgeCenterX = anchorRect.right + halfBadgeWidth - inset;
                break;
        }
    }

    private void drawText(Canvas canvas) {
        Rect textBounds = new Rect();
        String badgeText = getBadgeText();
        textPaint.getTextBounds(badgeText, 0, badgeText.length(), textBounds);
        canvas.drawText(
                badgeText,
                badgeCenterX,
                badgeCenterY + textBounds.height() / 2,
                textPaint);
    }

    @NonNull
    private String getBadgeText() {
        // If number exceeds max count, show badgeMaxCount+ instead of the number.
        if (getNumber() <= maxBadgeNumber) {
            return Integer.toString(getNumber());
        } else {
            Context context = contextRef.get();
            if (context == null) {
                return "";
            }
            return maxBadgeNumber + DEFAULT_EXCEED_MAX_BADGE_NUMBER_SUFFIX;
        }
    }

    private void updateMaxBadgeNumber() {
        maxBadgeNumber = (int) Math.pow(10.0d, (double) getMaxCharacterCount() - 1) - 1;
    }

    public float getTextWidth(String text) {
        if (!textWidthDirty) {
            return textWidth;
        }

        textWidth = calculateTextWidth(text);
        textWidthDirty = false;
        return textWidth;
    }

    private float calculateTextWidth(@Nullable CharSequence charSequence) {
        if (charSequence == null) {
            return 0f;
        }
        return textPaint.measureText(charSequence, 0, charSequence.length());
    }
}

