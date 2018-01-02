/*
 * Copyright (C) 2017 Xizhi Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.zionsoft.fitchart;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.RequiresApi;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class FitChart extends View {
    public static class Value {
        public final int value;
        @ColorInt
        public final int color;

        public Value(int value, int color) {
            this.value = value;
            this.color = color;
        }
    }

    public enum AnimationType {
        None,
        Sequential,
        Parallel
    }

    private static final float DEFAULT_START_ANGLE = 0.0F;
    private static final float DEFAULT_END_ANGLE = 360.0F;

    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;

    private static final int DEFAULT_STROKE_WIDTH_IN_DP = 16;
    private static final int DEFAULT_STROKE_COLOR = 0xFFCCCCCC;

    private final RectF rectF = new RectF();

    private float startAngle = DEFAULT_START_ANGLE;
    private float endAngle = DEFAULT_END_ANGLE;

    private int minValue = DEFAULT_MIN_VALUE;
    private int maxValue = DEFAULT_MAX_VALUE;

    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float strokeWidthInPixel;

    private AnimationType animationType = AnimationType.Sequential;
    private float animationFraction = 1.0F;

    private Value[] values;
    private float totalValue;

    public FitChart(Context context) {
        super(context);
        init(context);
    }

    public FitChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FitChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FitChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        strokeWidthInPixel = density * DEFAULT_STROKE_WIDTH_IN_DP;
        valuePaint.setStrokeCap(Paint.Cap.ROUND);
        valuePaint.setStrokeWidth(strokeWidthInPixel);
        valuePaint.setStyle(Paint.Style.STROKE);

        backgroundPaint.setColor(DEFAULT_STROKE_COLOR);
        backgroundPaint.setStrokeWidth(strokeWidthInPixel);
        backgroundPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void invalidate() {
        if (animationType == AnimationType.None || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            animationFraction = 1.0F;
            super.invalidate();
        } else {
            animateValues();
        }
    }

    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    private void animateValues() {
        animationFraction = 0.0F;

        final ValueAnimator animator = ValueAnimator.ofFloat(0.0F, 1.0F).setDuration(1000L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                animationFraction = (float) valueAnimator.getAnimatedValue();
                FitChart.super.invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        final int leftPadding = getPaddingLeft();
        final int rightPadding = getPaddingRight();
        final int topPadding = getPaddingTop();
        final int bottomPadding = getPaddingBottom();

        final float halfSize = Math.min(width - leftPadding - rightPadding, height - topPadding - bottomPadding) / 2.0F;
        final float centerX = (width + leftPadding - rightPadding) / 2.0F;
        final float centerY = (height + topPadding - bottomPadding) / 2.0F;

        final float halfStrokeWidth = strokeWidthInPixel / 2.0F;

        rectF.left = centerX - halfSize + halfStrokeWidth;
        rectF.right = centerX + halfSize - halfStrokeWidth;
        rectF.top = centerY - halfSize + halfStrokeWidth;
        rectF.bottom = centerY + halfSize - halfStrokeWidth;

        canvas.drawArc(rectF, startAngle, endAngle - startAngle, false, backgroundPaint);

        if (values == null) {
            return;
        }
        switch (animationType) {
            case None:
            case Parallel:
                drawParallel(canvas);
                break;
            case Sequential:
                drawSequential(canvas);
                break;
        }
    }

    private void drawParallel(Canvas canvas) {
        float totalValue = this.totalValue;
        for (int i = values.length - 1; i >= 0; --i) {
            final Value value = values[i];
            valuePaint.setColor(value.color);

            final float sweepAngle = (endAngle - startAngle) * totalValue / (maxValue - minValue);
            canvas.drawArc(rectF, startAngle, sweepAngle * animationFraction, false, valuePaint);
            totalValue -= value.value;
        }
    }

    private void drawSequential(Canvas canvas) {
        final float maxValueToDraw = totalValue * animationFraction;
        int lastIndexToDraw = 0;
        float totalValue = 0.0F;
        for (Value value : values) {
            totalValue += value.value;
            if (totalValue >= maxValueToDraw) {
                break;
            } else {
                ++lastIndexToDraw;
            }
        }

        final float lastValue = maxValueToDraw - (totalValue - values[lastIndexToDraw].value);
        totalValue = maxValueToDraw;
        for (int i = lastIndexToDraw; i >= 0; --i) {
            final Value value = values[i];
            valuePaint.setColor(value.color);

            final float sweepAngle = (endAngle - startAngle) * totalValue / (maxValue - minValue);
            canvas.drawArc(rectF, startAngle, sweepAngle, false, valuePaint);
            if (i == lastIndexToDraw) {
                totalValue -= lastValue;
            } else {
                totalValue -= value.value;
            }
        }
    }

    public FitChart withAnimationType(AnimationType animationType) {
        this.animationType = animationType;
        return this;
    }

    @UiThread
    public FitChart withStartAngle(float startAngle) {
        return withAngleRange(startAngle, endAngle);
    }

    @UiThread
    public FitChart withEndAngle(float endAngle) {
        return withAngleRange(startAngle, endAngle);
    }

    @UiThread
    public FitChart withAngleRange(float startAngle, float endAngle) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        invalidate();
        return this;
    }

    @UiThread
    public FitChart withMinValue(int minValue) {
        return withValueRange(minValue, maxValue);
    }

    @UiThread
    public FitChart withMaxValue(int maxValue) {
        return withValueRange(minValue, maxValue);
    }

    @UiThread
    public FitChart withValueRange(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        invalidate();
        return this;
    }

    @UiThread
    public FitChart withValues(Value[] values) {
        this.values = new Value[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        totalValue = 0.0F;
        for (Value value : this.values) {
            totalValue += value.value;
        }
        invalidate();
        return this;
    }
}
