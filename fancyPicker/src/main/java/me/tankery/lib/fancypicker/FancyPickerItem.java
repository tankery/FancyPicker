package me.tankery.lib.fancypicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import me.tankery.lib.circularseekbar.CircularSeekBar;

/**
 * Created by tankery on 6/19/15.
 *
 * Item to hold & change the value.
 */
public class FancyPickerItem extends FrameLayout implements
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

        public abstract void onStopTrackingTouch(FancyPickerItem pickerItem);

        public abstract void onStartTrackingTouch(FancyPickerItem seekBar);
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
     * progress of picker.
     * This value of progress is from -100 ~ 100
     */
    private float progress;


    /**
     * {@code Paint} instance used to draw the static item value
     */
    private Paint itemBasePaint;

    /**
     * {@code Path} used to draw the static item value.
     */
    private Path itemBasePath;

    private RectF circlePathRect;


    /**
     * {@code CircularSeekBar} circular seekbar will visible when touch to modify the value.
     */
    private CircularSeekBar circularSeekBar;

    private OnFancyPickerItemChangeListener onFancyPickerItemChangeListener;

    public FancyPickerItem(Context context) {
        super(context);
        init(null, 0);
    }

    public FancyPickerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        circularSeekBar = new CircularSeekBar(context, attrs);
        init(attrs, 0);
    }

    public FancyPickerItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void setOnFancyPickerItemChangeListener(OnFancyPickerItemChangeListener listener) {
        onFancyPickerItemChangeListener = listener;
    }

    /**
     * Get & set the progress of item.
     */
    public float getProgress() {
        return progress;
    }
    public void setProgress(float progress) {
        if (this.progress != progress) {
            this.progress = progress;
            recalculateLayout();
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.FancyPickerItem, defStyle, 0);
        initAttributes(attrArray);
        attrArray.recycle();

        initProperties();

        circularSeekBar = new CircularSeekBar(getContext(), attrs, defStyle);
        initCircularSeekBar();
        addView(circularSeekBar);

        initPaints();
        initPaths();

        setWillNotDraw(false);
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
        circlePathRect = new RectF();
    }

    private void initCircularSeekBar() {
        circularSeekBar.setCircleStrokeWidth(strokeWidth);
        circularSeekBar.setCircleColor(Color.TRANSPARENT);
        circularSeekBar.setCircleProgressColor(itemProgressColor);
        circularSeekBar.setPointerStrokeWidth(strokeWidth);
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
        itemBasePaint = new Paint();
        itemBasePaint.setAntiAlias(true);
        itemBasePaint.setDither(true);
        itemBasePaint.setColor(itemBaseColor);
        itemBasePaint.setStrokeWidth(strokeWidth);
        itemBasePaint.setStyle(Paint.Style.STROKE);
        itemBasePaint.setStrokeJoin(Paint.Join.ROUND);
        itemBasePaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void initPaths() {
        itemBasePath = new Path();
        float diff = endAngle - startAngle;
        if (diff < 0) diff += 360;
        itemBasePath.addArc(circlePathRect, startAngle, diff);
    }

    private void updateCircularSeekBar() {
        circularSeekBar.setEnabled(false);
        circularSeekBar.setProgress(progress);

        // Update angle
        float diff = endAngle - startAngle;
        if (diff < 0) diff += 360;
        circularSeekBar.setPointerAngle(diff);

        float start = getProgressBarOriginPoint();
        circularSeekBar.setStartAngle(start);
        circularSeekBar.setEndAngle(start);

        if (circularSeekBar.getVisibility() == VISIBLE) {
            postDelayed(hideCircularSeekBarRunnable, 500);
        }
    }

    private Runnable hideCircularSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            circularSeekBar.setVisibility(INVISIBLE);
        }
    };

    private float getProgressBarOriginPoint() {
        // Update angle
        float crossAngle = (endAngle - startAngle);
        if (crossAngle < 0) crossAngle += 360;
        float centerAngle = 0.5f * crossAngle + startAngle;
        if (centerAngle > 360) centerAngle -= 360;

        float start = centerAngle - 360f * progress / 100;
        if (start < 0) start += 360;
        else if (start > 360f) start -= 360;

        return start;
    }

    private void recalculateLayout() {
        updateCircularSeekBar();
        initPaths();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        circlePathRect.set(circularSeekBar.getPathCircle());
        initPaths();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (circularSeekBar.getVisibility() == VISIBLE) {
            super.onDraw(canvas);
            return;
        }

        canvas.save();
        canvas.translate(this.getWidth() / 2, this.getHeight() / 2);

        canvas.drawPath(itemBasePath, itemBasePaint);
        canvas.restore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        // Because the start - end angle may cross the origin point, we can't compare it with touch angle directly.
        if ( event.getAction() == MotionEvent.ACTION_DOWN && touchInRange(event)) {
            // Touch inside the item, enable circular seek bar.
            circularSeekBar.setVisibility(VISIBLE);
            circularSeekBar.setEnabled(true);
            removeCallbacks(hideCircularSeekBarRunnable);
        }

        return super.dispatchTouchEvent(event);
    }


    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
        this.progress = progress;
        if (onFancyPickerItemChangeListener != null)
            onFancyPickerItemChangeListener.onProgressChanged(this, this.progress, fromUser);
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {
        // update layout when value changed.
        recalculateLayout();
        if (onFancyPickerItemChangeListener != null)
            onFancyPickerItemChangeListener.onStopTrackingTouch(this);
    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {
        if (onFancyPickerItemChangeListener != null) {
            onFancyPickerItemChangeListener.onStartTrackingTouch(this);
        }
    }

    private boolean touchInRange(MotionEvent event) {

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

        float crossAngle = (endAngle - startAngle);
        if (crossAngle < 0) crossAngle += 360;
        float centerAngle = 0.5f * crossAngle + startAngle;
        if (centerAngle > 360) centerAngle -= 360;
        float cwDistanceFromPointer = touchAngle - centerAngle;
        if (cwDistanceFromPointer < 0) cwDistanceFromPointer += 360;
        float ccwDistanceFromPointer = 360f - cwDistanceFromPointer;

        return ((touchEventRadius >= innerRadius) && (touchEventRadius <= outerRadius)) &&
                ((cwDistanceFromPointer <= crossAngle/2) || (ccwDistanceFromPointer <= crossAngle/2));
    }
}
