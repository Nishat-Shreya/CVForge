package com.example.cvforge.ui.tracker;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.cvforge.R;

/**
 * The picture that follows the finger while a card is dragged: the card tilted by 3 degrees
 * with a violet border (DESIGN.md, animation A8).
 */
public class CardDragShadow extends View.DragShadowBuilder {

    private final float padding;
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CardDragShadow(View card) {
        super(card);
        padding = 16 * card.getResources().getDisplayMetrics().density;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2 * card.getResources().getDisplayMetrics().density);
        borderPaint.setColor(ContextCompat.getColor(card.getContext(), R.color.violet));
    }

    @Override
    public void onProvideShadowMetrics(Point size, Point touch) {
        View card = getView();
        size.set(Math.round(card.getWidth() + 2 * padding), Math.round(card.getHeight() + 2 * padding));
        touch.set(size.x / 2, size.y / 2);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        View card = getView();
        float width = card.getWidth();
        float height = card.getHeight();

        canvas.save();
        canvas.translate(padding, padding);
        canvas.rotate(3f, width / 2f, height / 2f);
        card.draw(canvas);
        float radius = 20 * card.getResources().getDisplayMetrics().density;
        canvas.drawRoundRect(new RectF(0, 0, width, height), radius, radius, borderPaint);
        canvas.restore();
    }
}
