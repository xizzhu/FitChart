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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

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

    private static final float DEFAULT_START_ANGLE = 0.0F;
    private static final float DEFAULT_END_ANGLE = 360.0F;

    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;

    private static final int DEFAULT_VALUE_WIDTH_IN_DP = 8;
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFF888888;
    private static final int DEFAULT_BACKGROUND_WIDTH_IN_DP = 4;

    private final RectF rectF = new RectF();

    private float startAngle = DEFAULT_START_ANGLE;
    private float endAngle = DEFAULT_END_ANGLE;

    private int minValue = DEFAULT_MIN_VALUE;
    private int maxValue = DEFAULT_MAX_VALUE;

    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float valueWidthInPixel;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float backgroundWidthInPixel;

    private Value[] values;

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
        valueWidthInPixel = density * DEFAULT_VALUE_WIDTH_IN_DP;
        valuePaint.setStrokeCap(Paint.Cap.ROUND);
        valuePaint.setStrokeWidth(valueWidthInPixel);
        valuePaint.setStyle(Paint.Style.STROKE);

        backgroundWidthInPixel = density * DEFAULT_BACKGROUND_WIDTH_IN_DP;
        backgroundPaint.setColor(DEFAULT_BACKGROUND_COLOR);
        backgroundPaint.setStrokeWidth(backgroundWidthInPixel);
        backgroundPaint.setStyle(Paint.Style.STROKE);
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

        final float halfStrokeWidth = Math.min(backgroundWidthInPixel, valueWidthInPixel) / 2.0F;

        rectF.left = centerX - halfSize + halfStrokeWidth;
        rectF.right = centerX + halfSize - halfStrokeWidth;
        rectF.top = centerY - halfSize + halfStrokeWidth;
        rectF.bottom = centerY + halfSize - halfStrokeWidth;

        canvas.drawArc(rectF, startAngle, endAngle - startAngle, false, backgroundPaint);

        if (values == null) {
            return;
        }
        for (Value value : values) {
            valuePaint.setColor(value.color);

            final float sweepAngle = (endAngle - startAngle) * value.value / (maxValue - minValue);
            canvas.drawArc(rectF, startAngle, sweepAngle, false, valuePaint);
        }
    }

    @UiThread
    public FitChart setStartAngle(float startAngle) {
        return setAngleRange(startAngle, endAngle);
    }

    @UiThread
    public FitChart setEndAngle(float endAngle) {
        return setAngleRange(startAngle, endAngle);
    }

    @UiThread
    public FitChart setAngleRange(float startAngle, float endAngle) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        invalidate();
        return this;
    }

    @UiThread
    public FitChart setMinValue(int minValue) {
        return setValueRange(minValue, maxValue);
    }

    @UiThread
    public FitChart setMaxValue(int maxValue) {
        return setValueRange(minValue, maxValue);
    }

    @UiThread
    public FitChart setValueRange(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        invalidate();
        return this;
    }

    @UiThread
    public FitChart setValues(Value[] values) {
        this.values = new Value[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        invalidate();
        return this;
    }
}
