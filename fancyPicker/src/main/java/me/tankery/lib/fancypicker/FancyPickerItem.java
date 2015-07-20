package me.tankery.lib.fancypicker;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.tankery.lib.circularseekbar.CircularSeekBar;

/**
 * Created by tankery on 6/19/15.
 *
 * Item to hold & change the value.
 */
public class FancyPickerItem extends TextView implements
        CircularSeekBar.OnCircularSeekBarChangeListener {

    /**
     * Used to scale the dp units to pixels
     */
    private final float DPTOPX_SCALE = getResources().getDisplayMetrics().density;

    /**
     * Minimum touch target size in DP. 48dp is the Android design recommendation
     */
    private static final float MIN_TOUCH_TARGET_DP = 48;

    private static final int DEFAULT_ITEM_COLOR = Color.argb(235, 74, 138, 255);
    private static final int DEFAULT_ITEM_PROGRESS_COLOR = Color.argb(235, 74, 138, 255);
    private static final int DEFAULT_ITEM_HOVER_COLOR = Color.argb(135, 74, 138, 255);

    private static final float DEFAULT_START_ANGLE = 270f - 15f;
    private static final float DEFAULT_END_ANGLE = 270f + 15f;
    private static final float DEFAULT_STROKE_WIDTH = 48;


    /**
     * Listener for the FancyPickerItem. Implements the same methods as the normal OnSeekBarChangeListener.
     */
    public interface OnFancyPickerItemChangeListener {

        public abstract void onProgressChanged(FancyPickerItem pickerItem, float progress, boolean fromUser);

        public abstract void onStartTrackingTouch(FancyPickerItem pickerItem);

        public abstract void onStopTrackingTouch(FancyPickerItem pickerItem);

        public abstract void onEndTrackingAnimation(FancyPickerItem pickerItem);
    }

    /**
     * Holds the color value for {@code itemBaseColor} before the {@code Paint} instance is created.
     */
    private int itemBaseColor = DEFAULT_ITEM_COLOR;
    /**
     * Holds the color value for {@code itemProgressColor} before the {@code Paint} instance is created.
     */
    private int itemProgressColor = DEFAULT_ITEM_PROGRESS_COLOR;
    /**
     * Holds the color value for {@code itemProgressHoverColor} before the {@code Paint} instance is created.
     */
    private int itemProgressHoverColor = DEFAULT_ITEM_HOVER_COLOR;


    /**
     * Angle of the value arc start.
     * This value will set by {@code FancyPickerLayout}
     */
    private float startAngle;
    /**
     * Angle of the value arc end.
     * This value will set by {@code FancyPickerLayout}
     */
    private float endAngle;
    /**
     * Stroke width for value/progress/hover.
     * This value will set by {@code FancyPickerLayout}
     */
    private float strokeWidth;



    /**
     * position of item base shape center.
     */
    float[] itemCenterPos = new float[2];


    /**
     * progress of picker.
     * This value of progress is from -100 ~ 100
     */
    private float progress;
    /**
     * progress when touch down
     */
    private float touchStartProgress;


    /**
     * {@code Paint} instance used to draw the static item value
     */
    private Paint itemBasePaint = new Paint();

    /**
     * {@code Path} used to draw the static item value.
     */
    private Path itemBasePath = new Path();

    private RectF circlePathRect = new RectF();


    /**
     * {@code CircularSeekBar} circular seekbar will visible when touch to modify the value.
     */
    private CircularSeekBar circularSeekBar;

    private List<OnFancyPickerItemChangeListener> onChangeListeners;

    public FancyPickerItem(Context context) {
        super(context);
        init(null, 0);
    }

    public FancyPickerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public FancyPickerItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void addOnFancyPickerItemChangeListener(OnFancyPickerItemChangeListener listener) {
        onChangeListeners.add(listener);
    }
    public void removeOnFancyPickerItemChangeListener(OnFancyPickerItemChangeListener listener) {
        onChangeListeners.remove(listener);
    }
    public boolean hasOnFancyPickerItemChangeListener(OnFancyPickerItemChangeListener listener) {
        return onChangeListeners.contains(listener);
    }

    /**
     * Get & set the progress of item.
     */
    @SuppressWarnings("unused")
    public float getProgress() {
        return progress;
    }
    @SuppressWarnings("unused")
    public void setProgress(float progress) {
        if (this.progress != progress) {
            this.progress = progress;
            recalculateLayout();
        }
    }

    public void setGeometry(float startAngle, float endAngle, float strokeWidth) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.strokeWidth = strokeWidth;
        recalculateLayout();
    }

    protected RectF getCirclePathRect() {
        return circlePathRect;
    }


    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.FancyPickerItem, defStyle, 0);
        initAttributes(attrArray);
        attrArray.recycle();

        initProperties();

        circularSeekBar = new CircularSeekBar(getContext(), attrs, defStyle);
        initCircularSeekBar();

        initPaints();
        initPaths();

        setWillNotDraw(false);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.TRANSPARENT);
        onChangeListeners = new ArrayList<>();
    }

    /**
     * Initialize the FancyPickerItem with the attributes from the XML style.
     * Uses the defaults defined at the top of this file when an attribute is not specified by the user.
     * @param attrArray TypedArray containing the attributes.
     */
    private void initAttributes(TypedArray attrArray) {
        itemBaseColor = attrArray.getColor(R.styleable.FancyPickerItem_fp_item_color, DEFAULT_ITEM_COLOR);
        itemProgressColor = attrArray.getColor(R.styleable.FancyPickerItem_fp_item_progress_color, DEFAULT_ITEM_PROGRESS_COLOR);
        itemProgressHoverColor = attrArray.getColor(R.styleable.FancyPickerItem_fp_item_hover_color, DEFAULT_ITEM_HOVER_COLOR);
    }

    private void initProperties() {
        startAngle = DEFAULT_START_ANGLE;
        endAngle = DEFAULT_END_ANGLE;
        strokeWidth = DEFAULT_STROKE_WIDTH;
        progress = 0;
        touchStartProgress = 0;
    }

    private void initCircularSeekBar() {
        circularSeekBar.setCircleColor(Color.TRANSPARENT);
        circularSeekBar.setCircleProgressColor(itemProgressColor);
        circularSeekBar.setPointerColor(itemProgressColor);
        circularSeekBar.setPointerHaloColor(itemProgressHoverColor);
        circularSeekBar.setCircleStyle(Paint.Cap.BUTT);
        circularSeekBar.setLockEnabled(true);
        circularSeekBar.setNegativeEnabled(true);

        circularSeekBar.setVisibility(INVISIBLE);
        circularSeekBar.setEnabled(false);
        circularSeekBar.setOnSeekBarChangeListener(this);

        updateCircularSeekBar();
    }

    /**
     * Initializes the {@code Paint} objects with the appropriate styles.
     */
    private void initPaints() {
        itemBasePaint.reset();
        itemBasePaint.setAntiAlias(true);
        itemBasePaint.setDither(true);
        itemBasePaint.setColor(itemBaseColor);
        itemBasePaint.setStrokeWidth(strokeWidth);
        itemBasePaint.setStyle(Paint.Style.STROKE);
        itemBasePaint.setStrokeJoin(Paint.Join.ROUND);
        itemBasePaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void initPaths() {
        itemBasePath.reset();
        float diff = normalizeAngle(endAngle - startAngle);
        itemBasePath.addArc(circlePathRect, startAngle, diff);

        updateItemCenter();
    }

    private void updateCircularSeekBar() {
        circularSeekBar.setEnabled(false);
        circularSeekBar.setProgress(progress);
        circularSeekBar.setCircleStrokeWidth(strokeWidth);
        circularSeekBar.setPointerStrokeWidth(strokeWidth);

        // Update angle
        float diff = normalizeAngle(endAngle - startAngle);
        circularSeekBar.setPointerAngle(diff);

        if ((progress - touchStartProgress) % 360 != 0) {

            float targetStart = getAnimatableProgressBarTargetStartAngle();
            float currentStart = circularSeekBar.getStartAngle();

            ValueAnimator rotate = ValueAnimator.ofFloat(currentStart, targetStart);
            rotate.setDuration(getResources().getInteger(R.integer.progress_bar_end_animation));
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            rotate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = (Float) valueAnimator.getAnimatedValue();
                    setSeekBarOrigin(value);
                }
            });
            rotate.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) { }
                @Override
                public void onAnimationRepeat(Animator animator) { }

                @Override
                public void onAnimationEnd(Animator animator) {
                    delayHideSeekBar();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    hideCircularSeekBarRunnable.run();
                }
            });
            rotate.start();
        } else {
            // Make sure the seek bar is on right position.
            setSeekBarOrigin(getProgressBarOriginPoint());

            delayHideSeekBar();
        }
    }

    private void setSeekBarOrigin(float origin) {
        origin = normalizeAngle(origin);
        circularSeekBar.setStartAngle(origin);
        circularSeekBar.setEndAngle(origin);
    }

    private void delayHideSeekBar() {
        if (circularSeekBar.getVisibility() == VISIBLE) {
            postDelayed(hideCircularSeekBarRunnable, 500);
        }
    }

    private Runnable hideCircularSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            circularSeekBar.setVisibility(INVISIBLE);
            circularSeekBar.setEnabled(false);
            invalidate();
            for (OnFancyPickerItemChangeListener listener : onChangeListeners)
                listener.onEndTrackingAnimation(FancyPickerItem.this);
        }
    };

    private float normalizeAngle(float angle) {
        while (angle > 360) angle -= 360;
        while (angle < 0) angle += 360;

        return angle;
    }

    private float getProgressBarValueArcCenter() {
        float crossAngle = normalizeAngle(endAngle - startAngle);
        float centerAngle = 0.5f * crossAngle + startAngle;

        return normalizeAngle(centerAngle);
    }

    private float getProgressBarOriginPoint() {
        // Update angle
        float centerAngle = getProgressBarValueArcCenter();

        float start = centerAngle - 360f * progress / 100;

        return normalizeAngle(start);
    }

    private float getAnimatableProgressBarTargetStartAngle() {
        float targetStart = getProgressBarOriginPoint();
        float currentStart = circularSeekBar.getStartAngle();

        // Choose the right animation direction.
        boolean cwRotate = (progress < touchStartProgress);

        if (cwRotate) {
            float delta = normalizeAngle(targetStart - currentStart);
            targetStart = currentStart + delta;
        } else {
            float delta = normalizeAngle(currentStart - targetStart);
            targetStart = currentStart - delta;
        }

        return targetStart;
    }

    private void recalculateLayout() {
        updateCircularSeekBar();
        initPaints();
        initPaths();
        invalidate();
    }

    public void updateItemCenter() {
        float crossAngle = normalizeAngle(endAngle - startAngle);
        float centerAngle = normalizeAngle(0.5f * crossAngle + startAngle);
        double itemAnglePolar = normalizeAngle(centerAngle + 90) * 2 * Math.PI / 360;
        float radius = Math.min(circlePathRect.width(), circlePathRect.height()) / 2.0f;
        itemCenterPos[0] = (float) (radius * Math.sin(itemAnglePolar));
        itemCenterPos[1] = - (float) (radius * Math.cos(itemAnglePolar));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (getParent() == null || !(getParent() instanceof FancyPickerLayout)) {
            throw new RuntimeException("FancyPickerItem must be used inside FancyPickerLayout");
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        circlePathRect.set(circularSeekBar.getPathCircle());
        initPaths();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (circularSeekBar.getVisibility() == VISIBLE) {
            return;
        }

        canvas.save();
        canvas.translate(this.getWidth() / 2, this.getHeight() / 2);

        canvas.drawPath(itemBasePath, itemBasePaint);

        float[] xy = itemCenterPos;
        float ts = getTextSize();
        Paint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        paint.setTextSize(getTextSize());
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(getText(), 0, getText().length(), xy[0], xy[1] + ts/2, paint);

        canvas.restore();
    }


    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
        this.progress = progress;
        for (OnFancyPickerItemChangeListener listener : onChangeListeners)
            listener.onProgressChanged(this, this.progress, fromUser);
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {
        // update layout when value changed.
        recalculateLayout();
        touchStartProgress = progress;
        for (OnFancyPickerItemChangeListener listener : onChangeListeners)
            listener.onStopTrackingTouch(this);
    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {
        for (OnFancyPickerItemChangeListener listener : onChangeListeners)
            listener.onStartTrackingTouch(this);

        // Save touch down progress
        touchStartProgress = progress;
    }

    public CircularSeekBar getCircularSeekBar() {
        return circularSeekBar;
    }

    public void enableCircularSeekBar() {
        removeCallbacks(hideCircularSeekBarRunnable);
        circularSeekBar.setVisibility(VISIBLE);
        circularSeekBar.setEnabled(true);
        invalidate();
    }

    public boolean touchInRange(MotionEvent event) {

        // Convert coordinates to our internal coordinate system
        float x = event.getX() - getWidth() / 2;
        float y = event.getY() - getHeight() / 2;

        // Get the distance from the center of the circle in terms of x and y
        float distanceX = circlePathRect.centerX() - x;
        float distanceY = circlePathRect.centerY() - y;

        // Get the distance from the center of the circle in terms of a radius
        float touchEventRadius = (float) Math.sqrt((Math.pow(distanceX, 2) + Math.pow(distanceY, 2)));

        float minimumTouchTarget = MIN_TOUCH_TARGET_DP * DPTOPX_SCALE; // Convert minimum touch target into px
        float additionalRadius; // Either uses the minimumTouchTarget size or larger if the ring/pointer is larger

        if (strokeWidth < minimumTouchTarget) { // If the width is less than the minimumTouchTarget, use the minimumTouchTarget
            additionalRadius = minimumTouchTarget / 2;
        } else {
            additionalRadius = strokeWidth / 2; // Otherwise use the width
        }
        float circleWidth = circlePathRect.width() / 2f;
        float circleHeight = circlePathRect.height() / 2f;
        float outerRadius = Math.max(circleHeight, circleWidth) + additionalRadius; // Max outer radius of the circle, including the minimumTouchTarget or wheel width
        float innerRadius = Math.min(circleHeight, circleWidth) - additionalRadius; // Min inner radius of the circle, including the minimumTouchTarget or wheel width

        float touchAngle;
        touchAngle = (float) ((Math.atan2(y, x) / Math.PI * 180) % 360); // Verified
        touchAngle = (touchAngle < 0 ? 360 + touchAngle : touchAngle); // Verified

        float crossAngle = normalizeAngle(endAngle - startAngle);
        float centerAngle = normalizeAngle(0.5f * crossAngle + startAngle);
        float cwDistanceFromPointer = normalizeAngle(touchAngle - centerAngle);
        float ccwDistanceFromPointer = 360f - cwDistanceFromPointer;

        return ((touchEventRadius >= innerRadius) && (touchEventRadius <= outerRadius)) &&
                ((cwDistanceFromPointer <= crossAngle/2) || (ccwDistanceFromPointer <= crossAngle/2));
    }
}
