package com.example.cvforge.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cvforge.R;

/**
 * Draws one decorative circle. The circle can sit half outside the view,
 * so only the part inside the view bounds is visible (put the view in a
 * card or header that clips its children to get the corner effect).
 */
public class CircleDecorationView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerX = 1f;
    private float centerY = 0f;
    private float radius;

    public CircleDecorationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        radius = 60 * getResources().getDisplayMetrics().density;
        paint.setColor(ContextCompat.getColor(context, R.color.coral));

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleDecorationView);
            paint.setColor(a.getColor(R.styleable.CircleDecorationView_circleColor, paint.getColor()));
            centerX = a.getFloat(R.styleable.CircleDecorationView_circleCenterX, centerX);
            centerY = a.getFloat(R.styleable.CircleDecorationView_circleCenterY, centerY);
            radius = a.getDimension(R.styleable.CircleDecorationView_circleRadius, radius);
            a.recycle();
        }
        // purely decorative, hidden from screen readers
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(getWidth() * centerX, getHeight() * centerY, radius, paint);
    }
}
