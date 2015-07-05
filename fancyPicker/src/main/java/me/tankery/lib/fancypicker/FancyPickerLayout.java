package me.tankery.lib.fancypicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by tankery on 6/18/15.
 */
public class FancyPickerLayout extends FrameLayout implements
        FancyPickerItem.OnFancyPickerItemChangeListener {

    /**
     * Used to scale the dp units to pixels
     */
    private final float DPTOPX_SCALE = getResources().getDisplayMetrics().density;

    /**
     * Minimum touch target size in DP. 48dp is the Android design recommendation
     */
    private static final float MIN_TOUCH_TARGET_DP = 48;

    private static final int DEFAULT_ORBIT_BASE_COLOR = Color.argb(255, 128, 128, 128);

    private static final float DEFAULT_START_ANGLE = 90f;
    private static final float DEFAULT_END_ANGLE = 270f;
    private static final float DEFAULT_ORBIT_STROKE_WIDTH = 56;


    /**
     * Holds the color value for {@code orbitBaseColor} before the {@code Paint} instance is created.
     */
    private int orbitBaseColor = DEFAULT_ORBIT_BASE_COLOR;

    /**
     * Angle of the layout start.
     */
    private float startAngle;
    /**
     * Angle of the layout end.
     */
    private float endAngle;
    /**
     * Stroke width for orbit & item.
     */
    private float orbitStrokeWidth;


    /**
     * {@code Paint} instance used to draw the orbit circle.
     */
    private Paint orbitBasePaint = new Paint();

    /**
     * {@code Path} used to draw the orbit circle.
     */
    private Path orbitBasePath = new Path();

    private RectF circlePathRect = new RectF();


    int[] fancyItemIndexes;
    int fancyItemCount;

    public FancyPickerLayout(Context context) {
        super(context);
        init(null, 0);
    }

    public FancyPickerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public FancyPickerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray attrArray = getContext().obtainStyledAttributes(attrs, R.styleable.FancyPickerLayout, defStyle, 0);
        initAttributes(attrArray);
        attrArray.recycle();

        initPaints();

        setWillNotDraw(false);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (int i = 0; i < fancyItemCount; i++) {
            int pos = fancyItemIndexes[i];
            FancyPickerItem item = (FancyPickerItem) getChildAt(pos);
            item.removeOnFancyPickerItemChangeListener(this);
        }
    }

    /**
     * Initialize the FancyPickerLayout with the attributes from the XML style.
     * Uses the defaults defined at the top of this file when an attribute is not specified by the user.
     * @param attrArray TypedArray containing the attributes.
     */
    private void initAttributes(TypedArray attrArray) {
        startAngle = attrArray.getFloat(R.styleable.FancyPickerLayout_fp_start_angle, DEFAULT_START_ANGLE);
        endAngle = attrArray.getFloat(R.styleable.FancyPickerLayout_fp_end_angle, DEFAULT_END_ANGLE);
        orbitStrokeWidth = attrArray.getDimension(R.styleable.FancyPickerLayout_fp_orbit_stroke_width, DEFAULT_ORBIT_STROKE_WIDTH);
        orbitBaseColor = attrArray.getColor(R.styleable.FancyPickerLayout_fp_orbit_base_color, DEFAULT_ORBIT_BASE_COLOR);
    }

    private void initPaints() {
        orbitBasePaint.reset();
        orbitBasePaint.setAntiAlias(true);
        orbitBasePaint.setDither(true);
        orbitBasePaint.setColor(orbitBaseColor);
        orbitBasePaint.setStrokeWidth(orbitStrokeWidth);
        orbitBasePaint.setStyle(Paint.Style.STROKE);
        orbitBasePaint.setStrokeJoin(Paint.Join.ROUND);
        orbitBasePaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void initPaths() {
        orbitBasePath.reset();
        orbitBasePath.addArc(circlePathRect, startAngle, 360);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final int count = getChildCount();

        fancyItemIndexes = new int[count];
        fancyItemCount = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof FancyPickerItem) {
                fancyItemIndexes[fancyItemCount] = i;
                fancyItemCount++;
            }
        }

        float rangeAngle = endAngle - startAngle;
        if (rangeAngle > 360) rangeAngle -= 360;
        if (rangeAngle <= 0) rangeAngle += 360;
        float itemAngle = rangeAngle / fancyItemCount;

        circlePathRect.setEmpty();

        for (int i = 0; i < fancyItemCount; i++) {
            int pos = fancyItemIndexes[i];
            FancyPickerItem item = (FancyPickerItem) getChildAt(pos);

            float start = startAngle + itemAngle * i;
            float end = start + itemAngle;
            item.setGeometry(start, end, orbitStrokeWidth);

            circlePathRect.union(item.getCirclePathRect());

            if (!item.hasOnFancyPickerItemChangeListener(this))
                item.addOnFancyPickerItemChangeListener(this);
        }

        initPaths();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(this.getWidth() / 2, this.getHeight() / 2);

        canvas.drawPath(orbitBasePath, orbitBasePaint);
        canvas.restore();
    }

    @Override
    public void onProgressChanged(FancyPickerItem pickerItem, float progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(FancyPickerItem pickerItem) {
        for (int i = 0; i < fancyItemCount; i++) {
            int pos = fancyItemIndexes[i];
            FancyPickerItem item = (FancyPickerItem) getChildAt(pos);
            if (item != pickerItem) {
                item.setVisibility(INVISIBLE);
                item.setEnabled(false);
            }
        }
    }

    @Override
    public void onStopTrackingTouch(FancyPickerItem pickerItem) {
    }

    @Override
    public void onEndTrackingAnimation(FancyPickerItem pickerItem) {
        for (int i = 0; i < fancyItemCount; i++) {
            int pos = fancyItemIndexes[i];
            FancyPickerItem item = (FancyPickerItem) getChildAt(pos);
            item.setVisibility(VISIBLE);
            item.setEnabled(true);
        }
    }

}
